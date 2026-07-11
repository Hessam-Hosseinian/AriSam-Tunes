#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
IP="${DEV_HOST_IP:-$("$ROOT_DIR/tools/detect-lan-ip.sh")}"

if [ -z "$IP" ]; then
  echo "Could not detect a LAN IP. Set DEV_HOST_IP manually, for example:" >&2
  echo "DEV_HOST_IP=192.168.1.20 tools/dev-compose-up.sh" >&2
  exit 1
fi

API_BASE_URL="http://$IP:${BACKEND_PORT:-8080}"
ENV_FILE="$ROOT_DIR/.env"
TMP_FILE="$ENV_FILE.tmp"

touch "$ENV_FILE"
awk -F= '
  $1 != "API_BASE_URL" && $1 != "PUBLIC_BASE_URL" { print }
' "$ENV_FILE" > "$TMP_FILE"
{
  echo "API_BASE_URL='"$API_BASE_URL"'"
  echo "PUBLIC_BASE_URL='"$API_BASE_URL"'"
} >> "$TMP_FILE"
mv "$TMP_FILE" "$ENV_FILE"

echo "Using API_BASE_URL=$API_BASE_URL"
echo "Using PUBLIC_BASE_URL=$API_BASE_URL"
cd "$ROOT_DIR"
docker compose up --build "$@"
