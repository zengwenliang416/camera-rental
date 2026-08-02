#!/usr/bin/env bash
set -euo pipefail

root="${1:-/opt/camera-rental/rustfs}"
deploy_root="${2:-/opt/camera-rental}"
env_file="${root}/.env"
app_env_file="${deploy_root}/shared/rustfs-app.env"
health_url="${RUSTFS_HEALTH_URL:-http://127.0.0.1:9000/health/ready}"

if [ ! -f "${env_file}" ]; then
  echo "[rustfs][error] RustFS is not provisioned: ${env_file} is missing" >&2
  exit 1
fi
if [ ! -f "${app_env_file}" ]; then
  echo "[rustfs][error] application credentials are missing: ${app_env_file}" >&2
  exit 1
fi

for key in \
  RUSTFS_APP_ACCESS_KEY \
  RUSTFS_APP_SECRET_KEY \
  RUSTFS_REGION \
  RUSTFS_BUCKET \
  RUSTFS_PUBLIC_ENDPOINT; do
  if ! grep -Eq "^${key}=.+" "${app_env_file}"; then
    echo "[rustfs][error] ${key} is missing from ${app_env_file}" >&2
    exit 1
  fi
done

curl -fsS --max-time 10 "${health_url}" >/dev/null
echo "[rustfs] pre-provisioned service is healthy"
