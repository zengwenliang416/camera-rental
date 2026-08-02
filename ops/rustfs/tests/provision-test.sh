#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-rustfs-provision.XXXXXX)"
rustfs_root="${test_dir}/rustfs"
deploy_root="${test_dir}/deploy"
mock_install="${test_dir}/mock-install.sh"

cleanup() {
  rm -rf "${test_dir}"
}
trap cleanup EXIT

cat > "${mock_install}" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
test -f "$1/.env"
printf 'installed\n' > "$1/install-marker"
EOF
chmod +x "${mock_install}"

RUSTFS_INSTALL_SCRIPT="${mock_install}" \
  bash "${repo_root}/ops/rustfs/provision.sh" "${rustfs_root}" "${deploy_root}"

test -f "${rustfs_root}/install-marker"
test "$(stat -f '%Lp' "${rustfs_root}/.env")" = "600"
test "$(stat -f '%Lp' "${deploy_root}/shared/rustfs-app.env")" = "600"
grep -Eq '^RUSTFS_ACCESS_KEY=rustfsroot[0-9a-f]{16}$' "${rustfs_root}/.env"
grep -Eq '^RUSTFS_SECRET_KEY=[0-9a-f]{64}$' "${rustfs_root}/.env"
grep -Eq '^RUSTFS_APP_ACCESS_KEY=returnapp[0-9a-f]{16}$' \
  "${deploy_root}/shared/rustfs-app.env"
grep -Eq '^RUSTFS_APP_SECRET_KEY=[0-9a-f]{40}$' \
  "${deploy_root}/shared/rustfs-app.env"
if grep -q '^RUSTFS_ACCESS_KEY=' "${deploy_root}/shared/rustfs-app.env"; then
  echo "root access key leaked into application environment" >&2
  exit 1
fi

first_checksum="$(sha256sum "${rustfs_root}/.env" | awk '{print $1}')"
RUSTFS_INSTALL_SCRIPT="${mock_install}" \
  bash "${repo_root}/ops/rustfs/provision.sh" "${rustfs_root}" "${deploy_root}"
second_checksum="$(sha256sum "${rustfs_root}/.env" | awk '{print $1}')"
test "${first_checksum}" = "${second_checksum}"

RUSTFS_HEALTH_URL="file://${rustfs_root}/install-marker" \
  bash "${repo_root}/ops/rustfs/verify.sh" "${rustfs_root}" "${deploy_root}"

echo "RustFS provisioning tests passed"
