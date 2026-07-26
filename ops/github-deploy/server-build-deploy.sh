#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/camera-rental}"
RELEASE_SHA="${RELEASE_SHA:?RELEASE_SHA is required}"
REPO_URL="${REPO_URL:-git@github.com:zengwenliang416/camera-rental.git}"
SOURCE_DIR="${SOURCE_DIR:-${DEPLOY_ROOT}/source}"
GIT_SSH_COMMAND="${GIT_SSH_COMMAND:-ssh -i ~/.ssh/camera_rental_github_pull -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=20 -C}"
export GIT_SSH_COMMAND
NODE_VERSION="${NODE_VERSION:-22.14.0}"
NODE_ARCHIVE_ARCH=""

build_dir="$(mktemp -d /tmp/camera-rental-build.XXXXXX)"
release_dir="${build_dir}/release"
release_archive="/tmp/camera-rental-${RELEASE_SHA}.tgz"

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
  -e camera-rental-staff/node_modules/ \
  -e camera-rental-web/node_modules/

mkdir -p "${release_dir}/server" "${release_dir}/admin" "${release_dir}/staff" "${release_dir}/web"

echo "[build-deploy] build backend"
(
  cd "${SOURCE_DIR}/camera-rental-server"
  mvn -pl yudao-server -am -DskipTests package
)
cp "${SOURCE_DIR}/camera-rental-server/yudao-server/target/yudao-server.jar" "${release_dir}/server/yudao-server.jar"

echo "[build-deploy] build admin"
(
  cd "${SOURCE_DIR}/camera-rental-admin"
  pnpm install --frozen-lockfile
  pnpm build:prod
)
cp -R "${SOURCE_DIR}/camera-rental-admin/dist/." "${release_dir}/admin/"

echo "[build-deploy] build staff h5"
(
  cd "${SOURCE_DIR}/camera-rental-staff"
  pnpm install --frozen-lockfile
  pnpm build:h5:prod
)
cp -R "${SOURCE_DIR}/camera-rental-staff/dist/build/h5/." "${release_dir}/staff/"

echo "[build-deploy] build pc web"
(
  cd "${SOURCE_DIR}/camera-rental-web"
  bun install --frozen-lockfile
  bun run build
)
cp -R "${SOURCE_DIR}/camera-rental-web/.output/." "${release_dir}/web/"

echo "[build-deploy] pack release"
rm -f "${release_archive}"
tar -czf "${release_archive}" -C "${release_dir}" .

exec bash /tmp/camera-rental-server-deploy.sh "${release_archive}"
