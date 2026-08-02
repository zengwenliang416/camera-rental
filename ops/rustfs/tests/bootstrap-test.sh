#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-rustfs-bootstrap.XXXXXX)"
rustfs_root="${test_dir}/rustfs"
mock_bin="${test_dir}/bin"
curl_log="${test_dir}/curl.log"

cleanup() {
  rm -rf "${test_dir}"
}
trap cleanup EXIT

mkdir -p "${rustfs_root}" "${mock_bin}"
cat > "${rustfs_root}/.env" <<'EOF'
RUSTFS_ACCESS_KEY=rustfsroot-test
RUSTFS_SECRET_KEY=rustfs-root-secret
RUSTFS_APP_ACCESS_KEY=returnapp-test
RUSTFS_APP_SECRET_KEY=return-app-secret
RUSTFS_REGION=us-east-1
RUSTFS_BUCKET=camera-rental-return
RUSTFS_RC_VERSION=v0.1.30
EOF

cat > "${mock_bin}/uname" <<'EOF'
#!/usr/bin/env bash
printf 'x86_64\n'
EOF
cat > "${mock_bin}/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

url=""
output=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    http*) url="$1" ;;
    -o)
      shift
      output="$1"
      ;;
  esac
  shift
done

printf '%s\n' "${url}" >> "${CURL_LOG}"
if [[ "${url}" == *.sha256 ]]; then
  printf 'test-checksum  rustfs-cli.tar.gz\n' > "${output}"
else
  printf 'test-archive\n' > "${output}"
fi
EOF
cat > "${mock_bin}/sha256sum" <<'EOF'
#!/usr/bin/env bash
printf 'test-checksum  %s\n' "$1"
EOF
cat > "${mock_bin}/tar" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

target=""
while [ "$#" -gt 0 ]; do
  if [ "$1" = "-C" ]; then
    shift
    target="$1"
  fi
  shift
done

cat > "${target}/rc" <<'RC'
#!/usr/bin/env bash
exit 0
RC
EOF
chmod +x "${mock_bin}/uname" "${mock_bin}/curl" "${mock_bin}/sha256sum" "${mock_bin}/tar"

CURL_LOG="${curl_log}" PATH="${mock_bin}:${PATH}" \
  bash "${repo_root}/ops/rustfs/bootstrap.sh" "${rustfs_root}"

expected_base="rustfs-cli-linux-amd64-v0.1.30.tar.gz"
grep -Fq "/v0.1.30/${expected_base}" "${curl_log}"
grep -Fq "/v0.1.30/${expected_base}.sha256" "${curl_log}"
if grep -Fq -- "-gnu-" "${curl_log}"; then
  echo "glibc-linked RustFS CLI asset must not be used" >&2
  exit 1
fi

echo "RustFS bootstrap tests passed"
