#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-production-80.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

deploy_root="${test_dir}/deploy"
backend_env="${deploy_root}/shared/backend.env"
web_env="${deploy_root}/shared/web.env"
nginx_config="${test_dir}/camera-rental.conf"
nginx_search_root="${test_dir}/nginx/sites-enabled"
mkdir -p "${deploy_root}/shared" "${nginx_search_root}"

cat > "${backend_env}" <<'EOF'
JAVA_OPTS=-Xms128m
WX_MP_APP_ID=real-app-id
WX_MP_SECRET=
MYBATIS_PLUS_ENCRYPTOR_PASSWORD=0123456789abcdef0123456789abcdef
EOF

cat > "${web_env}" <<'EOF'
HOST=0.0.0.0
PORT=3001
NODE_ENV=development
EOF

cat > "${nginx_config}" <<'EOF'
server {
    server_name rental.motion-cover.com;
    location / {
        proxy_pass http://127.0.0.1:3001;
    }
}
EOF
ln -s "${nginx_config}" "${nginx_search_root}/rental"

DEPLOY_ROOT="${deploy_root}" \
BACKEND_ENV="${backend_env}" \
WEB_ENV="${web_env}" \
NGINX_SEARCH_ROOTS="${nginx_search_root}" \
NGINX_TEST_CMD=true \
  bash "${repo_root}/ops/github-deploy/prepare-production-80.sh"

grep -qx 'HOST=127.0.0.1' "${web_env}"
grep -qx 'PORT=3102' "${web_env}"
grep -qx 'NODE_ENV=production' "${web_env}"
grep -qx 'WX_MP_APP_ID=real-app-id' "${backend_env}"
grep -qx 'WX_MP_SECRET=disabled' "${backend_env}"
grep -qx 'WX_MINIAPP_APP_ID=disabled' "${backend_env}"
grep -qx 'WX_MINIAPP_SECRET=disabled' "${backend_env}"
encryptor_password="$(sed -n 's/^MYBATIS_PLUS_ENCRYPTOR_PASSWORD=//p' "${backend_env}")"
test "${#encryptor_password}" -eq 32
test "${encryptor_password}" = "0123456789abcdef0123456789abcdef"
grep -qx 'SPRINGDOC_API_DOCS_ENABLED=false' "${backend_env}"
grep -qx 'SPRINGDOC_SWAGGER_UI_ENABLED=false' "${backend_env}"
grep -qx 'KNIFE4J_ENABLE=false' "${backend_env}"
grep -q 'proxy_pass http://127.0.0.1:3102;' "${nginx_config}"

sed -i.bak 's/^MYBATIS_PLUS_ENCRYPTOR_PASSWORD=.*/MYBATIS_PLUS_ENCRYPTOR_PASSWORD=invalid-length/' "${backend_env}"
if DEPLOY_ROOT="${deploy_root}" \
  BACKEND_ENV="${backend_env}" \
  WEB_ENV="${web_env}" \
  NGINX_SEARCH_ROOTS="${nginx_search_root}" \
  NGINX_TEST_CMD=true \
    bash "${repo_root}/ops/github-deploy/prepare-production-80.sh" \
    >"${test_dir}/invalid-key.out" 2>"${test_dir}/invalid-key.err"; then
  echo "prepare-production-80 should reject an existing invalid encryptor key" >&2
  exit 1
fi
grep -q 'refusing automatic key rotation' "${test_dir}/invalid-key.err"

echo "production-80 preparation tests passed"
