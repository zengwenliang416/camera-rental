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

global_changed_files="${test_dir}/global-changed-files.txt"
cat > "${global_changed_files}" <<'EOF'
ops/github-deploy/server-build-deploy.sh
EOF
assert_success "build script changes should invalidate admin artifacts" \
  component_changed admin "${global_changed_files}"
assert_success "build script changes should invalidate schedule center artifacts" \
  component_changed schedule-center "${global_changed_files}"
assert_success "build script changes should invalidate server artifacts" \
  component_changed server "${global_changed_files}"

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

artifact_dir="${test_dir}/admin-artifact"
mkdir -p "${artifact_dir}"
cat > "${artifact_dir}/index.html" <<'EOF'
<title>相机租赁管理后台</title>
<script type="module" src="/admin/assets/index-good.js"></script>
<img src="/admin/logo.gif" alt="Logo" />
EOF
assert_success "scoped admin artifact should pass validation" \
  validate_admin_artifact "${artifact_dir}/index.html"

cat > "${artifact_dir}/index.html" <<'EOF'
<title>%VITE_APP_TITLE%</title>
<script type="module" src="/assets/index-bad.js"></script>
<img src="/logo.gif" alt="Logo" />
EOF
assert_failure "root-scoped admin artifact should fail validation" \
  validate_admin_artifact "${artifact_dir}/index.html"

echo "incremental build helper tests passed"
