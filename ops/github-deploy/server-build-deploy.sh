#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/camera-rental}"
RELEASE_SHA="${RELEASE_SHA:?RELEASE_SHA is required}"
REPO_URL="${REPO_URL:-git@gitee.com:wenliang_zeng/camera-rental.git}"
SOURCE_DIR="${SOURCE_DIR:-${DEPLOY_ROOT}/source}"
GIT_SSH_COMMAND="${GIT_SSH_COMMAND:-ssh -i ~/.ssh/camera_rental_gitee_pull -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=20 -C}"
export GIT_SSH_COMMAND
NODE_VERSION="${NODE_VERSION:-22.14.0}"
NODE_ARCHIVE_ARCH=""
MAVEN_THREADS="${MAVEN_THREADS:-2}"
FORCE_FULL_BUILD="${FORCE_FULL_BUILD:-false}"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=incremental-build-lib.sh
source "${script_dir}/incremental-build-lib.sh"

build_dir="$(mktemp -d /tmp/camera-rental-build.XXXXXX)"
release_dir="${build_dir}/release"
release_archive="/tmp/camera-rental-${RELEASE_SHA}.tgz"
changed_files="${build_dir}/changed-files.txt"
previous_release_dir=""
previous_release_sha=""
incremental_build=false
built_components=()
reused_components=()

cleanup() {
  rm -rf "${build_dir}"
}
trap cleanup EXIT

ensure_node_runtime() {
  case "$(uname -m)" in
    x86_64|amd64) NODE_ARCHIVE_ARCH="x64" ;;
    aarch64|arm64) NODE_ARCHIVE_ARCH="arm64" ;;
    *) echo "[build-deploy] unsupported node arch: $(uname -m)" >&2; exit 1 ;;
  esac

  local toolchain_dir="${DEPLOY_ROOT}/toolchain"
  local node_dir="${toolchain_dir}/node-v${NODE_VERSION}-linux-${NODE_ARCHIVE_ARCH}"
  local node_tgz="${toolchain_dir}/node-v${NODE_VERSION}-linux-${NODE_ARCHIVE_ARCH}.tar.xz"

  mkdir -p "${toolchain_dir}"
  if [ ! -x "${node_dir}/bin/node" ]; then
    echo "[build-deploy] install node v${NODE_VERSION}"
    rm -rf "${node_dir}" "${node_tgz}"
    curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${NODE_ARCHIVE_ARCH}.tar.xz" -o "${node_tgz}"
    tar -xJf "${node_tgz}" -C "${toolchain_dir}"
    rm -f "${node_tgz}"
  fi

  export PATH="${node_dir}/bin:${PATH}"
  node --version

  if ! command -v pnpm >/dev/null 2>&1 || ! pnpm --version | grep -q '^10\.'; then
    npm install -g pnpm@10
  fi
  pnpm --version
}

prepare_incremental_build() {
  previous_release_dir="$(readlink -f "${DEPLOY_ROOT}/current" 2>/dev/null || true)"
  if [ -n "${previous_release_dir}" ]; then
    previous_release_sha="$(basename "${previous_release_dir}")"
  fi

  if [ "${FORCE_FULL_BUILD}" = "true" ]; then
    echo "[build-deploy] full build forced by FORCE_FULL_BUILD"
    return
  fi
  if [[ ! "${previous_release_sha}" =~ ^[0-9a-f]{40}$ ]] || [ ! -d "${previous_release_dir}" ]; then
    echo "[build-deploy] no Git release baseline; use full build"
    return
  fi
  if ! git -C "${SOURCE_DIR}" fetch --quiet --no-tags --depth 1 origin "${previous_release_sha}"; then
    echo "[build-deploy][warn] unable to fetch previous release ${previous_release_sha}; use full build"
    return
  fi
  if ! git -C "${SOURCE_DIR}" diff --name-only "${previous_release_sha}" "${RELEASE_SHA}" > "${changed_files}"; then
    echo "[build-deploy][warn] unable to compare releases; use full build"
    return
  fi

  incremental_build=true
  echo "[build-deploy] incremental baseline=${previous_release_sha}"
  if [ -s "${changed_files}" ]; then
    sed 's/^/[build-deploy] changed: /' "${changed_files}"
  else
    echo "[build-deploy] no source changes since the active release"
  fi
}

install_frontend_dependencies() {
  local component="$1"
  local project_dir="$2"
  local package_manager="$3"

  if dependencies_are_current "${project_dir}" "${component}"; then
    echo "[build-deploy] reuse ${component} dependencies"
    return
  fi
  if [ "${incremental_build}" = true ] \
    && [ -d "${project_dir}/node_modules" ] \
    && ! dependency_inputs_changed "${component}" "${changed_files}"; then
    echo "[build-deploy] adopt existing ${component} dependencies; manifests unchanged"
    write_dependency_stamp "${project_dir}" "${component}"
    return
  fi

  echo "[build-deploy] install ${component} dependencies"
  (
    cd "${project_dir}"
    case "${package_manager}" in
      pnpm) pnpm install --frozen-lockfile --prefer-offline ;;
      bun) bun install --frozen-lockfile ;;
      *) echo "[build-deploy] unsupported package manager: ${package_manager}" >&2; exit 1 ;;
    esac
  )
  write_dependency_stamp "${project_dir}" "${component}"
}

reuse_component() {
  local component="$1"
  local component_dir="$2"

  echo "[build-deploy] reuse ${component} artifact from ${previous_release_sha}"
  rm -rf "${release_dir:?}/${component_dir}"
  mkdir -p "${release_dir}/${component_dir}"
  cp -a "${previous_release_dir}/${component_dir}/." "${release_dir}/${component_dir}/"
  reused_components+=("${component}")
}

should_reuse_component() {
  local component="$1"

  [ "${incremental_build}" = true ] \
    && ! component_changed "${component}" "${changed_files}" \
    && component_artifact_available "${previous_release_dir}" "${component}"
}

json_array() {
  local first=true
  local item

  printf '['
  for item in "$@"; do
    if [ "${first}" = true ]; then
      first=false
    else
      printf ','
    fi
    printf '"%s"' "${item}"
  done
  printf ']'
}

write_release_info() {
  local build_mode="full"
  local built_json
  local reused_json
  local release_info
  local static_dir

  if [ "${incremental_build}" = true ]; then
    build_mode="incremental"
  fi
  built_json="$(json_array "${built_components[@]}")"
  reused_json="$(json_array "${reused_components[@]}")"
  release_info="${release_dir}/release-info.json"

  cat > "${release_info}" <<EOF
{
  "releaseSha": "${RELEASE_SHA}",
  "previousRelease": "${previous_release_sha}",
  "builtAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "buildMode": "${build_mode}",
  "builtComponents": ${built_json},
  "reusedComponents": ${reused_json}
}
EOF

  for static_dir in admin schedule-center staff; do
    cp "${release_info}" "${release_dir}/${static_dir}/release-info.json"
  done
  if [ -d "${release_dir}/web/public" ]; then
    cp "${release_info}" "${release_dir}/web/public/release-info.json"
  fi
  cat "${release_info}"
}

echo "[build-deploy] source=${SOURCE_DIR}"
mkdir -p "${DEPLOY_ROOT}" "$(dirname "${SOURCE_DIR}")"
ensure_node_runtime

if [ -d "${SOURCE_DIR}/.git" ]; then
  git -C "${SOURCE_DIR}" remote set-url origin "${REPO_URL}"
  git -C "${SOURCE_DIR}" fetch --progress --no-tags --depth 1 origin "${RELEASE_SHA}"
else
  rm -rf "${SOURCE_DIR}"
  git clone --progress --no-tags --depth 1 "${REPO_URL}" "${SOURCE_DIR}"
  git -C "${SOURCE_DIR}" fetch --progress --no-tags --depth 1 origin "${RELEASE_SHA}"
fi

git -C "${SOURCE_DIR}" checkout --force "${RELEASE_SHA}"
git -C "${SOURCE_DIR}" reset --hard "${RELEASE_SHA}"
git -C "${SOURCE_DIR}" clean -fd \
  -e camera-rental-admin/node_modules/ \
  -e camera-rental-schedule-center/node_modules/ \
  -e camera-rental-staff/node_modules/ \
  -e camera-rental-web/node_modules/

mkdir -p "${release_dir}/server" "${release_dir}/admin" "${release_dir}/schedule-center" "${release_dir}/staff" "${release_dir}/web"
prepare_incremental_build

if should_reuse_component server; then
  reuse_component server server
else
  echo "[build-deploy] build backend"
  (
    cd "${SOURCE_DIR}/camera-rental-server"
    mvn -T "${MAVEN_THREADS}" -pl yudao-server -am -DskipTests package
  )
  cp "${SOURCE_DIR}/camera-rental-server/yudao-server/target/yudao-server.jar" "${release_dir}/server/yudao-server.jar"
  built_components+=("server")
fi

if should_reuse_component admin; then
  reuse_component admin admin
else
  echo "[build-deploy] build admin"
  install_frontend_dependencies admin "${SOURCE_DIR}/camera-rental-admin" pnpm
  rm -rf "${SOURCE_DIR}/camera-rental-admin/dist"
  (
    cd "${SOURCE_DIR}/camera-rental-admin"
    pnpm build:prod
  )
  cp -R "${SOURCE_DIR}/camera-rental-admin/dist/." "${release_dir}/admin/"
  built_components+=("admin")
fi

if should_reuse_component schedule-center; then
  reuse_component schedule-center schedule-center
else
  echo "[build-deploy] build schedule center"
  install_frontend_dependencies schedule-center "${SOURCE_DIR}/camera-rental-schedule-center" bun
  rm -rf "${SOURCE_DIR}/camera-rental-schedule-center/dist"
  (
    cd "${SOURCE_DIR}/camera-rental-schedule-center"
    VITE_BASE_PATH=/admin/schedule-center/ VITE_BASE_URL=https://rental.motion-cover.com VITE_API_URL=/admin-api bun run build
  )
  cp -R "${SOURCE_DIR}/camera-rental-schedule-center/dist/." "${release_dir}/schedule-center/"
  built_components+=("schedule-center")
fi

if should_reuse_component staff; then
  reuse_component staff staff
else
  echo "[build-deploy] build staff h5"
  install_frontend_dependencies staff "${SOURCE_DIR}/camera-rental-staff" pnpm
  rm -rf "${SOURCE_DIR}/camera-rental-staff/dist/build/h5"
  (
    cd "${SOURCE_DIR}/camera-rental-staff"
    pnpm build:h5:prod
  )
  cp -R "${SOURCE_DIR}/camera-rental-staff/dist/build/h5/." "${release_dir}/staff/"
  built_components+=("staff")
fi

if should_reuse_component web; then
  reuse_component web web
else
  echo "[build-deploy] build pc web"
  install_frontend_dependencies web "${SOURCE_DIR}/camera-rental-web" bun
  rm -rf "${SOURCE_DIR}/camera-rental-web/.output"
  (
    cd "${SOURCE_DIR}/camera-rental-web"
    bun run build
  )
  cp -R "${SOURCE_DIR}/camera-rental-web/.output/." "${release_dir}/web/"
  built_components+=("web")
fi

write_release_info

echo "[build-deploy] pack release"
rm -f "${release_archive}"
tar -czf "${release_archive}" -C "${release_dir}" .

install -m 0755 "${SOURCE_DIR}/ops/github-deploy/server-deploy.sh" /tmp/camera-rental-server-deploy.sh
exec bash /tmp/camera-rental-server-deploy.sh "${release_archive}"
