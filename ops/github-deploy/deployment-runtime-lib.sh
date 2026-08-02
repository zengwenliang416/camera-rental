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

verify_return_registration_artifact() {
  local application_jar="$1"
  local nested_paths
  local nested_count
  local nested_path
  local verification_dir
  local nested_jar

  application_jar="$(
    cd "$(dirname "${application_jar}")"
    printf '%s/%s\n' "$(pwd)" "$(basename "${application_jar}")"
  )"
  nested_paths="$(
    jar tf "${application_jar}" \
      | grep -E '^BOOT-INF/lib/yudao-module-rental-biz-.*\.jar$' || true
  )"
  nested_count="$(
    printf '%s\n' "${nested_paths}" | sed '/^$/d' | wc -l | tr -d ' '
  )"
  if [ "${nested_count}" -ne 1 ]; then
    echo "[deploy][error] expected one rental module JAR, found ${nested_count}" >&2
    return 1
  fi

  nested_path="${nested_paths}"
  verification_dir="$(mktemp -d /tmp/camera-rental-jar-check.XXXXXX)"
  (
    cd "${verification_dir}"
    jar xf "${application_jar}" "${nested_path}"
  )
  nested_jar="${verification_dir}/${nested_path}"

  if ! jar tf "${nested_jar}" \
    | grep -F 'AppReturnRegistrationController.class' >/dev/null; then
    rm -rf "${verification_dir}"
    echo "[deploy][error] customer return controller is missing from backend artifact" >&2
    return 1
  fi

  rm -rf "${verification_dir}"
  echo "[deploy] customer return backend artifact verified"
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
