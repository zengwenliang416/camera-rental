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
