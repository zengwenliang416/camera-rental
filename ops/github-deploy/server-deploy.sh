#!/usr/bin/env bash
set -euo pipefail

RELEASE_ARCHIVE="${1:?usage: server-deploy.sh /tmp/camera-rental-release.tgz}"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/camera-rental}"
RELEASE_SHA="${RELEASE_SHA:-manual-$(date +%Y%m%d%H%M%S)}"
KEEP_RELEASES="${KEEP_RELEASES:-5}"
SERVER_SERVICE="${SERVER_SERVICE:-camera-rental-server.service}"
WEB_SERVICE="${WEB_SERVICE:-camera-rental-web.service}"
NGINX_RELOAD_CMD="${NGINX_RELOAD_CMD:-nginx -t && systemctl reload nginx}"

release_dir="${DEPLOY_ROOT}/releases/${RELEASE_SHA}"

echo "[deploy] root=${DEPLOY_ROOT}"
echo "[deploy] release=${RELEASE_SHA}"

mkdir -p "${DEPLOY_ROOT}/releases" "${DEPLOY_ROOT}/shared" "${DEPLOY_ROOT}/shared/logs"
rm -rf "${release_dir}"
mkdir -p "${release_dir}"
tar -xzf "${RELEASE_ARCHIVE}" -C "${release_dir}"

test -f "${release_dir}/server/yudao-server.jar"
test -f "${release_dir}/admin/index.html"
test -f "${release_dir}/schedule-center/index.html"

# Archives built on macOS may preserve owner-only modes. Nginx must be able to
# traverse release directories and read static frontend assets.
chmod 755 "${release_dir}" "${release_dir}/server"
chmod 644 "${release_dir}/server/yudao-server.jar"
for static_dir in admin schedule-center; do
  find "${release_dir}/${static_dir}" -type d -exec chmod 755 {} +
  find "${release_dir}/${static_dir}" -type f -exec chmod 644 {} +
done

has_web_artifact=false
if [ -f "${release_dir}/web/server/index.mjs" ]; then
  has_web_artifact=true
fi

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
  echo "[deploy][warn] ${SERVER_SERVICE} not found; backend artifact deployed but not restarted"
fi

if [ "${has_web_artifact}" = true ] && systemctl list-unit-files "${WEB_SERVICE}" >/dev/null 2>&1; then
  echo "[deploy] restart ${WEB_SERVICE}"
  systemctl restart "${WEB_SERVICE}"
elif [ "${has_web_artifact}" = false ]; then
  echo "[deploy] skip ${WEB_SERVICE}; web artifact not included"
else
  echo "[deploy][warn] ${WEB_SERVICE} not found; PC web artifact deployed but not restarted"
fi

if command -v nginx >/dev/null 2>&1; then
  echo "[deploy] reload nginx"
  bash -lc "${NGINX_RELOAD_CMD}"
else
  echo "[deploy][warn] nginx not installed or not on PATH; static frontends deployed but nginx not reloaded"
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
