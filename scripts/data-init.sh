#!/usr/bin/env bash
set -euxo pipefail

# ============================================================
# REQUIRED CONFIG - REPLACE BEFORE CREATING THE DROPLET
# ============================================================

POSTGRES_DB="elearning"
POSTGRES_USER="appuser"
POSTGRES_PASSWORD="replace_me_strong_password"

VALKEY_PASSWORD="replace_me_strong_password"

R2_BUCKET="replace-me-bucket"
R2_ENDPOINT="https://<accountid>.r2.cloudflarestorage.com"
AWS_ACCESS_KEY_ID="replace-me"
AWS_SECRET_ACCESS_KEY="replace-me"
AWS_DEFAULT_REGION="auto"

BACKUP_SCHEDULE_CRON="0 3 * * *"

POSTGRES_IMAGE="postgres:16"
VALKEY_IMAGE="valkey/valkey:7"

# ============================================================
# BASE PACKAGES
# ============================================================

export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  gnupg \
  lsb-release \
  awscli \
  cron

# ============================================================
# INSTALL DOCKER + COMPOSE
# ============================================================

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable docker
systemctl start docker
systemctl enable cron
systemctl start cron

# ============================================================
# DIRECTORIES
# ============================================================

mkdir -p /opt/e-learning-data/postgres
mkdir -p /opt/e-learning-data/valkey
cd /opt/e-learning-data

# ============================================================
# ENV FILE
# ============================================================

cat > /opt/e-learning-data/.env <<EOF
POSTGRES_DB=${POSTGRES_DB}
POSTGRES_USER=${POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
VALKEY_PASSWORD=${VALKEY_PASSWORD}
R2_BUCKET=${R2_BUCKET}
R2_ENDPOINT=${R2_ENDPOINT}
AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION}
EOF

chmod 600 /opt/e-learning-data/.env

# ============================================================
# POSTGRES INIT
# Enables external connections.
# Security is controlled by DigitalOcean Firewall, not this script.
# ============================================================

mkdir -p /opt/e-learning-data/postgres-init

cat > /opt/e-learning-data/postgres-init/01-configure-access.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

PGDATA_DIR="${PGDATA:-/var/lib/postgresql/data}"

echo "listen_addresses='*'" >> "${PGDATA_DIR}/postgresql.conf"

cat >> "${PGDATA_DIR}/pg_hba.conf" <<'HBAEOF'
host    all             all             0.0.0.0/0               scram-sha-256
host    all             all             ::/0                    scram-sha-256
HBAEOF
EOF

chmod +x /opt/e-learning-data/postgres-init/01-configure-access.sh

# ============================================================
# DOCKER COMPOSE
# Ports are exposed intentionally.
# Restrict access with DigitalOcean Firewall.
# ============================================================

cat > /opt/e-learning-data/docker-compose.yml <<EOF
services:
  postgres:
    image: ${POSTGRES_IMAGE}
    container_name: postgres
    restart: always
    env_file:
      - /opt/e-learning-data/.env
    ports:
      - "5432:5432"
    volumes:
      - /opt/e-learning-data/postgres:/var/lib/postgresql/data
      - /opt/e-learning-data/postgres-init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \$\$POSTGRES_USER -d \$\$POSTGRES_DB"]
      interval: 10s
      timeout: 5s
      retries: 10

  valkey:
    image: ${VALKEY_IMAGE}
    container_name: valkey
    restart: always
    command: ["valkey-server", "--bind", "0.0.0.0", "--requirepass", "${VALKEY_PASSWORD}", "--appendonly", "yes"]
    ports:
      - "6379:6379"
    volumes:
      - /opt/e-learning-data/valkey:/data
EOF

# ============================================================
# SYSTEMD SERVICE
# ============================================================

cat > /etc/systemd/system/e-learning-data.service <<'EOF'
[Unit]
Description=E-Learning Data Services
Requires=docker.service
After=docker.service network.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/e-learning-data
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable e-learning-data
systemctl start e-learning-data

# ============================================================
# BACKUP SCRIPT
# ============================================================

cat > /usr/local/bin/db-backup.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

source /opt/e-learning-data/.env

DATE=$(date +"%Y-%m-%d_%H-%M")
BACKUP_FILE="/tmp/backup_${POSTGRES_DB}_${DATE}.sql.gz"

docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" postgres \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" \
  | gzip > "$BACKUP_FILE"

aws s3 cp "$BACKUP_FILE" "s3://$R2_BUCKET/$(basename "$BACKUP_FILE")" \
  --endpoint-url "$R2_ENDPOINT"

rm -f "$BACKUP_FILE"
EOF

chmod +x /usr/local/bin/db-backup.sh

cat > /etc/cron.d/db-backup <<EOF
${BACKUP_SCHEDULE_CRON} root /usr/local/bin/db-backup.sh >> /var/log/db-backup.log 2>&1
EOF

chmod 644 /etc/cron.d/db-backup
systemctl restart cron

echo "Stage data droplet ready."