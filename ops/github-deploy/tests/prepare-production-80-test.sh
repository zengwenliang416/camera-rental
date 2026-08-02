#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-production-80.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

deploy_root="${test_dir}/deploy"
backend_env="${deploy_root}/shared/backend.env"
web_env="${deploy_root}/shared/web.env"
nginx_config="${test_dir}/camera-rental.conf"
mkdir -p "${deploy_root}/shared"

cat > "${backend_env}" <<'EOF'
JAVA_OPTS=-Xms128m
WX_MP_APP_ID=real-app-id
WX_MP_SECRET=
EOF

cat > "${web_env}" <<'EOF'
HOST=0.0.0.0
PORT=3001
NODE_ENV=development
EOF

cat > "${nginx_config}" <<'EOF'
server {
    location / {
        proxy_pass http://127.0.0.1:3001;
    }
}
EOF

DEPLOY_ROOT="${deploy_root}" \
BACKEND_ENV="${backend_env}" \
WEB_ENV="${web_env}" \
NGINX_CONFIG="${nginx_config}" \
NGINX_TEST_CMD=true \
  bash "${repo_root}/ops/github-deploy/prepare-production-80.sh"

grep -qx 'HOST=127.0.0.1' "${web_env}"
grep -qx 'PORT=3102' "${web_env}"
grep -qx 'NODE_ENV=production' "${web_env}"
grep -qx 'WX_MP_APP_ID=real-app-id' "${backend_env}"
grep -qx 'WX_MP_SECRET=disabled' "${backend_env}"
grep -qx 'WX_MINIAPP_APP_ID=disabled' "${backend_env}"
grep -qx 'WX_MINIAPP_SECRET=disabled' "${backend_env}"
grep -q 'proxy_pass http://127.0.0.1:3102;' "${nginx_config}"

echo "production-80 preparation tests passed"
