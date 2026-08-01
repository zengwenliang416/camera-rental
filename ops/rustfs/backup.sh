#!/usr/bin/env bash
set -euo pipefail

root="${1:-/opt/camera-rental/rustfs}"
source "${root}/.env"
: "${RUSTFS_APP_ACCESS_KEY:?}"
: "${RUSTFS_APP_SECRET_KEY:?}"
: "${RUSTFS_BUCKET:?}"

target="${root}/backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "${target}"

docker run --rm --network host \
  -e AWS_ACCESS_KEY_ID="${RUSTFS_APP_ACCESS_KEY}" \
  -e AWS_SECRET_ACCESS_KEY="${RUSTFS_APP_SECRET_KEY}" \
  -e AWS_DEFAULT_REGION="${RUSTFS_REGION:-us-east-1}" \
  -v "${target}:/backup" \
  amazon/aws-cli:2 \
  --endpoint-url http://127.0.0.1:9000 s3 sync "s3://${RUSTFS_BUCKET}" /backup

find "${root}/backups" -mindepth 1 -maxdepth 1 -type d -mtime +14 -exec rm -rf {} +
