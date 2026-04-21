#!/usr/bin/env bash
# Fail fast:
# -e: exit on error
# -u: error on unset variables
# -o pipefail: fail pipeline if any command fails
set -euo pipefail

# Name of the systemd service unit we create for the API app.
APP_NAME="e-learning-api"

# Public hostname Caddy will serve for this API.
# IMPORTANT: for prod, change this to api.globalislamicinstitute.com.
DOMAIN="stage-api.globalislamicinstitute.com"
DEPLOY_PUBLIC_KEY="REPLACE_ME"

# Make apt non-interactive (required for unattended cloud-init runs).
export DEBIAN_FRONTEND=noninteractive

# Install base tools needed to add HTTPS apt repositories and keys.
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release debian-keyring debian-archive-keyring apt-transport-https

# Create dedicated keyring directory for external apt repo signing keys.
install -m 0755 -d /etc/apt/keyrings

# Add Adoptium repository signing key (for Temurin JRE packages).
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
chmod a+r /etc/apt/keyrings/adoptium.gpg

# Add Adoptium apt repository for this Ubuntu/Debian codename.
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
  > /etc/apt/sources.list.d/adoptium.list

# Add Caddy repository signing key.
curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/gpg.key \
  | gpg --dearmor -o /etc/apt/keyrings/caddy.gpg
chmod a+r /etc/apt/keyrings/caddy.gpg

# Add official Caddy stable apt repository.
echo "deb [signed-by=/etc/apt/keyrings/caddy.gpg] https://dl.cloudsmith.io/public/caddy/stable/deb/debian any-version main" \
  > /etc/apt/sources.list.d/caddy-stable.list

# Refresh package index and install Java runtime + Caddy reverse proxy.
apt-get update
apt-get install -y temurin-25-jre caddy

# Create non-root deploy user if it does not exist.
id -u deploy >/dev/null 2>&1 || useradd -m -s /bin/bash deploy

# Create non-root deploy user if it does not exist.
id -u deploy >/dev/null 2>&1 || useradd -m -s /bin/bash deploy

# Configure SSH access for deploy user.
install -d -o deploy -g deploy -m 700 /home/deploy/.ssh
touch /home/deploy/.ssh/authorized_keys
grep -qxF "${DEPLOY_PUBLIC_KEY}" /home/deploy/.ssh/authorized_keys || \
  echo "${DEPLOY_PUBLIC_KEY}" >> /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys

# Create app directories with least-privilege ownership/permissions:
# /opt/e-learning/.env     -> runtime env vars (injected by deploy pipeline)
# /opt/e-learning/releases -> immutable release dirs
install -d -o deploy -g deploy -m 0750 /opt/e-learning
install -o deploy -g deploy -m 0600 /dev/null /opt/e-learning/.env
install -d -o deploy -g deploy -m 0755 /opt/e-learning/releases

# Allow deploy user to restart/check only API service via sudo (principle of least privilege).
cat > /etc/sudoers.d/e-learning-api <<'EOF'
deploy ALL=(root) NOPASSWD: /usr/bin/systemctl restart e-learning-api, /usr/bin/systemctl is-active e-learning-api
EOF
chmod 440 /etc/sudoers.d/e-learning-api

# Create systemd unit for API jar process.
cat > /etc/systemd/system/${APP_NAME}.service <<'EOF'
[Unit]
Description=E-Learning API
After=network-online.target
Wants=network-online.target

[Service]
# Run app as non-root user.
User=deploy
Group=deploy

# App root and runtime env file.
WorkingDirectory=/opt/e-learning
EnvironmentFile=/opt/e-learning/.env

# Jar path used by your deploy workflow (symlink /opt/e-learning/current).
ExecStart=/usr/bin/java -jar /opt/e-learning/current/api.jar

# Auto-restart if process exits unexpectedly.
Restart=always
RestartSec=5
SuccessExitStatus=143

# Basic systemd hardening.
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=strict
ReadWritePaths=/opt/e-learning
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true

[Install]
WantedBy=multi-user.target
EOF

# Configure Caddy to terminate TLS and reverse-proxy to local Spring Boot port 8080.
cat > /etc/caddy/Caddyfile <<EOF
${DOMAIN} {
    encode gzip
    reverse_proxy 127.0.0.1:8080
}
EOF

# Reload systemd unit files, enable services at boot, start/restart Caddy now.
systemctl daemon-reload
systemctl enable ${APP_NAME}
systemctl enable caddy
systemctl restart caddy
