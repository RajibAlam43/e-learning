#!/usr/bin/env bash
# DigitalOcean data droplet bootstrap:
# 1) Install + secure PostgreSQL
# 2) Install + secure Valkey (or Redis-compatible fallback)
# 3) Create app DB/user credentials
# 4) Configure private-network access for API + Worker droplets
# 5) Configure nightly PostgreSQL backups to Cloudflare R2
# 6) Apply baseline host firewall rules
#
# Notes:
# - This script is intended for Ubuntu/Debian droplets.
# - Run as root (User Data does this by default).
# - Re-runnable: tries to be idempotent where practical.

set -euo pipefail
umask 027

#######################################
# 0) EDIT THESE VALUES BEFORE RUNNING
#######################################

# Use simple identifier-safe names (letters/numbers/underscore).
# Avoid hyphens in DB/user names to prevent quoting hassles.
APP_DB_NAME="gii_stage_db"
APP_DB_USER="gii_stage_app"

# Allowed client CIDRs for app connections (use /32 for single droplet IP).
# Example: "10.116.0.8/32"
API_ALLOWED_CIDR="REPLACE_ME"
WORKER_ALLOWED_CIDR="REPLACE_ME"

# Optional direct DBeaver access.
# Recommended: keep false and use SSH tunnel instead.
ALLOW_DBEAVER_DIRECT="false"
DBEAVER_IP_CIDR="REPLACE_ME" # Example: 203.0.113.10/32

# Cloudflare R2 settings (required for backup upload).
R2_ACCOUNT_ID="REPLACE_ME"
R2_BUCKET="REPLACE_ME"
R2_ACCESS_KEY_ID="REPLACE_ME"
R2_SECRET_ACCESS_KEY="REPLACE_ME"

# Nightly backup schedule (UTC).
BACKUP_HOUR_UTC="03"
BACKUP_MINUTE_UTC="20"

#######################################
# 1) AUTO-DETECT DATA PRIVATE IP
#######################################

# DigitalOcean metadata service for private VPC IPv4.
DATA_PRIVATE_IP="$(curl -fsS http://169.254.169.254/metadata/v1/interfaces/private/0/ipv4/address || true)"
if [[ -z "${DATA_PRIVATE_IP}" ]]; then
  echo "ERROR: Could not detect DATA_PRIVATE_IP from DO metadata."
  echo "Ensure the droplet is attached to a VPC with private networking enabled."
  exit 1
fi

#######################################
# 2) VALIDATE INPUTS
#######################################

required_vars=(
  APP_DB_NAME
  APP_DB_USER
  API_ALLOWED_CIDR
  WORKER_ALLOWED_CIDR
  R2_ACCOUNT_ID
  R2_BUCKET
  R2_ACCESS_KEY_ID
  R2_SECRET_ACCESS_KEY
)

for v in "${required_vars[@]}"; do
  if [[ -z "${!v:-}" || "${!v}" == "REPLACE_ME" ]]; then
    echo "ERROR: ${v} is not set. Edit script values first."
    exit 1
  fi
done

if [[ "${ALLOW_DBEAVER_DIRECT}" == "true" ]]; then
  if [[ -z "${DBEAVER_IP_CIDR:-}" || "${DBEAVER_IP_CIDR}" == "REPLACE_ME" ]]; then
    echo "ERROR: DBEAVER_IP_CIDR must be set when ALLOW_DBEAVER_DIRECT=true."
    exit 1
  fi
fi

# Keep DB/user names strict and predictable.
if [[ ! "${APP_DB_NAME}" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
  echo "ERROR: APP_DB_NAME must match ^[a-zA-Z_][a-zA-Z0-9_]*$"
  exit 1
fi

if [[ ! "${APP_DB_USER}" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
  echo "ERROR: APP_DB_USER must match ^[a-zA-Z_][a-zA-Z0-9_]*$"
  exit 1
fi

#######################################
# 3) HELPERS
#######################################

add_line_if_missing() {
  local line="$1"
  local file="$2"
  grep -qxF "$line" "$file" || echo "$line" >>"$file"
}

install_aws_cli() {
  if command -v aws >/dev/null 2>&1; then
    return 0
  fi

  local arch zip_url
  arch="$(dpkg --print-architecture)"
  case "${arch}" in
    amd64) zip_url="https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" ;;
    arm64) zip_url="https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" ;;
    *)
      echo "ERROR: Unsupported architecture for AWS CLI install: ${arch}"
      exit 1
      ;;
  esac

  curl -fsSL "${zip_url}" -o /tmp/awscliv2.zip
  unzip -q /tmp/awscliv2.zip -d /tmp
  /tmp/aws/install --update
}

#######################################
# 4) INSTALL PACKAGES
#######################################

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y \
  postgresql postgresql-contrib \
  zstd unzip \
  ca-certificates curl gnupg lsb-release \
  ufw unattended-upgrades openssl

install_aws_cli

# Install Valkey if present; fallback to Redis server (protocol-compatible).
VALKEY_SERVICE=""
if apt-cache show valkey >/dev/null 2>&1; then
  apt-get install -y valkey
  VALKEY_SERVICE="valkey"
elif apt-cache show valkey-server >/dev/null 2>&1; then
  apt-get install -y valkey-server
  VALKEY_SERVICE="valkey-server"
elif apt-cache show redis-server >/dev/null 2>&1; then
  apt-get install -y redis-server
  VALKEY_SERVICE="redis-server"
else
  echo "ERROR: Could not find valkey/redis-server package in apt repositories."
  exit 1
fi

#######################################
# 5) GENERATE + STORE APP SECRETS
#######################################

APP_DB_PASSWORD="$(openssl rand -base64 36 | tr -d '\n')"
VALKEY_PASSWORD="$(openssl rand -base64 36 | tr -d '\n')"

install -d -m 0750 /etc/e-learning

# Source-of-truth runtime values to copy into API/Worker GitHub env secrets.
cat >/etc/e-learning/runtime.env <<EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://${DATA_PRIVATE_IP}:5432/${APP_DB_NAME}
SPRING_DATASOURCE_USERNAME=${APP_DB_USER}
SPRING_DATASOURCE_PASSWORD=${APP_DB_PASSWORD}
SPRING_DATA_VALKEY_HOST=${DATA_PRIVATE_IP}
SPRING_DATA_VALKEY_PORT=6379
SPRING_DATA_VALKEY_PASSWORD=${VALKEY_PASSWORD}
EOF
chmod 0600 /etc/e-learning/runtime.env
chown root:root /etc/e-learning/runtime.env

#######################################
# 6) CONFIGURE POSTGRESQL
#######################################

PG_VERSION="$(ls /etc/postgresql | sort -V | tail -n1)"
PG_CONF="/etc/postgresql/${PG_VERSION}/main/postgresql.conf"
PG_HBA="/etc/postgresql/${PG_VERSION}/main/pg_hba.conf"

# Bind only loopback + private VPC IP.
sed -i "s/^#\?listen_addresses.*/listen_addresses = '127.0.0.1,${DATA_PRIVATE_IP}'/" "${PG_CONF}"

# Strong password hashing.
if grep -q "^#\?password_encryption" "${PG_CONF}"; then
  sed -i "s/^#\?password_encryption.*/password_encryption = scram-sha-256/" "${PG_CONF}"
else
  echo "password_encryption = scram-sha-256" >>"${PG_CONF}"
fi

# Allow only known clients.
add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} 127.0.0.1/32 scram-sha-256" "${PG_HBA}"
add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} ${API_ALLOWED_CIDR} scram-sha-256" "${PG_HBA}"
add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} ${WORKER_ALLOWED_CIDR} scram-sha-256" "${PG_HBA}"

if [[ "${ALLOW_DBEAVER_DIRECT}" == "true" ]]; then
  add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} ${DBEAVER_IP_CIDR} scram-sha-256" "${PG_HBA}"
fi

systemctl enable postgresql
systemctl restart postgresql

# Create/rotate role and create DB idempotently.
sudo -u postgres psql <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${APP_DB_USER}') THEN
    EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '${APP_DB_USER}', '${APP_DB_PASSWORD}');
  ELSE
    EXECUTE format('ALTER ROLE %I WITH LOGIN PASSWORD %L', '${APP_DB_USER}', '${APP_DB_PASSWORD}');
  END IF;
END
\$\$;

DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '${APP_DB_NAME}') THEN
    EXECUTE format('CREATE DATABASE %I OWNER %I', '${APP_DB_NAME}', '${APP_DB_USER}');
  END IF;
END
\$\$;
SQL

#######################################
# 7) CONFIGURE VALKEY/REDIS
#######################################

VALKEY_CONF=""
for c in /etc/valkey/valkey.conf /etc/valkey.conf /etc/redis/redis.conf; do
  if [[ -f "$c" ]]; then
    VALKEY_CONF="$c"
    break
  fi
done

if [[ -z "${VALKEY_CONF}" ]]; then
  echo "ERROR: Could not locate Valkey/Redis config file."
  exit 1
fi

# Bind only loopback + private VPC IP.
sed -i "s/^#\?bind .*/bind 127.0.0.1 ${DATA_PRIVATE_IP}/" "${VALKEY_CONF}"
sed -i "s/^#\?protected-mode .*/protected-mode yes/" "${VALKEY_CONF}"
sed -i "s/^#\?port .*/port 6379/" "${VALKEY_CONF}"
sed -i "s/^#\?supervised .*/supervised systemd/" "${VALKEY_CONF}"

# Require password for all clients.
if grep -q "^#\?requirepass " "${VALKEY_CONF}"; then
  sed -i "s|^#\?requirepass .*|requirepass ${VALKEY_PASSWORD}|" "${VALKEY_CONF}"
else
  echo "requirepass ${VALKEY_PASSWORD}" >>"${VALKEY_CONF}"
fi

# Enable AOF persistence.
if grep -q "^#\?appendonly " "${VALKEY_CONF}"; then
  sed -i "s/^#\?appendonly .*/appendonly yes/" "${VALKEY_CONF}"
else
  echo "appendonly yes" >>"${VALKEY_CONF}"
fi

# Resolve service name defensively.
if systemctl list-unit-files | grep -q '^valkey-server\.service'; then
  VALKEY_SERVICE="valkey-server"
elif systemctl list-unit-files | grep -q '^valkey\.service'; then
  VALKEY_SERVICE="valkey"
elif systemctl list-unit-files | grep -q '^redis-server\.service'; then
  VALKEY_SERVICE="redis-server"
fi

if [[ -z "${VALKEY_SERVICE}" ]]; then
  echo "ERROR: Could not detect Valkey/Redis service name."
  exit 1
fi

[[ "${VALKEY_SERVICE}" != "valkey" ]] && systemctl enable "${VALKEY_SERVICE}"
systemctl restart "${VALKEY_SERVICE}"

#######################################
# 8) BACKUP SCRIPT (POSTGRES -> R2)
#######################################

cat >/usr/local/sbin/pg_backup_to_r2.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
umask 027

source /etc/e-learning/backup-r2.env

TS="$(date -u +%Y%m%dT%H%M%SZ)"
HOST="$(hostname -s)"
TMP_DIR="$(mktemp -d /tmp/pgbackup.XXXXXX)"
trap 'rm -rf "$TMP_DIR"' EXIT

DUMP_FILE="${TMP_DIR}/${PGDATABASE}_${TS}.dump"
ARCHIVE_FILE="${DUMP_FILE}.zst"
S3_KEY="${HOST}/${PGDATABASE}/${PGDATABASE}_${TS}.dump.zst"

# Dump as postgres OS user (peer auth), then compress and upload.
sudo -u postgres pg_dump \
  --format=custom \
  --no-owner \
  --no-privileges \
  --dbname="${PGDATABASE}" \
  --file="${DUMP_FILE}"

zstd -19 --quiet "${DUMP_FILE}" -o "${ARCHIVE_FILE}"

aws --endpoint-url "https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com" \
  s3 cp "${ARCHIVE_FILE}" "s3://${R2_BUCKET}/${S3_KEY}" \
  --no-progress
EOF

chmod 0700 /usr/local/sbin/pg_backup_to_r2.sh
chown root:root /usr/local/sbin/pg_backup_to_r2.sh

cat >/etc/e-learning/backup-r2.env <<EOF
AWS_ACCESS_KEY_ID=${R2_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY=${R2_SECRET_ACCESS_KEY}
AWS_DEFAULT_REGION=auto
R2_ACCOUNT_ID=${R2_ACCOUNT_ID}
R2_BUCKET=${R2_BUCKET}
PGDATABASE=${APP_DB_NAME}
EOF

chmod 0600 /etc/e-learning/backup-r2.env
chown root:root /etc/e-learning/backup-r2.env

#######################################
# 9) SCHEDULE NIGHTLY BACKUPS
#######################################

cat >/etc/systemd/system/pg-backup-r2.service <<'EOF'
[Unit]
Description=PostgreSQL backup upload to Cloudflare R2
After=network-online.target postgresql.service
Wants=network-online.target

[Service]
Type=oneshot
User=root
Group=root
ExecStart=/usr/local/sbin/pg_backup_to_r2.sh
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/tmp
EOF

cat >/etc/systemd/system/pg-backup-r2.timer <<EOF
[Unit]
Description=Nightly PostgreSQL backup to Cloudflare R2

[Timer]
OnCalendar=*-*-* ${BACKUP_HOUR_UTC}:${BACKUP_MINUTE_UTC}:00
RandomizedDelaySec=1800
Persistent=true

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now pg-backup-r2.timer

#######################################
# 10) FIREWALL RULES
#######################################

# Deny all inbound except explicit allow rules.
ufw allow OpenSSH
ufw --force default deny incoming
ufw --force default allow outgoing

# Allow app droplets to reach Postgres + Valkey.
ufw allow from "${API_ALLOWED_CIDR}" to any port 5432 proto tcp
ufw allow from "${WORKER_ALLOWED_CIDR}" to any port 5432 proto tcp
ufw allow from "${API_ALLOWED_CIDR}" to any port 6379 proto tcp
ufw allow from "${WORKER_ALLOWED_CIDR}" to any port 6379 proto tcp

# Optional direct DBeaver TCP access.
if [[ "${ALLOW_DBEAVER_DIRECT}" == "true" ]]; then
  ufw allow from "${DBEAVER_IP_CIDR}" to any port 5432 proto tcp
fi

ufw --force enable

#######################################
# 11) FINAL OUTPUT + QUICK CHECKS
#######################################

echo "Bootstrap complete."
echo "Data private IP: ${DATA_PRIVATE_IP}"
echo "PostgreSQL service: active expected"
echo "Valkey/Redis service: ${VALKEY_SERVICE}"
echo "Runtime app secrets file: /etc/e-learning/runtime.env"
echo "Backup timer: pg-backup-r2.timer"

echo ""
echo "Quick checks:"
echo "  sudo ss -lntp | egrep '(:5432|:6379)'"
echo "  sudo systemctl status postgresql --no-pager"
echo "  sudo systemctl status ${VALKEY_SERVICE} --no-pager"
echo "  sudo systemctl status pg-backup-r2.timer --no-pager"
