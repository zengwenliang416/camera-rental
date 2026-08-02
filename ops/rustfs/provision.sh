#!/usr/bin/env bash
set -euo pipefail

root="${1:-/opt/camera-rental/rustfs}"
deploy_root="${2:-/opt/camera-rental}"
source_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
install_script="${RUSTFS_INSTALL_SCRIPT:-${source_dir}/install.sh}"
env_file="${root}/.env"
app_env_file="${deploy_root}/shared/rustfs-app.env"

mkdir -p "${root}" "${deploy_root}/shared"

if [ ! -f "${env_file}" ]; then
  echo "[rustfs] generate persistent root and application credentials"
  umask 077
  cat > "${env_file}" <<EOF
RUSTFS_IMAGE=rustfs/rustfs:1.0.0-beta.12
RUSTFS_ACCESS_KEY=rustfsroot$(openssl rand -hex 8)
RUSTFS_SECRET_KEY=$(openssl rand -hex 32)
RUSTFS_APP_ACCESS_KEY=returnapp$(openssl rand -hex 8)
RUSTFS_APP_SECRET_KEY=$(openssl rand -hex 32)
RUSTFS_REGION=us-east-1
RUSTFS_CORS_ALLOWED_ORIGINS=https://rental.motion-cover.com
RUSTFS_CONSOLE_CORS_ALLOWED_ORIGINS=https://rental.motion-cover.com
RUSTFS_BUCKET=camera-rental-return
RUSTFS_PUBLIC_ENDPOINT=https://storage.motion-cover.com
RUSTFS_RC_VERSION=v0.1.30
EOF
fi

chmod 700 "${root}"
chmod 600 "${env_file}"
bash "${install_script}" "${root}"

umask 077
grep -E '^RUSTFS_(APP_ACCESS_KEY|APP_SECRET_KEY|REGION|BUCKET|PUBLIC_ENDPOINT)=' \
  "${env_file}" > "${app_env_file}"
chmod 600 "${app_env_file}"

echo "[rustfs] provisioned on this host"
echo "[rustfs] application configuration: ${app_env_file}"
