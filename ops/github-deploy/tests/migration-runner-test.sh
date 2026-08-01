#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-migration-test.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

release_dir="${test_dir}/release"
deploy_root="${test_dir}/deploy"
bin_dir="${test_dir}/bin"
state_file="${test_dir}/migration-state"
mkdir -p "${release_dir}/server/migrations" "${deploy_root}/shared" "${bin_dir}"

cat > "${deploy_root}/shared/deploy.env" <<'EOF'
CAMERA_RENTAL_DB_HOST=127.0.0.1
CAMERA_RENTAL_DB_PORT=3306
CAMERA_RENTAL_DB_NAME=camera_rental
CAMERA_RENTAL_DB_USER=test
CAMERA_RENTAL_DB_PASSWORD=test
EOF

cat > "${release_dir}/server/migrations.txt" <<'EOF'
camera-rental-server/sql/mysql/migrations/001_test.sql
EOF
printf 'CREATE TABLE example(id bigint);\n' > "${release_dir}/server/migrations/001_test.sql"

cat > "${bin_dir}/mysql" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
state="${FAKE_MYSQL_STATE:?}"
query=""
while [ "$#" -gt 0 ]; do
  if [ "$1" = "-e" ]; then
    query="$2"
    shift 2
    continue
  fi
  shift
done
if [[ "${query}" == SELECT\ checksum* ]]; then
  [ -f "${state}" ] && cat "${state}" || true
elif [[ "${query}" == INSERT\ INTO\ camera_rental_schema_migration* ]]; then
  checksum="$(printf '%s' "${query}" | awk -F"'" '{print $4}')"
  printf '%s\n' "${checksum}" > "${state}"
else
  cat >/dev/null || true
fi
EOF
chmod +x "${bin_dir}/mysql"

run_migrations() {
  PATH="${bin_dir}:${PATH}" \
  FAKE_MYSQL_STATE="${state_file}" \
  DEPLOY_ROOT="${deploy_root}" \
  RELEASE_SHA="test-sha" \
    bash "${repo_root}/ops/github-deploy/apply-migrations.sh" "${release_dir}"
}

run_migrations
run_migrations

printf 'ALTER TABLE example ADD COLUMN name varchar(10);\n' \
  > "${release_dir}/server/migrations/001_test.sql"
if run_migrations >/dev/null 2>&1; then
  echo "FAIL: changed migration checksum should block deployment" >&2
  exit 1
fi

echo "migration runner tests passed"
