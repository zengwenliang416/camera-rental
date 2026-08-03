#!/usr/bin/env bash

component_source_prefix() {
  case "$1" in
    server) printf '%s\n' "camera-rental-server" ;;
    admin) printf '%s\n' "camera-rental-admin" ;;
    schedule-center) printf '%s\n' "camera-rental-schedule-center" ;;
    staff) printf '%s\n' "camera-rental-staff" ;;
    web) printf '%s\n' "camera-rental-web" ;;
    *) return 1 ;;
  esac
}

component_required_artifact() {
  case "$1" in
    server) printf '%s\n' "server/yudao-server.jar" ;;
    admin) printf '%s\n' "admin/index.html" ;;
    schedule-center) printf '%s\n' "schedule-center/index.html" ;;
    staff) printf '%s\n' "staff/index.html" ;;
    web) printf '%s\n' "web/server/index.mjs" ;;
    *) return 1 ;;
  esac
}

component_changed() {
  local component="$1"
  local changed_files="$2"
  local prefix
  local path

  prefix="$(component_source_prefix "${component}")"
  while IFS= read -r path; do
    case "${path}" in
      .github/workflows/deploy-211.yml|\
      .workflow/deploy-production.yml|\
      ops/github-deploy/server-build-deploy.sh|\
      ops/github-deploy/incremental-build-lib.sh)
        return 0
        ;;
    esac
    if [ "${path}" = "${prefix}" ] || [[ "${path}" == "${prefix}/"* ]]; then
      return 0
    fi
  done < "${changed_files}"
  return 1
}

component_artifact_available() {
  local release_dir="$1"
  local component="$2"
  local artifact

  artifact="$(component_required_artifact "${component}")"
  if [ "${component}" = "admin" ]; then
    validate_admin_artifact "${release_dir}/${artifact}"
  else
    [ -f "${release_dir}/${artifact}" ]
  fi
}

dependency_input_paths() {
  case "$1" in
    admin|staff)
      printf '%s\n' package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc patches
      ;;
    schedule-center)
      printf '%s\n' package.json pnpm-lock.yaml .npmrc
      ;;
    web)
      printf '%s\n' package.json bun.lock .npmrc
      ;;
    *)
      return 1
      ;;
  esac
}

dependency_inputs_changed() {
  local component="$1"
  local changed_files="$2"
  local prefix
  local relative_path
  local path

  prefix="$(component_source_prefix "${component}")"
  while IFS= read -r path; do
    while IFS= read -r relative_path; do
      if [ "${path}" = "${prefix}/${relative_path}" ] \
        || [[ "${path}" == "${prefix}/${relative_path}/"* ]]; then
        return 0
      fi
    done < <(dependency_input_paths "${component}")
  done < "${changed_files}"
  return 1
}

dependency_fingerprint() {
  local project_dir="$1"
  local component="$2"
  local relative_path
  local full_path

  while IFS= read -r relative_path; do
    full_path="${project_dir}/${relative_path}"
    if [ -f "${full_path}" ]; then
      printf 'file %s ' "${relative_path}"
      sha256sum "${full_path}"
    elif [ -d "${full_path}" ]; then
      find "${full_path}" -type f -print0 \
        | sort -z \
        | while IFS= read -r -d '' dependency_file; do
            printf 'file %s ' "${dependency_file#${project_dir}/}"
            sha256sum "${dependency_file}"
          done
    else
      printf 'missing %s\n' "${relative_path}"
    fi
  done < <(dependency_input_paths "${component}") \
    | sha256sum \
    | awk '{print $1}'
}

dependencies_are_current() {
  local project_dir="$1"
  local component="$2"
  local stamp_file="${project_dir}/node_modules/.camera-rental-deps.sha256"
  local expected

  [ -d "${project_dir}/node_modules" ] || return 1
  [ -f "${stamp_file}" ] || return 1
  expected="$(dependency_fingerprint "${project_dir}" "${component}")"
  [ "$(cat "${stamp_file}")" = "${expected}" ]
}

write_dependency_stamp() {
  local project_dir="$1"
  local component="$2"
  local stamp_file="${project_dir}/node_modules/.camera-rental-deps.sha256"

  mkdir -p "${project_dir}/node_modules"
  dependency_fingerprint "${project_dir}" "${component}" > "${stamp_file}"
}
