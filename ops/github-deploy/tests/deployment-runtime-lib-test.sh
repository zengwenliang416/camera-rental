#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-runtime-health.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

bin_dir="${test_dir}/bin"
mkdir -p "${bin_dir}"

cat > "${bin_dir}/systemctl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [ "${1:-}" = "is-active" ]; then
  [ "${FAKE_SERVICE_ACTIVE:-true}" = "true" ]
  exit
fi
exit 0
EOF

cat > "${bin_dir}/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s' "${FAKE_HTTP_STATUS:-200}"
EOF

cat > "${bin_dir}/journalctl" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

chmod +x "${bin_dir}/systemctl" "${bin_dir}/curl" "${bin_dir}/journalctl"

# shellcheck source=../deployment-runtime-lib.sh
source "${repo_root}/ops/github-deploy/deployment-runtime-lib.sh"

PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=401 \
  wait_for_service_http backend.service http://backend.test reachable 1 0 0
PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=200 \
  wait_for_service_http web.service http://web.test success 1 0 0

if PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=500 \
  wait_for_service_http backend.service http://backend.test reachable 1 0 0 \
  >/dev/null 2>&1; then
  echo "FAIL: backend HTTP 500 must fail the deployment health check" >&2
  exit 1
fi

if PATH="${bin_dir}:${PATH}" FAKE_SERVICE_ACTIVE=false \
  wait_for_service_http web.service http://web.test success 1 0 0 \
  >/dev/null 2>&1; then
  echo "FAIL: inactive service must fail the deployment health check" >&2
  exit 1
fi

echo "deployment runtime health tests passed"
