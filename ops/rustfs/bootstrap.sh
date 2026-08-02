#!/usr/bin/env bash
set -euo pipefail

root="${1:-/opt/camera-rental/rustfs}"
# shellcheck disable=SC1090
source "${root}/.env"

: "${RUSTFS_ACCESS_KEY:?}"
: "${RUSTFS_SECRET_KEY:?}"
: "${RUSTFS_APP_ACCESS_KEY:?}"
: "${RUSTFS_APP_SECRET_KEY:?}"
: "${RUSTFS_BUCKET:?}"

rc_version="${RUSTFS_RC_VERSION:-v0.1.30}"
rc_bin="${root}/bin/rc"
rc_home="${root}/rc-home"
mkdir -p "${root}/bin" "${rc_home}"
chmod 700 "${root}/bin" "${rc_home}"

install_rc() {
  local arch asset archive checksum expected actual url checksum_url
  case "$(uname -m)" in
    x86_64|amd64) arch="amd64" ;;
    aarch64|arm64) arch="arm64" ;;
    *) echo "[rustfs] unsupported rc architecture: $(uname -m)" >&2; exit 1 ;;
  esac
  # The default Linux release is statically linked. The GNU build currently
  # requires a newer glibc than Ubuntu 22.04 provides.
  asset="rustfs-cli-linux-${arch}-${rc_version}.tar.gz"
  archive="$(mktemp)"
  checksum="$(mktemp)"

  for url in \
    "https://github.com/rustfs/cli/releases/download/${rc_version}/${asset}" \
    "https://gh-proxy.com/https://github.com/rustfs/cli/releases/download/${rc_version}/${asset}" \
    "https://ghfast.top/https://github.com/rustfs/cli/releases/download/${rc_version}/${asset}"; do
    if curl -fsSL --retry 3 --connect-timeout 15 "${url}" -o "${archive}"; then
      break
    fi
  done
  test -s "${archive}"
  for checksum_url in \
    "https://github.com/rustfs/cli/releases/download/${rc_version}/${asset}.sha256" \
    "https://gh-proxy.com/https://github.com/rustfs/cli/releases/download/${rc_version}/${asset}.sha256" \
    "https://ghfast.top/https://github.com/rustfs/cli/releases/download/${rc_version}/${asset}.sha256"; do
    if curl -fsSL --retry 3 --connect-timeout 15 "${checksum_url}" -o "${checksum}"; then
      break
    fi
  done
  test -s "${checksum}"
  expected="$(awk 'NR == 1 { print $1 }' "${checksum}")"
  actual="$(sha256sum "${archive}" | awk '{ print $1 }')"
  test -n "${expected}"
  if [ "${actual}" != "${expected}" ]; then
    rm -f "${archive}" "${checksum}"
    echo "[rustfs] rc checksum mismatch" >&2
    exit 1
  fi

  tar -xzf "${archive}" -C "${root}/bin" rc
  chmod 0755 "${rc_bin}"
  rm -f "${archive}" "${checksum}"
}

if [ ! -x "${rc_bin}" ]; then
  echo "[rustfs] install rc ${rc_version}"
  install_rc
fi

export HOME="${rc_home}"
"${rc_bin}" --quiet alias set local http://127.0.0.1:9000 \
  "${RUSTFS_ACCESS_KEY}" "${RUSTFS_SECRET_KEY}" \
  --region "${RUSTFS_REGION:-us-east-1}" --bucket-lookup path
"${rc_bin}" --quiet ready local/
"${rc_bin}" --quiet bucket create "local/${RUSTFS_BUCKET}" \
  --region "${RUSTFS_REGION:-us-east-1}" --ignore-existing

policy_file="$(mktemp)"
trap 'rm -f "${policy_file}"' EXIT
cat > "${policy_file}" <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetBucketLocation",
        "s3:ListBucket",
        "s3:ListBucketMultipartUploads"
      ],
      "Resource": ["arn:aws:s3:::${RUSTFS_BUCKET}"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:AbortMultipartUpload",
        "s3:DeleteObject",
        "s3:GetObject",
        "s3:ListMultipartUploadParts",
        "s3:PutObject"
      ],
      "Resource": ["arn:aws:s3:::${RUSTFS_BUCKET}/*"]
    }
  ]
}
EOF

if "${rc_bin}" admin access-key info local/ "${RUSTFS_APP_ACCESS_KEY}" --json \
  >/dev/null 2>&1; then
  "${rc_bin}" --quiet admin service-account update local/ "${RUSTFS_APP_ACCESS_KEY}" \
    --policy "${policy_file}" --description "camera-rental return uploads"
else
  "${rc_bin}" --quiet admin service-account create local/ \
    "${RUSTFS_APP_ACCESS_KEY}" "${RUSTFS_APP_SECRET_KEY}" \
    --policy "${policy_file}" --description "camera-rental return uploads"
fi

"${rc_bin}" admin access-key info local/ "${RUSTFS_APP_ACCESS_KEY}" --json \
  >/dev/null
echo "[rustfs] private bucket and least-privilege application account are ready"
