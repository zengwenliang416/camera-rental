#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/camera-rental}"
RELEASE_SHA="${RELEASE_SHA:?RELEASE_SHA is required}"
REPO_URL="${REPO_URL:-git@github.com:zengwenliang416/camera-rental.git}"
SOURCE_DIR="${SOURCE_DIR:-${DEPLOY_ROOT}/source}"
GIT_SSH_COMMAND="${GIT_SSH_COMMAND:-ssh -i ~/.ssh/camera_rental_github_pull -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=20 -C}"
export GIT_SSH_COMMAND

build_dir="$(mktemp -d /tmp/camera-rental-build.XXXXXX)"
release_dir="${build_dir}/release"
release_archive="/tmp/camera-rental-${RELEASE_SHA}.tgz"

cleanup() {
  rm -rf "${build_dir}"
}
trap cleanup EXIT

echo "[build-deploy] source=${SOURCE_DIR}"
mkdir -p "${DEPLOY_ROOT}" "$(dirname "${SOURCE_DIR}")"

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
  pnpm ts:check
  pnpm build:prod
)
cp -R "${SOURCE_DIR}/camera-rental-admin/dist/." "${release_dir}/admin/"

echo "[build-deploy] build staff h5"
(
  cd "${SOURCE_DIR}/camera-rental-staff"
  pnpm install --frozen-lockfile
  pnpm build:h5:prod
  pnpm type-check
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
