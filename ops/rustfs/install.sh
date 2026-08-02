#!/usr/bin/env bash
set -euo pipefail

root="${1:-/opt/camera-rental/rustfs}"
source_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=docker-runtime.sh
source "${source_dir}/docker-runtime.sh"

rustfs_ensure_docker
if [ ! -f "${root}/.env" ]; then
  echo "[rustfs][error] missing persistent environment file: ${root}/.env" >&2
  exit 1
fi

mkdir -p "${root}/data" "${root}/logs" "${root}/backups" "${root}/bin"
install -m 0644 "${source_dir}/docker-compose.yml" "${root}/docker-compose.yml"
install -m 0644 "${source_dir}/docker-runtime.sh" "${root}/docker-runtime.sh"
install -m 0755 "${source_dir}/bootstrap.sh" "${root}/bootstrap.sh"
install -m 0755 "${source_dir}/backup.sh" "${root}/backup.sh"
chown -R 10001:10001 "${root}/data" "${root}/logs"
chmod 700 "${root}" "${root}/backups"
chmod 600 "${root}/.env"

rustfs_compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" pull
rustfs_compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" up -d

for _ in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:9000/health/ready >/dev/null; then
    "${root}/bootstrap.sh" "${root}"
    rustfs_compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" ps
    exit 0
  fi
  sleep 2
done

rustfs_compose --env-file "${root}/.env" -f "${root}/docker-compose.yml" logs --tail=100
exit 1
