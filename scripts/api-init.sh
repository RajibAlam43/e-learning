#!/usr/bin/env bash
set -euxo pipefail

# ==============================
# REQUIRED CONFIG
# ==============================
APP_NAME="e-learning-api"
DOMAIN="api.example.com"

# ==============================
# BASE PACKAGES
# ==============================
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release debian-keyring debian-archive-keyring apt-transport-https

# ==============================
# INSTALL JAVA 25 (TEMURIN)
# ==============================
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
chmod a+r /etc/apt/keyrings/adoptium.gpg

echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
  > /etc/apt/sources.list.d/adoptium.list

apt-get update
apt-get install -y temurin-25-jre

# ==============================
# INSTALL CADDY
# ==============================
curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/gpg.key \
  | gpg --dearmor -o /etc/apt/keyrings/caddy.gpg
chmod a+r /etc/apt/keyrings/caddy.gpg

echo "deb [signed-by=/etc/apt/keyrings/caddy.gpg] https://dl.cloudsmith.io/public/caddy/stable/deb/debian any-version main" \
  > /etc/apt/sources.list.d/caddy-stable.list

apt-get update
apt-get install -y caddy

# ==============================
# APP USER + DIR
# ==============================
id -u deploy >/dev/null 2>&1 || useradd -m -s /bin/bash deploy
mkdir -p /opt/e-learning
touch /opt/e-learning/.env
chown -R deploy:deploy /opt/e-learning
chmod 600 /opt/e-learning/.env

# ==============================
# SYSTEMD SERVICE
# ==============================
cat > /etc/systemd/system/${APP_NAME}.service <<'EOF'
[Unit]
Description=E-Learning API
After=network.target

[Service]
User=deploy
WorkingDirectory=/opt/e-learning
EnvironmentFile=/opt/e-learning/.env
ExecStart=/usr/bin/java -jar /opt/e-learning/api.jar
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ${APP_NAME}

# ==============================
# CADDY CONFIG
# ==============================
cat > /etc/caddy/Caddyfile <<EOF
${DOMAIN} {
    encode gzip
    reverse_proxy 127.0.0.1:8080
}
EOF

systemctl enable caddy
systemctl restart caddy

echo "API droplet ready."