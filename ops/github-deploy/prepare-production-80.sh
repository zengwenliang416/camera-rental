#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/camera-rental}"
WEB_PORT="${WEB_PORT:-3102}"
BACKEND_ENV="${BACKEND_ENV:-${DEPLOY_ROOT}/shared/backend.env}"
WEB_ENV="${WEB_ENV:-${DEPLOY_ROOT}/shared/web.env}"
NGINX_CONFIG="${NGINX_CONFIG:-/etc/nginx/conf.d/camera-rental.conf}"
NGINX_TEST_CMD="${NGINX_TEST_CMD:-nginx -t}"

set_env_value() {
  local file="$1"
  local key="$2"
  local value="$3"
  local temp_file

  touch "${file}"
  temp_file="$(mktemp "${file}.XXXXXX")"
  awk -v key="${key}" -v value="${value}" '
    BEGIN { found = 0 }
    index($0, key "=") == 1 {
      print key "=" value
      found = 1
      next
    }
    { print }
    END {
      if (!found) {
        print key "=" value
      }
    }
  ' "${file}" > "${temp_file}"
  cat "${temp_file}" > "${file}"
  rm -f "${temp_file}"
}

set_env_default() {
  local file="$1"
  local key="$2"
  local value="$3"
  local current

  if grep -q "^${key}=" "${file}" 2>/dev/null; then
    current="$(sed -n "s/^${key}=//p" "${file}" | tail -n 1)"
    if [ -n "${current}" ]; then
      return
    fi
  fi
  set_env_value "${file}" "${key}" "${value}"
}

mkdir -p "${DEPLOY_ROOT}/shared"
set_env_value "${WEB_ENV}" HOST 127.0.0.1
set_env_value "${WEB_ENV}" PORT "${WEB_PORT}"
set_env_value "${WEB_ENV}" NODE_ENV production

# WxJava starters require non-empty values even when WeChat integrations are
# intentionally disabled. Preserve any real credentials configured later.
set_env_default "${BACKEND_ENV}" WX_MP_APP_ID disabled
set_env_default "${BACKEND_ENV}" WX_MP_SECRET disabled
set_env_default "${BACKEND_ENV}" WX_MINIAPP_APP_ID disabled
set_env_default "${BACKEND_ENV}" WX_MINIAPP_SECRET disabled
chmod 600 "${BACKEND_ENV}" "${WEB_ENV}"

if [ ! -f "${NGINX_CONFIG}" ]; then
  echo "[production-80][error] nginx config not found: ${NGINX_CONFIG}" >&2
  exit 1
fi

nginx_backup="${NGINX_CONFIG}.camera-rental-backup.$$"
nginx_updated="${NGINX_CONFIG}.camera-rental-updated.$$"
cp -a "${NGINX_CONFIG}" "${nginx_backup}"
sed "s|127\\.0\\.0\\.1:3001|127.0.0.1:${WEB_PORT}|g" \
  "${NGINX_CONFIG}" > "${nginx_updated}"
cat "${nginx_updated}" > "${NGINX_CONFIG}"
rm -f "${nginx_updated}"

if ! grep -q "127\\.0\\.0\\.1:${WEB_PORT}" "${NGINX_CONFIG}"; then
  mv "${nginx_backup}" "${NGINX_CONFIG}"
  echo "[production-80][error] nginx config has no PC Web upstream to update" >&2
  exit 1
fi

if ! bash -c "${NGINX_TEST_CMD}"; then
  mv "${nginx_backup}" "${NGINX_CONFIG}"
  echo "[production-80][error] nginx validation failed; previous config restored" >&2
  exit 1
fi
rm -f "${nginx_backup}"

echo "[production-80] runtime configuration prepared"
echo "[production-80] PC Web port=${WEB_PORT}"
