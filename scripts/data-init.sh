#!/usr/bin/env bash
# DigitalOcean droplet init script for:
# 1) PostgreSQL setup + start
# 2) Valkey setup + start
# 3) Nightly PostgreSQL backup upload to Cloudflare R2
# 4) Baseline security hardening
# 5) Private-network access for API + worker droplets
#
# Paste this into Droplet "User Data".
# Assumes Ubuntu/Debian-based droplet and root execution.

set -euo pipefail
umask 027

#######################################
# 0) REQUIRED: EDIT THESE VALUES
#######################################

# Database settings
APP_DB_NAME="elearning"
APP_DB_USER="elearning_app"

# Auto-detect private IPv4 from DigitalOcean metadata service.
# Works after droplet boots (inside User Data script).
DATA_PRIVATE_IP="$(curl -fsS http://169.254.169.254/metadata/v1/interfaces/private/0/ipv4/address || true)"

if [[ -z "${DATA_PRIVATE_IP}" ]]; then
  echo "ERROR: Could not auto-detect DATA_PRIVATE_IP from metadata."
  echo "Make sure this droplet is attached to a VPC with private networking enabled."
  exit 1
fi

# Private network IPs of app droplets allowed to connect.
API_PRIVATE_IP="REPLACE_ME"
WORKER_PRIVATE_IP="REPLACE_ME"

# DBeaver direct-access toggle.
# Recommended: keep "false" and use SSH tunnel instead.
ALLOW_DBEAVER_DIRECT="false"
DBEAVER_IP_CIDR="REPLACE_ME" # Example: 203.0.113.10/32

# Cloudflare R2 settings
R2_ACCOUNT_ID="REPLACE_ME"
R2_BUCKET="REPLACE_ME"
R2_ACCESS_KEY_ID="REPLACE_ME"
R2_SECRET_ACCESS_KEY="REPLACE_ME"

# Backup schedule (UTC)
BACKUP_HOUR_UTC="03"
BACKUP_MINUTE_UTC="20"

#######################################
# 1) Validate required inputs
#######################################
required_vars=(
  DATA_PRIVATE_IP
  API_PRIVATE_IP
  WORKER_PRIVATE_IP
  R2_ACCOUNT_ID
  R2_BUCKET
  R2_ACCESS_KEY_ID
  R2_SECRET_ACCESS_KEY
)

for v in "${required_vars[@]}"; do
  if [[ -z "${!v:-}" || "${!v}" == "REPLACE_ME" ]]; then
    echo "ERROR: $v is not set. Edit the script before use."
    exit 1
  fi
done

if [[ "${ALLOW_DBEAVER_DIRECT}" == "true" ]] && [[ -z "${DBEAVER_IP_CIDR:-}" || "${DBEAVER_IP_CIDR}" == "REPLACE_ME" ]]; then
  echo "ERROR: DBEAVER_IP_CIDR must be set when ALLOW_DBEAVER_DIRECT=true."
  exit 1
fi

add_line_if_missing() {
  local line="$1"
  local file="$2"
  grep -qxF "$line" "$file" || echo "$line" >> "$file"
}

#######################################
# 2) Install packages and security basics
#######################################
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y \
  postgresql postgresql-contrib \
  awscli zstd \
  ca-certificates curl gnupg lsb-release \
  ufw unattended-upgrades openssl

if apt-cache show valkey >/dev/null 2>&1; then
  apt-get install -y valkey
elif apt-cache show valkey-server >/dev/null 2>&1; then
  apt-get install -y valkey-server
else
  echo "ERROR: Could not find valkey package in apt repositories."
  exit 1
fi

#######################################
# 3) Generate credentials and store secrets
#######################################
APP_DB_PASSWORD="$(openssl rand -base64 36 | tr -d '\n')"
VALKEY_PASSWORD="$(openssl rand -base64 36 | tr -d '\n')"

install -d -m 0750 /etc/e-learning

# This file is used as the source of truth for app connection values.
cat >/etc/e-learning/runtime.env <<EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://${DATA_PRIVATE_IP}:5432/${APP_DB_NAME}
SPRING_DATASOURCE_USERNAME=${APP_DB_USER}
SPRING_DATASOURCE_PASSWORD=${APP_DB_PASSWORD}
SPRING_DATA_REDIS_HOST=${DATA_PRIVATE_IP}
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=${VALKEY_PASSWORD}
EOF
chmod 0600 /etc/e-learning/runtime.env
chown root:root /etc/e-learning/runtime.env

#######################################
# 4) Configure PostgreSQL
#######################################
PG_VERSION="$(ls /etc/postgresql | sort -V | tail -n1)"
PG_CONF="/etc/postgresql/${PG_VERSION}/main/postgresql.conf"
PG_HBA="/etc/postgresql/${PG_VERSION}/main/pg_hba.conf"

# Listen on loopback + private VPC interface only.
sed -i "s/^#\?listen_addresses.*/listen_addresses = '127.0.0.1,${DATA_PRIVATE_IP}'/" "$PG_CONF"

# Use SCRAM for password hashing.
if grep -q "^#\?password_encryption" "$PG_CONF"; then
  sed -i "s/^#\?password_encryption.*/password_encryption = scram-sha-256/" "$PG_CONF"
else
  echo "password_encryption = scram-sha-256" >>"$PG_CONF"
fi

# Allow only known clients.
add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} 127.0.0.1/32 scram-sha-256" "$PG_HBA"
add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} ${API_PRIVATE_IP}/32 scram-sha-256" "$PG_HBA"
add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} ${WORKER_PRIVATE_IP}/32 scram-sha-256" "$PG_HBA"

if [[ "${ALLOW_DBEAVER_DIRECT}" == "true" ]]; then
  add_line_if_missing "host ${APP_DB_NAME} ${APP_DB_USER} ${DBEAVER_IP_CIDR} scram-sha-256" "$PG_HBA"
fi

systemctl enable postgresql
systemctl restart postgresql

sudo -u postgres psql <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${APP_DB_USER}') THEN
    CREATE ROLE ${APP_DB_USER} LOGIN PASSWORD '${APP_DB_PASSWORD}';
  ELSE
    ALTER ROLE ${APP_DB_USER} WITH LOGIN PASSWORD '${APP_DB_PASSWORD}';
  END IF;
END
\$\$;

DO \$\$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '${APP_DB_NAME}') THEN
    CREATE DATABASE ${APP_DB_NAME} OWNER ${APP_DB_USER};
  END IF;
END
\$\$;
SQL

#######################################
# 5) Configure Valkey
#######################################
VALKEY_CONF=""
for c in /etc/valkey/valkey.conf /etc/valkey.conf /etc/redis/redis.conf; do
  if [[ -f "$c" ]]; then
    VALKEY_CONF="$c"
    break
  fi
done

if [[ -z "$VALKEY_CONF" ]]; then
  echo "ERROR: Valkey config file not found."
  exit 1
fi

# Listen on loopback + private VPC interface only.
sed -i "s/^#\?bind .*/bind 127.0.0.1 ${DATA_PRIVATE_IP}/" "$VALKEY_CONF"
sed -i "s/^#\?protected-mode .*/protected-mode yes/" "$VALKEY_CONF"
sed -i "s/^#\?port .*/port 6379/" "$VALKEY_CONF"
sed -i "s/^#\?supervised .*/supervised systemd/" "$VALKEY_CONF"

if grep -q "^#\?requirepass " "$VALKEY_CONF"; then
  sed -i "s|^#\?requirepass .*|requirepass ${VALKEY_PASSWORD}|" "$VALKEY_CONF"
else
  echo "requirepass ${VALKEY_PASSWORD}" >>"$VALKEY_CONF"
fi

if grep -q "^#\?appendonly " "$VALKEY_CONF"; then
  sed -i "s/^#\?appendonly .*/appendonly yes/" "$VALKEY_CONF"
else
  echo "appendonly yes" >>"$VALKEY_CONF"
fi

VALKEY_SERVICE=""
if systemctl list-unit-files | grep -q '^valkey-server\.service'; then
  VALKEY_SERVICE="valkey-server"
elif systemctl list-unit-files | grep -q '^valkey\.service'; then
  VALKEY_SERVICE="valkey"
elif systemctl list-unit-files | grep -q '^redis-server\.service'; then
  VALKEY_SERVICE="redis-server"
else
  echo "ERROR: Could not detect Valkey systemd service name."
  exit 1
fi

systemctl enable "$VALKEY_SERVICE"
systemctl restart "$VALKEY_SERVICE"

#######################################
# 6) Backup script (PostgreSQL -> R2)
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
# 7) Schedule nightly backups
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
# 8) Firewall rules
#######################################
# SSH open; all other inbound denied unless explicitly allowed below.
ufw allow OpenSSH
ufw --force default deny incoming
ufw --force default allow outgoing

# Allow API and worker droplets only.
ufw allow from "${API_PRIVATE_IP}" to any port 5432 proto tcp
ufw allow from "${WORKER_PRIVATE_IP}" to any port 5432 proto tcp
ufw allow from "${API_PRIVATE_IP}" to any port 6379 proto tcp
ufw allow from "${WORKER_PRIVATE_IP}" to any port 6379 proto tcp

# Optional: direct DBeaver access from a single trusted IP/CIDR.
if [[ "${ALLOW_DBEAVER_DIRECT}" == "true" ]]; then
  ufw allow from "${DBEAVER_IP_CIDR}" to any port 5432 proto tcp
fi

ufw --force enable

#######################################
# 9) Final output
#######################################
echo "Bootstrap complete."
echo "PostgreSQL + Valkey started and enabled."
echo "App secrets source: /etc/e-learning/runtime.env (root-only)."
echo "Backup timer: pg-backup-r2.timer"
echo "For DBeaver, SSH tunnel is recommended over direct DB exposure."

#######################################
# 10) Verify
#######################################
# On data droplet: ensure DB/Valkey listen on private + loopback only
# sudo ss -lntp | egrep '(:5432|:6379)'

# On API/worker droplets: verify route to data private IP
# nc -vz <DATA_PRIVATE_IP> 5432
# nc -vz <DATA_PRIVATE_IP> 6379

# Backup timer check
# systemctl status pg-backup-r2.timer --no-pager

