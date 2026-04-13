#!/usr/bin/env bash
set -euo pipefail

APP_NAME="e-learning-worker"

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
chmod a+r /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
  > /etc/apt/sources.list.d/adoptium.list

apt-get update
apt-get install -y temurin-25-jre

id -u deploy >/dev/null 2>&1 || useradd -m -s /bin/bash deploy
install -d -o deploy -g deploy -m 0750 /opt/e-learning
install -o deploy -g deploy -m 0600 /dev/null /opt/e-learning/.env
install -d -o deploy -g deploy -m 0755 /opt/e-learning/releases

cat > /etc/sudoers.d/e-learning-worker <<'EOF'
deploy ALL=(root) NOPASSWD: /bin/systemctl restart e-learning-worker, /bin/systemctl is-active e-learning-worker
EOF
chmod 440 /etc/sudoers.d/e-learning-worker

cat > /etc/systemd/system/${APP_NAME}.service <<'EOF'
[Unit]
Description=E-Learning Worker
After=network-online.target
Wants=network-online.target

[Service]
User=deploy
Group=deploy
WorkingDirectory=/opt/e-learning
EnvironmentFile=/opt/e-learning/.env
ExecStart=/usr/bin/java -jar /opt/e-learning/current/worker.jar
Restart=always
RestartSec=5
SuccessExitStatus=143

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

systemctl daemon-reload
systemctl enable ${APP_NAME}
