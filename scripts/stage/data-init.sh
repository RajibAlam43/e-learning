#!/usr/bin/env bash
# DigitalOcean Valkey droplet bootstrap:
# 1) Install + secure Valkey/Redis-compatible server
# 2) Configure private-network access for API + Worker droplets
# 3) Apply baseline host firewall rules
#
# Intended for Ubuntu/Debian droplets.
# Run as root.

set -euo pipefail
umask 027

#######################################
# 0) EDIT THESE VALUES BEFORE RUNNING
#######################################

VALKEY_PASSWORD="REPLACE_WITH_STRONG_PASSWORD"

API_ALLOWED_CIDR="10.104.16.2/32"
WORKER_ALLOWED_CIDR="10.104.16.3/32"

#######################################
# 1) AUTO-DETECT PRIVATE IP
#######################################

DATA_PRIVATE_IP="$(curl -fsS http://169.254.169.254/metadata/v1/interfaces/private/0/ipv4/address || true)"

if [[ -z "${DATA_PRIVATE_IP}" ]]; then
  echo "ERROR: Could not detect private IP from DO metadata."
  exit 1
fi

#######################################
# 2) VALIDATE INPUTS
#######################################

required_vars=(
  VALKEY_PASSWORD
  API_ALLOWED_CIDR
  WORKER_ALLOWED_CIDR
)

for v in "${required_vars[@]}"; do
  if [[ -z "${!v:-}" || "${!v}" == "REPLACE_WITH_STRONG_PASSWORD" ]]; then
    echo "ERROR: ${v} is not set."
    exit 1
  fi
done

#######################################
# 3) INSTALL PACKAGES
#######################################

export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y \
  ca-certificates \
  curl \
  ufw \
  unattended-upgrades

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
  echo "ERROR: Could not find valkey/redis-server package."
  exit 1
fi

#######################################
# 4) STORE RUNTIME VALUES
#######################################

install -d -m 0750 /etc/e-learning

cat >/etc/e-learning/runtime.env <<EOF
SPRING_DATA_VALKEY_HOST=${DATA_PRIVATE_IP}
SPRING_DATA_VALKEY_PORT=6379
SPRING_DATA_VALKEY_PASSWORD=${VALKEY_PASSWORD}
EOF

chmod 0600 /etc/e-learning/runtime.env
chown root:root /etc/e-learning/runtime.env

#######################################
# 5) CONFIGURE VALKEY / REDIS
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

# Require password.
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

systemctl enable "${VALKEY_SERVICE}"
systemctl restart "${VALKEY_SERVICE}"

#######################################
# 6) FIREWALL RULES
#######################################

ufw allow OpenSSH
ufw --force default deny incoming
ufw --force default allow outgoing

# Allow API + worker to reach Valkey only.
ufw allow from "${API_ALLOWED_CIDR}" to any port 6379 proto tcp
ufw allow from "${WORKER_ALLOWED_CIDR}" to any port 6379 proto tcp

ufw --force enable

#######################################
# 7) FINAL OUTPUT
#######################################

echo "Valkey bootstrap complete."
echo "Valkey private IP: ${DATA_PRIVATE_IP}"
echo "Valkey/Redis service: ${VALKEY_SERVICE}"
echo "Runtime app secrets file: /etc/e-learning/runtime.env"

echo ""
echo "Quick checks:"
echo "  sudo ss -lntp | grep ':6379'"
echo "  sudo systemctl status ${VALKEY_SERVICE} --no-pager"
echo "  sudo cat /etc/e-learning/runtime.env"