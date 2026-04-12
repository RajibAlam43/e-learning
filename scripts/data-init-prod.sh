#!/usr/bin/env bash
set -euxo pipefail

# ============================================================
# REQUIRED CONFIG - REPLACE BEFORE CREATING THE DROPLET
# ============================================================

VALKEY_PASSWORD="replace_me_strong_password"
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
  lsb-release

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

# ============================================================
# DIRECTORIES
# ============================================================

mkdir -p /opt/e-learning-data/valkey
cd /opt/e-learning-data

# ============================================================
# ENV FILE
# ============================================================

cat > /opt/e-learning-data/.env <<EOF
VALKEY_PASSWORD=${VALKEY_PASSWORD}
EOF

chmod 600 /opt/e-learning-data/.env

# ============================================================
# DOCKER COMPOSE
# Port is exposed intentionally.
# Restrict access with DigitalOcean Firewall.
# ============================================================

cat > /opt/e-learning-data/docker-compose.yml <<EOF
services:
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
Description=E-Learning Valkey Service
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

echo "Prod data droplet ready."