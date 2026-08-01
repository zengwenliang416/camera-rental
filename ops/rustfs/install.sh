#!/usr/bin/env bash
set -euo pipefail

root="${1:-/opt/camera-rental/rustfs}"
source_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

command -v docker >/dev/null
docker compose version >/dev/null
test -f "${root}/.env"

mkdir -p "${root}/data" "${root}/logs" "${root}/backups" "${root}/bin"
install -m 0644 "${source_dir}/docker-compose.yml" "${root}/docker-compose.yml"
install -m 0755 "${source_dir}/bootstrap.sh" "${root}/bootstrap.sh"
install -m 0755 "${source_dir}/backup.sh" "${root}/backup.sh"
chown -R 10001:10001 "${root}/data" "${root}/logs"
chmod 700 "${root}" "${root}/backups"
chmod 600 "${root}/.env"

docker compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" pull
docker compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" up -d

for _ in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:9000/health/ready >/dev/null; then
    "${root}/bootstrap.sh" "${root}"
    docker compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" ps
    exit 0
  fi
  sleep 2
done

docker compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" logs --tail=100
exit 1
