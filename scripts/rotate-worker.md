
- [ ] Create a script on the **data droplet**
- [ ] Run it with old/new worker IPs
- [ ] Verify PostgreSQL ACL + UFW rules updated
- [ ] Test connectivity from the new worker droplet
- [ ] (Optional) update `scripts/data-init.sh` source-of-truth for future reprovisioning

### 1) SSH into the data droplet
```bash
ssh root@<DATA_DROPLET_PUBLIC_IP>
```

### 2) Create the script
```bash
cat > /root/rotate-worker-access.sh <<'EOF'
#!/usr/bin/env bash
# Rotates worker access for PostgreSQL + Valkey on the data droplet.
# - Updates PostgreSQL pg_hba.conf to replace old worker IP with new worker IP
# - Updates UFW allow rules for ports 5432 and 6379
# - Reloads PostgreSQL (no full restart)
#
# Usage:
#   sudo bash /root/rotate-worker-access.sh \
#     --new-ip 10.50.2.44 \
#     --old-ip 10.10.1.23 \
#     --db-name elearning \
#     --db-user elearning_app
#
# Notes:
# - --old-ip is optional (for first-time setup, you can omit it)
# - Requires root/sudo
# - This only updates host-level allowlists; routing between VPCs must exist separately.

set -euo pipefail

NEW_IP=""
OLD_IP=""
DB_NAME="elearning"
DB_USER="elearning_app"

usage() {
  cat <<USAGE
Usage:
  $0 --new-ip <IP> [--old-ip <IP>] [--db-name <name>] [--db-user <user>]

Required:
  --new-ip    New worker droplet IP to allow

Optional:
  --old-ip    Old worker droplet IP to remove
  --db-name   Database name (default: elearning)
  --db-user   Database user (default: elearning_app)
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --new-ip) NEW_IP="${2:-}"; shift 2 ;;
    --old-ip) OLD_IP="${2:-}"; shift 2 ;;
    --db-name) DB_NAME="${2:-}"; shift 2 ;;
    --db-user) DB_USER="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1"; usage; exit 1 ;;
  esac
done

if [[ -z "$NEW_IP" ]]; then
  echo "ERROR: --new-ip is required"
  usage
  exit 1
fi

# Basic IPv4 validation (simple format check).
is_ipv4() {
  [[ "$1" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]
}
if ! is_ipv4 "$NEW_IP"; then
  echo "ERROR: --new-ip is not a valid IPv4 format: $NEW_IP"
  exit 1
fi
if [[ -n "$OLD_IP" ]] && ! is_ipv4 "$OLD_IP"; then
  echo "ERROR: --old-ip is not a valid IPv4 format: $OLD_IP"
  exit 1
fi

# Find PostgreSQL HBA file from installed version.
PG_VERSION="$(ls /etc/postgresql | sort -V | tail -n1)"
PG_HBA="/etc/postgresql/${PG_VERSION}/main/pg_hba.conf"

if [[ ! -f "$PG_HBA" ]]; then
  echo "ERROR: Could not find pg_hba.conf at $PG_HBA"
  exit 1
fi

echo "Using pg_hba.conf: $PG_HBA"

add_line_if_missing() {
  local line="$1"
  local file="$2"
  grep -qxF "$line" "$file" || echo "$line" >> "$file"
}

# Backup pg_hba before changing.
cp "$PG_HBA" "${PG_HBA}.bak.$(date +%Y%m%d%H%M%S)"

# If old IP provided, remove exact matching DB line.
if [[ -n "$OLD_IP" ]]; then
  sed -i \
    "\|^host[[:space:]]\+${DB_NAME}[[:space:]]\+${DB_USER}[[:space:]]\+${OLD_IP}/32[[:space:]]\+scram-sha-256$|d" \
    "$PG_HBA"
fi

# Add new worker IP allow line.
add_line_if_missing "host ${DB_NAME} ${DB_USER} ${NEW_IP}/32 scram-sha-256" "$PG_HBA"

# Reload PostgreSQL config.
systemctl reload postgresql

# UFW rules: remove old, add new for PostgreSQL and Valkey.
if [[ -n "$OLD_IP" ]]; then
  ufw delete allow from "${OLD_IP}" to any port 5432 proto tcp || true
  ufw delete allow from "${OLD_IP}" to any port 6379 proto tcp || true
fi

ufw allow from "${NEW_IP}" to any port 5432 proto tcp
ufw allow from "${NEW_IP}" to any port 6379 proto tcp

echo ""
echo "Done. Current relevant pg_hba lines:"
grep -E "^host[[:space:]]+${DB_NAME}[[:space:]]+${DB_USER}[[:space:]]+.*scram-sha-256$" "$PG_HBA" || true

echo ""
echo "Done. Current UFW rules for 5432/6379:"
ufw status | grep -E "5432|6379" || true

echo ""
echo "Next: test from new worker droplet:"
echo "  nc -vz <DATA_PRIVATE_IP> 5432"
echo "  nc -vz <DATA_PRIVATE_IP> 6379"
EOF
```

### 3) Make it executable
```bash
chmod +x /root/rotate-worker-access.sh
```

### 4) Run it
```bash
sudo /root/rotate-worker-access.sh \
  --new-ip <NEW_WORKER_IP> \
  --old-ip <OLD_WORKER_IP> \
  --db-name elearning \
  --db-user elearning_app
```

If you don’t know old IP (or no old worker rule exists), run without `--old-ip`:
```bash
sudo /root/rotate-worker-access.sh \
  --new-ip <NEW_WORKER_IP> \
  --db-name elearning \
  --db-user elearning_app
```

### 5) Validate from the new worker droplet
```bash
nc -vz <DATA_PRIVATE_IP> 5432
nc -vz <DATA_PRIVATE_IP> 6379
```

### 6) Keep future bootstrap in sync
Update `WORKER_PRIVATE_IP` in `scripts/data-init.sh` so your source-of-truth matches the new worker IP; otherwise next reprovision may reintroduce stale rules.
