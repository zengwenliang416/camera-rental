#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../incremental-build-lib.sh
source "${script_dir}/../incremental-build-lib.sh"

test_dir="$(mktemp -d /tmp/camera-rental-incremental-test.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

assert_success() {
  local message="$1"
  shift
  if ! "$@"; then
    echo "FAIL: ${message}" >&2
    exit 1
  fi
}

assert_failure() {
  local message="$1"
  shift
  if "$@"; then
    echo "FAIL: ${message}" >&2
    exit 1
  fi
}

changed_files="${test_dir}/changed-files.txt"
cat > "${changed_files}" <<'EOF'
camera-rental-admin/src/App.vue
docs/deployment.md
EOF

assert_success "admin changes should trigger the admin build" \
  component_changed admin "${changed_files}"
assert_failure "server should remain reusable for admin-only changes" \
  component_changed server "${changed_files}"
assert_failure "source-only changes should not invalidate dependencies" \
  dependency_inputs_changed admin "${changed_files}"

cat >> "${changed_files}" <<'EOF'
camera-rental-admin/pnpm-lock.yaml
EOF
assert_success "lockfile changes should invalidate dependencies" \
  dependency_inputs_changed admin "${changed_files}"

release_dir="${test_dir}/release"
mkdir -p "${release_dir}/server"
touch "${release_dir}/server/yudao-server.jar"
assert_success "server artifact should be detected" \
  component_artifact_available "${release_dir}" server
assert_failure "missing web artifact should force a web build" \
  component_artifact_available "${release_dir}" web

project_dir="${test_dir}/camera-rental-admin"
mkdir -p "${project_dir}/node_modules"
printf '{"name":"admin"}\n' > "${project_dir}/package.json"
printf 'lockfileVersion: 9\n' > "${project_dir}/pnpm-lock.yaml"

assert_failure "dependencies without a fingerprint stamp are not reusable" \
  dependencies_are_current "${project_dir}" admin
write_dependency_stamp "${project_dir}" admin
assert_success "matching dependency fingerprints should be reusable" \
  dependencies_are_current "${project_dir}" admin
printf '{"name":"admin","version":"2"}\n' > "${project_dir}/package.json"
assert_failure "manifest changes should invalidate dependency reuse" \
  dependencies_are_current "${project_dir}" admin

echo "incremental build helper tests passed"
