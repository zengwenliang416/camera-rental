#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/camera-rental}"
WEB_PORT="${WEB_PORT:-3102}"
BACKEND_ENV="${BACKEND_ENV:-${DEPLOY_ROOT}/shared/backend.env}"
WEB_ENV="${WEB_ENV:-${DEPLOY_ROOT}/shared/web.env}"
NGINX_CONFIG="${NGINX_CONFIG:-}"
NGINX_SEARCH_ROOTS="${NGINX_SEARCH_ROOTS:-/etc/nginx/conf.d /etc/nginx/sites-enabled}"
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

generate_encryptor_password() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
    return
  fi
  od -An -N16 -tx1 /dev/urandom | tr -d ' \n'
}

ensure_encryptor_password() {
  local file="$1"
  local current=""
  local length=0

  if grep -q '^MYBATIS_PLUS_ENCRYPTOR_PASSWORD=' "${file}" 2>/dev/null; then
    current="$(sed -n 's/^MYBATIS_PLUS_ENCRYPTOR_PASSWORD=//p' "${file}" | tail -n 1)"
    length="$(printf '%s' "${current}" | wc -c | tr -d ' ')"
    case "${length}" in
      16|24|32) return ;;
    esac
    echo "[production-80][error] existing MYBATIS_PLUS_ENCRYPTOR_PASSWORD must be 16, 24, or 32 bytes; refusing automatic key rotation" >&2
    exit 1
  fi
  set_env_value "${file}" MYBATIS_PLUS_ENCRYPTOR_PASSWORD "$(generate_encryptor_password)"
}

discover_nginx_config() {
  local search_root
  local candidate
  local -a candidates=()

  for search_root in ${NGINX_SEARCH_ROOTS}; do
    [ -d "${search_root}" ] || continue
    while IFS= read -r candidate; do
      [ -n "${candidate}" ] || continue
      if grep -qE 'server_name[[:space:]]+[^;]*rental\.motion-cover\.com' \
        "${candidate}"; then
        candidates+=("$(readlink -f "${candidate}")")
      fi
    done < <(
      find -L "${search_root}" -type f -print 2>/dev/null || true
    )
  done

  if [ "${#candidates[@]}" -eq 0 ]; then
    echo "[production-80][error] unable to locate rental.motion-cover.com nginx config" >&2
    return 1
  fi

  printf '%s\n' "${candidates[@]}" | awk '!seen[$0]++'
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
ensure_encryptor_password "${BACKEND_ENV}"

# Production API documentation is disabled. Knife4j must be disabled with
# SpringDoc, otherwise its auto-configuration requires a bean SpringDoc omits.
set_env_value "${BACKEND_ENV}" SPRINGDOC_API_DOCS_ENABLED false
set_env_value "${BACKEND_ENV}" SPRINGDOC_SWAGGER_UI_ENABLED false
set_env_value "${BACKEND_ENV}" KNIFE4J_ENABLE false
chmod 600 "${BACKEND_ENV}" "${WEB_ENV}"

if [ -z "${NGINX_CONFIG}" ]; then
  nginx_configs="$(discover_nginx_config)"
  nginx_config_count="$(printf '%s\n' "${nginx_configs}" | sed '/^$/d' | wc -l | tr -d ' ')"
  if [ "${nginx_config_count}" -ne 1 ]; then
    printf '[production-80][error] expected one nginx config, found %s\n' \
      "${nginx_config_count}" >&2
    printf '%s\n' "${nginx_configs}" | sed 's/^/  /' >&2
    exit 1
  fi
  NGINX_CONFIG="${nginx_configs}"
fi

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
echo "[production-80] nginx config=${NGINX_CONFIG}"
