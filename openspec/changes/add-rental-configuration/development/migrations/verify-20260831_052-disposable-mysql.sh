#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../../.." && pwd)
FORWARD_SQL="$PROJECT_ROOT/camera-rental-server/sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql"
ROLLBACK_SQL="$SCRIPT_DIR/rollback-20260831_052_rental_configuration_foundation.sql"
BASE_SQL="$SCRIPT_DIR/fixture-20260831_052-base.sql"
FORWARD_ASSERTIONS_SQL="$SCRIPT_DIR/fixture-20260831_052-forward-assertions.sql"
ROLLBACK_ASSERTIONS_SQL="$SCRIPT_DIR/fixture-20260831_052-rollback-assertions.sql"

CONTAINER="codex-rental-mysql-052-$$"
VOLUME="${CONTAINER}-data"
MYSQL_PASSWORD="fixture-root-password"

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  docker volume rm "$VOLUME" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

for file in \
  "$FORWARD_SQL" \
  "$ROLLBACK_SQL" \
  "$BASE_SQL" \
  "$FORWARD_ASSERTIONS_SQL" \
  "$ROLLBACK_ASSERTIONS_SQL"
do
  if [ ! -f "$file" ]; then
    echo "missing fixture input: $file" >&2
    exit 1
  fi
done

docker run --detach \
  --name "$CONTAINER" \
  --network none \
  --mount "type=volume,source=$VOLUME,target=/var/lib/mysql" \
  --env "MYSQL_ROOT_PASSWORD=$MYSQL_PASSWORD" \
  --env "MYSQL_DATABASE=rental_fixture" \
  mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci >/dev/null

attempt=0
until docker exec \
  --env "MYSQL_PWD=$MYSQL_PASSWORD" \
  "$CONTAINER" \
  mysql \
  --user=root \
  --database=rental_fixture \
  --execute="SELECT 1" >/dev/null 2>&1
do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    docker logs "$CONTAINER" >&2
    exit 1
  fi
  sleep 2
done

mysql_exec() {
  docker exec \
    --interactive \
    --env "MYSQL_PWD=$MYSQL_PASSWORD" \
    "$CONTAINER" \
    mysql \
    --user=root \
    --database=rental_fixture \
    --batch \
    --raw
}

echo "MYSQL_CONTAINER=$CONTAINER"
echo "MYSQL_IMAGE=mysql:8.4"
echo "MYSQL_NETWORK=none"

mysql_exec < "$BASE_SQL"
echo "BASE_SCHEMA_PASS"

mysql_exec < "$FORWARD_SQL"
echo "FORWARD_MIGRATION_PASS"

mysql_exec < "$FORWARD_ASSERTIONS_SQL"

mysql_exec < "$ROLLBACK_SQL"
echo "ROLLBACK_MIGRATION_PASS"

mysql_exec < "$ROLLBACK_ASSERTIONS_SQL"

echo "DISPOSABLE_MYSQL_052_PASS"
