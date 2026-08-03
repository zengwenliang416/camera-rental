#!/usr/bin/env bash

is_http_status_acceptable() {
  local mode="$1"
  local status="$2"

  case "${mode}" in
    reachable)
      [[ "${status}" =~ ^[1-4][0-9][0-9]$ ]]
      ;;
    success)
      [[ "${status}" =~ ^[23][0-9][0-9]$ ]]
      ;;
    *)
      echo "[deploy][error] unknown health check mode: ${mode}" >&2
      return 2
      ;;
  esac
}

admin_entry_script() {
  local index_file="$1"

  sed -nE 's#.*src="(/admin/assets/[0-9a-f]{12}/[^"]+\.js)".*#\1#p' "${index_file}" \
    | head -n 1
}

validate_admin_index_markup() {
  local index_file="$1"
  local release_sha="${2:-}"
  local entry_script

  [ -f "${index_file}" ] || return 1
  ! grep -q '%VITE_' "${index_file}" || return 1
  grep -q 'src="/admin/logo.gif"' "${index_file}" || return 1
  entry_script="$(admin_entry_script "${index_file}")"
  [ -n "${entry_script}" ] || return 1
  if [ -n "${release_sha}" ]; then
    [[ "${entry_script}" == "/admin/assets/${release_sha:0:12}/"* ]] || return 1
  fi
}

validate_admin_artifact() {
  local index_file="$1"
  local release_sha="${2:-}"
  local artifact_dir
  local entry_script
  local entry_file

  validate_admin_index_markup "${index_file}" "${release_sha}" || return 1
  artifact_dir="$(cd "$(dirname "${index_file}")" && pwd)"
  entry_script="$(admin_entry_script "${index_file}")"
  entry_file="${artifact_dir}/${entry_script#/admin/}"
  [ -f "${entry_file}" ] && [ -f "${artifact_dir}/logo.gif" ]
}

verify_return_registration_artifact() {
  local application_jar="$1"
  local rental_paths
  local rental_count
  local rental_path
  local ai_paths
  local ai_count
  local ai_path
  local verification_dir
  local rental_jar
  local ai_jar

  application_jar="$(
    cd "$(dirname "${application_jar}")"
    printf '%s/%s\n' "$(pwd)" "$(basename "${application_jar}")"
  )"
  rental_paths="$(
    jar tf "${application_jar}" \
      | grep -E '^BOOT-INF/lib/yudao-module-rental-biz-.*\.jar$' || true
  )"
  rental_count="$(
    printf '%s\n' "${rental_paths}" | sed '/^$/d' | wc -l | tr -d ' '
  )"
  if [ "${rental_count}" -ne 1 ]; then
    echo "[deploy][error] expected one rental module JAR, found ${rental_count}" >&2
    return 1
  fi
  ai_paths="$(
    jar tf "${application_jar}" \
      | grep -E '^BOOT-INF/lib/yudao-module-ai-.*\.jar$' || true
  )"
  ai_count="$(
    printf '%s\n' "${ai_paths}" | sed '/^$/d' | wc -l | tr -d ' '
  )"
  if [ "${ai_count}" -ne 1 ]; then
    echo "[deploy][error] expected one AI module JAR, found ${ai_count}" >&2
    return 1
  fi

  rental_path="${rental_paths}"
  ai_path="${ai_paths}"
  if ! zipinfo -l "${application_jar}" "${rental_path}" \
    | grep -Eq '[[:space:]]stor[[:space:]]'; then
    echo "[deploy][error] rental module JAR must be stored without compression" >&2
    return 1
  fi
  if ! zipinfo -l "${application_jar}" "${ai_path}" \
    | grep -Eq '[[:space:]]stor[[:space:]]'; then
    echo "[deploy][error] AI module JAR must be stored without compression" >&2
    return 1
  fi
  verification_dir="$(mktemp -d /tmp/camera-rental-jar-check.XXXXXX)"
  (
    cd "${verification_dir}"
    jar xf "${application_jar}" "${rental_path}" "${ai_path}"
  )
  rental_jar="${verification_dir}/${rental_path}"
  ai_jar="${verification_dir}/${ai_path}"

  if ! jar tf "${rental_jar}" \
    | grep -F 'AppReturnRegistrationController.class' >/dev/null; then
    rm -rf "${verification_dir}"
    echo "[deploy][error] customer return controller is missing from backend artifact" >&2
    return 1
  fi
  if ! jar tf "${rental_jar}" \
    | grep -F 'SellerRemarkAiFallbackService.class' >/dev/null; then
    rm -rf "${verification_dir}"
    echo "[deploy][error] seller remark AI fallback is missing from backend artifact" >&2
    return 1
  fi
  if ! jar tf "${ai_jar}" \
    | grep -F 'AiApiKeyController.class' >/dev/null; then
    rm -rf "${verification_dir}"
    echo "[deploy][error] AI module controllers are missing from backend artifact" >&2
    return 1
  fi

  rm -rf "${verification_dir}"
  echo "[deploy] customer return and AI backend artifacts verified"
}

verify_rental_routes() {
  local base_url="${1:-http://127.0.0.1:48080}"
  local public_status
  local admin_status

  public_status="$(
    curl -sS -o /dev/null -w '%{http_code}' --max-time 5 \
      -H 'Content-Type: application/json' \
      -X POST "${base_url}/app-api/rental/return-registration/simple-submit" \
      --data '{"orderNo":"","mobileLast4":"","machineCode":"","waybillNo":""}' || true
  )"
  admin_status="$(
    curl -sS -o /dev/null -w '%{http_code}' --max-time 5 \
      "${base_url}/admin-api/rental/xianyu/order/page?pageNo=1&pageSize=1" || true
  )"
  if [ "${public_status}" = "404" ] || [ "${admin_status}" = "404" ]; then
    echo "[deploy][error] rental routes are not registered" >&2
    return 1
  fi
  echo "[deploy] rental routes registered (public=${public_status}, admin=${admin_status})"
}

verify_static_frontend_route() {
  local url="$1"
  local resolve="${2:-}"
  local curl_args=(
    -k
    -sS
    -o /dev/null
    -w '%{http_code}'
    --max-time 10
  )
  local status

  if [ -n "${resolve}" ]; then
    curl_args+=(--resolve "${resolve}")
  fi
  status="$(curl "${curl_args[@]}" "${url}" || true)"
  if ! is_http_status_acceptable success "${status}"; then
    echo "[deploy][error] static frontend failed health check: ${url} (${status:-no response})" >&2
    return 1
  fi
  echo "[deploy] static frontend healthy (${status}): ${url}"
}

verify_admin_frontend_route() {
  local url="$1"
  local resolve="${2:-}"
  local html_file
  local entry_script
  local origin
  local html_status
  local asset_status
  local curl_args=(
    -k
    -sS
    --max-time 10
  )

  if [ -n "${resolve}" ]; then
    curl_args+=(--resolve "${resolve}")
  fi
  html_file="$(mktemp /tmp/camera-rental-admin-index.XXXXXX)"
  html_status="$(
    curl "${curl_args[@]}" -o "${html_file}" -w '%{http_code}' "${url}" || true
  )"
  if ! is_http_status_acceptable success "${html_status}" \
    || ! validate_admin_index_markup "${html_file}"; then
    rm -f "${html_file}"
    echo "[deploy][error] admin frontend HTML failed validation: ${url} (${html_status:-no response})" >&2
    return 1
  fi

  entry_script="$(admin_entry_script "${html_file}")"
  origin="$(printf '%s' "${url}" | sed -E 's#^(https?://[^/]+).*$#\1#')"
  asset_status="$(
    curl "${curl_args[@]}" -o /dev/null -w '%{http_code}' "${origin}${entry_script}" || true
  )"
  rm -f "${html_file}"
  if ! is_http_status_acceptable success "${asset_status}"; then
    echo "[deploy][error] admin entry asset failed health check: ${entry_script} (${asset_status:-no response})" >&2
    return 1
  fi
  echo "[deploy] admin frontend and entry asset healthy (${html_status}/${asset_status})"
}

dump_service_diagnostics() {
  local service="$1"

  systemctl --no-pager --full status "${service}" >&2 || true
  journalctl -u "${service}" -n 60 --no-pager >&2 || true
}

wait_for_service_http() {
  local service="$1"
  local url="$2"
  local mode="$3"
  local attempts="${4:-60}"
  local interval_seconds="${5:-2}"
  local stabilize_seconds="${6:-5}"
  local attempt
  local status

  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if systemctl is-active --quiet "${service}"; then
      status="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "${url}" || true)"
      if is_http_status_acceptable "${mode}" "${status}"; then
        if [ "${stabilize_seconds}" != "0" ]; then
          sleep "${stabilize_seconds}"
        fi
        if systemctl is-active --quiet "${service}"; then
          status="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "${url}" || true)"
          if is_http_status_acceptable "${mode}" "${status}"; then
            echo "[deploy] ${service} healthy (${status})"
            return 0
          fi
        fi
      fi
    fi
    if [ "${attempt}" -lt "${attempts}" ]; then
      sleep "${interval_seconds}"
    fi
  done

  echo "[deploy][error] ${service} failed health check: ${url}" >&2
  dump_service_diagnostics "${service}"
  return 1
}
