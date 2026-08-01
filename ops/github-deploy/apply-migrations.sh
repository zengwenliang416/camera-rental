#!/usr/bin/env bash
set -euo pipefail

release_dir="${1:?usage: apply-migrations.sh RELEASE_DIR}"
deploy_root="${DEPLOY_ROOT:-/opt/camera-rental}"
env_file="${DEPLOY_ENV_FILE:-${deploy_root}/shared/deploy.env}"
manifest="${release_dir}/server/migrations.txt"

test -f "${env_file}"
test -f "${manifest}"
# shellcheck disable=SC1090
source "${env_file}"
: "${CAMERA_RENTAL_DB_HOST:?}"
: "${CAMERA_RENTAL_DB_PORT:=3306}"
: "${CAMERA_RENTAL_DB_NAME:?}"
: "${CAMERA_RENTAL_DB_USER:?}"
: "${CAMERA_RENTAL_DB_PASSWORD:?}"

mysql_args=(
  --protocol=TCP
  --host="${CAMERA_RENTAL_DB_HOST}"
  --port="${CAMERA_RENTAL_DB_PORT}"
  --user="${CAMERA_RENTAL_DB_USER}"
  --database="${CAMERA_RENTAL_DB_NAME}"
  --default-character-set=utf8mb4
  --batch
  --skip-column-names
)

export MYSQL_PWD="${CAMERA_RENTAL_DB_PASSWORD}"
trap 'unset MYSQL_PWD' EXIT

mysql "${mysql_args[@]}" <<'SQL'
CREATE TABLE IF NOT EXISTS camera_rental_schema_migration (
  migration_id varchar(255) NOT NULL,
  checksum char(64) NOT NULL,
  applied_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  release_sha varchar(64) DEFAULT NULL,
  PRIMARY KEY (migration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SQL

while IFS= read -r relative_path; do
  [ -n "${relative_path}" ] || continue
  migration="${release_dir}/server/migrations/${relative_path##*/}"
  test -f "${migration}"
  migration_id="${relative_path##*/}"
  checksum="$(sha256sum "${migration}" | awk '{print $1}')"
  existing="$(mysql "${mysql_args[@]}" -e "SELECT checksum FROM camera_rental_schema_migration WHERE migration_id='${migration_id}'")"
  if [ -n "${existing}" ]; then
    if [ "${existing}" != "${checksum}" ]; then
      echo "[migration][error] checksum changed for ${migration_id}" >&2
      exit 1
    fi
    echo "[migration] already applied ${migration_id}"
    continue
  fi
  echo "[migration] apply ${migration_id}"
  mysql "${mysql_args[@]}" < "${migration}"
  mysql "${mysql_args[@]}" -e "INSERT INTO camera_rental_schema_migration(migration_id, checksum, release_sha) VALUES('${migration_id}', '${checksum}', '${RELEASE_SHA:-manual}')"
done < "${manifest}"
