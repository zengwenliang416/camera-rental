#!/usr/bin/env bash
set -euo pipefail

RELEASE_ARCHIVE="${1:?usage: server-deploy.sh /tmp/camera-rental-release.tgz}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/camera-rental}"
RELEASE_SHA="${RELEASE_SHA:-manual-$(date +%Y%m%d%H%M%S)}"
KEEP_RELEASES="${KEEP_RELEASES:-5}"
SERVER_SERVICE="${SERVER_SERVICE:-camera-rental-server.service}"
WEB_SERVICE="${WEB_SERVICE:-camera-rental-web.service}"
NGINX_RELOAD_CMD="${NGINX_RELOAD_CMD:-nginx -t && systemctl reload nginx}"
BACKEND_HEALTH_URL="${BACKEND_HEALTH_URL:-http://127.0.0.1:48080/admin-api/system/auth/get-permission-info}"
WEB_HEALTH_ATTEMPTS="${WEB_HEALTH_ATTEMPTS:-60}"
BACKEND_HEALTH_ATTEMPTS="${BACKEND_HEALTH_ATTEMPTS:-90}"
HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-2}"
HEALTH_STABILIZE_SECONDS="${HEALTH_STABILIZE_SECONDS:-5}"

release_dir="${DEPLOY_ROOT}/releases/${RELEASE_SHA}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=deployment-runtime-lib.sh
source "${script_dir}/deployment-runtime-lib.sh"

echo "[deploy] root=${DEPLOY_ROOT}"
echo "[deploy] release=${RELEASE_SHA}"

mkdir -p "${DEPLOY_ROOT}/releases" "${DEPLOY_ROOT}/shared" "${DEPLOY_ROOT}/shared/logs"
rm -rf "${release_dir}"
mkdir -p "${release_dir}"
tar -xzf "${RELEASE_ARCHIVE}" -C "${release_dir}"

test -f "${release_dir}/server/yudao-server.jar"
test -x "${release_dir}/server/apply-migrations.sh"
test -f "${release_dir}/admin/index.html"
test -f "${release_dir}/schedule-center/index.html"
test -f "${release_dir}/server/migrations/20260801_036_customer_return_registration.sql"
test -f "${release_dir}/server/migrations/20260802_037_return_registration_fixed_entry.sql"
verify_return_registration_artifact "${release_dir}/server/yudao-server.jar"

rustfs_source="${release_dir}/ops/rustfs"
rustfs_root="${DEPLOY_ROOT}/rustfs"
if [ -d "${rustfs_source}" ]; then
  echo "[deploy] verify pre-provisioned RustFS"
  bash "${rustfs_source}/verify.sh" "${rustfs_root}" "${DEPLOY_ROOT}"
fi

# Archives built on macOS may preserve owner-only modes. Nginx must be able to
# traverse release directories and read static frontend assets.
chmod 755 "${release_dir}" "${release_dir}/server"
chmod 644 "${release_dir}/server/yudao-server.jar"

echo "[deploy] apply release migrations before activation"
DEPLOY_ROOT="${DEPLOY_ROOT}" RELEASE_SHA="${RELEASE_SHA}" \
  "${release_dir}/server/apply-migrations.sh" "${release_dir}"
for static_dir in admin schedule-center; do
  find "${release_dir}/${static_dir}" -type d -exec chmod 755 {} +
  find "${release_dir}/${static_dir}" -type f -exec chmod 644 {} +
done

has_web_artifact=false
if [ -f "${release_dir}/web/server/index.mjs" ]; then
  has_web_artifact=true
fi

web_port="$(
  sed -n 's/^PORT=//p' "${DEPLOY_ROOT}/shared/web.env" 2>/dev/null \
    | tail -n 1
)"
web_port="${web_port:-3001}"
WEB_HEALTH_URL="${WEB_HEALTH_URL:-http://127.0.0.1:${web_port}/}"

for preserved_dir in staff web; do
  if [ ! -e "${release_dir}/${preserved_dir}" ] && [ -e "${DEPLOY_ROOT}/current/${preserved_dir}" ]; then
    echo "[deploy] preserve existing ${preserved_dir} artifact"
    cp -a "${DEPLOY_ROOT}/current/${preserved_dir}" "${release_dir}/${preserved_dir}"
  fi
done

echo "[deploy] link schedule center under admin route"
rm -rf "${release_dir}/admin/schedule-center"
ln -sfn ../schedule-center "${release_dir}/admin/schedule-center"

ln -sfn "${release_dir}" "${DEPLOY_ROOT}/current"

if systemctl list-unit-files "${SERVER_SERVICE}" >/dev/null 2>&1; then
  echo "[deploy] restart ${SERVER_SERVICE}"
  systemctl restart "${SERVER_SERVICE}"
else
  echo "[deploy][error] ${SERVER_SERVICE} not found" >&2
  exit 1
fi

if [ "${has_web_artifact}" = true ] && systemctl list-unit-files "${WEB_SERVICE}" >/dev/null 2>&1; then
  echo "[deploy] restart ${WEB_SERVICE}"
  systemctl restart "${WEB_SERVICE}"
elif [ "${has_web_artifact}" = false ]; then
  echo "[deploy] skip ${WEB_SERVICE}; web artifact not included"
else
  echo "[deploy][error] ${WEB_SERVICE} not found" >&2
  exit 1
fi

if command -v nginx >/dev/null 2>&1; then
  echo "[deploy] reload nginx"
  bash -lc "${NGINX_RELOAD_CMD}"
else
  echo "[deploy][warn] nginx not installed or not on PATH; static frontends deployed but nginx not reloaded"
fi

wait_for_service_http \
  "${SERVER_SERVICE}" \
  "${BACKEND_HEALTH_URL}" \
  reachable \
  "${BACKEND_HEALTH_ATTEMPTS}" \
  "${HEALTH_INTERVAL_SECONDS}" \
  "${HEALTH_STABILIZE_SECONDS}"

if [ "${has_web_artifact}" = true ]; then
  wait_for_service_http \
    "${WEB_SERVICE}" \
    "${WEB_HEALTH_URL}" \
    success \
    "${WEB_HEALTH_ATTEMPTS}" \
    "${HEALTH_INTERVAL_SECONDS}" \
    "${HEALTH_STABILIZE_SECONDS}"
fi

echo "[deploy] cleanup old releases, keep=${KEEP_RELEASES}"
# Always retain the active release, then keep the newest remaining releases by
# modification time. Release names may be timestamps or Git SHAs, so lexical
# sorting can delete the release that was just activated.
find "${DEPLOY_ROOT}/releases" -mindepth 1 -maxdepth 1 -type d \
  ! -path "${release_dir}" -printf '%T@ %p\n' \
  | sort -nr \
  | tail -n +"${KEEP_RELEASES}" \
  | cut -d' ' -f2- \
  | xargs -r rm -rf

rm -f "${RELEASE_ARCHIVE}"
echo "[deploy] done"
