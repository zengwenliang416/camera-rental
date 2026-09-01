#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../../.." && pwd)
FORWARD_SQL="$PROJECT_ROOT/camera-rental-server/sql/mysql/migrations/20260831_054_rental_fulfillment_facts.sql"
DEVELOPMENT_SQL="$SCRIPT_DIR/20260831_054_rental_fulfillment_facts.sql"
ROLLBACK_SQL="$SCRIPT_DIR/rollback-20260831_054_rental_fulfillment_facts.sql"
BASE_SQL="$SCRIPT_DIR/fixture-20260831_054-fulfillment-base.sql"
MAVEN="/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn"
MAVEN_REPO="/Volumes/zwl/maven-repository"

CONTAINER="codex-rental-mysql-054-$$"
VOLUME="${CONTAINER}-data"
MYSQL_PASSWORD="fixture-root-password"
DATABASE="rental_fixture"

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  docker volume rm "$VOLUME" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

for file in "$FORWARD_SQL" "$DEVELOPMENT_SQL" "$ROLLBACK_SQL" "$BASE_SQL"
do
  if [ ! -f "$file" ]; then
    echo "missing fixture input: $file" >&2
    exit 1
  fi
done

if ! cmp -s "$FORWARD_SQL" "$DEVELOPMENT_SQL"; then
  echo "production and development migration copies differ" >&2
  exit 1
fi

docker run --detach \
  --name "$CONTAINER" \
  --publish "127.0.0.1::3306" \
  --mount "type=volume,source=$VOLUME,target=/var/lib/mysql" \
  --env "MYSQL_ROOT_PASSWORD=$MYSQL_PASSWORD" \
  mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci >/dev/null

attempt=0
ready_streak=0
while [ "$ready_streak" -lt 3 ]
do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    docker logs "$CONTAINER" >&2
    exit 1
  fi

  if docker exec \
      --env "MYSQL_PWD=$MYSQL_PASSWORD" \
      "$CONTAINER" \
      mysql \
      --user=root \
      --default-character-set=utf8mb4 \
      --execute="SELECT 1" >/dev/null 2>&1; then
    ready_streak=$((ready_streak + 1))
  else
    ready_streak=0
  fi
  sleep 1
done

mysql_root_exec() {
  docker exec \
    --interactive \
    --env "MYSQL_PWD=$MYSQL_PASSWORD" \
    "$CONTAINER" \
    mysql \
    --user=root \
    --default-character-set=utf8mb4 \
    --batch \
    --raw \
    --skip-column-names
}

mysql_exec() {
  docker exec \
    --interactive \
    --env "MYSQL_PWD=$MYSQL_PASSWORD" \
    "$CONTAINER" \
    mysql \
    --user=root \
    --database="$DATABASE" \
    --default-character-set=utf8mb4 \
    --batch \
    --raw \
    --skip-column-names
}

mysql_query() {
  docker exec \
    --env "MYSQL_PWD=$MYSQL_PASSWORD" \
    "$CONTAINER" \
    mysql \
    --user=root \
    --database="$DATABASE" \
    --default-character-set=utf8mb4 \
    --batch \
    --raw \
    --skip-column-names \
    --execute="$1"
}

assert_equals() {
  expected=$1
  actual=$2
  label=$3
  if [ "$actual" != "$expected" ]; then
    echo "assertion failed: $label (expected=$expected actual=$actual)" >&2
    exit 1
  fi
  echo "${label}_PASS"
}

reset_fixture() {
  mysql_root_exec <<SQL
DROP DATABASE IF EXISTS \`$DATABASE\`;
CREATE DATABASE \`$DATABASE\`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
SQL
  mysql_exec < "$BASE_SQL"
}

echo "MYSQL_CONTAINER=$CONTAINER"
echo "MYSQL_IMAGE=mysql:8.4"
echo "MYSQL_BIND=127.0.0.1:random"

reset_fixture
mysql_exec < "$FORWARD_SQL"

assert_equals "5" "$(mysql_query "
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = '$DATABASE'
    AND (
      (TABLE_NAME = 'rental_order'
        AND COLUMN_NAME IN ('expected_send_back_date', 'settled_at'))
      OR (TABLE_NAME = 'rental_order_item'
        AND COLUMN_NAME = 'expected_send_back_date')
      OR (TABLE_NAME = 'rental_device_assignment'
        AND COLUMN_NAME IN ('inspection_completed_at', 'inspection_result'))
    )
    AND IS_NULLABLE = 'YES';
")" "FULFILLMENT_FACT_SCHEMA"

assert_equals "5" "$(mysql_query "
  SELECT
    (expected_send_back_date IS NULL)
      + (settled_at IS NULL)
      + (
        SELECT expected_send_back_date IS NULL
        FROM rental_order_item
        WHERE id = 1
      )
      + (
        SELECT inspection_completed_at IS NULL
        FROM rental_device_assignment
        WHERE id = 1
      )
      + (
        SELECT inspection_result IS NULL
        FROM rental_device_assignment
        WHERE id = 1
      )
  FROM rental_order
  WHERE id = 1;
")" "EXISTING_ROWS_NULL_DEFAULTS"

mysql_query "
  UPDATE rental_order
  SET expected_send_back_date = '2026-09-01',
      settled_at = '2026-09-03 12:00:00'
  WHERE id = 1;
  UPDATE rental_order_item
  SET expected_send_back_date = '2026-09-01'
  WHERE id = 1;
  UPDATE rental_device_assignment
  SET inspection_completed_at = '2026-09-03 11:00:00',
      inspection_result = 'PASSED'
  WHERE id = 1;
"

assert_equals "2026-09-01|2026-09-03 12:00:00|2026-09-01|2026-09-03 11:00:00|PASSED" \
  "$(mysql_query "
    SELECT CONCAT_WS(
      '|',
      rental_order.expected_send_back_date,
      rental_order.settled_at,
      rental_order_item.expected_send_back_date,
      rental_device_assignment.inspection_completed_at,
      rental_device_assignment.inspection_result
    )
    FROM rental_order
    JOIN rental_order_item ON rental_order_item.id = rental_order.id
    JOIN rental_device_assignment ON rental_device_assignment.id = rental_order.id
    WHERE rental_order.id = 1;
  ")" "FULFILLMENT_FACT_ROUND_TRIP"

mysql_exec < "$ROLLBACK_SQL"

assert_equals "0" "$(mysql_query "
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = '$DATABASE'
    AND COLUMN_NAME IN (
      'expected_send_back_date',
      'settled_at',
      'inspection_completed_at',
      'inspection_result'
    );
")" "ROLLBACK_REMOVES_054_COLUMNS"

assert_equals "6" "$(mysql_query "
  SELECT
    (SELECT COUNT(*) FROM xianyu_order)
      + (SELECT COUNT(*) FROM rental_order)
      + (SELECT COUNT(*) FROM rental_order_item)
      + (SELECT COUNT(*) FROM rental_device)
      + (SELECT COUNT(*) FROM rental_device_assignment)
      + (SELECT COUNT(*) FROM rental_schedule);
")" "ROLLBACK_RETAINS_BASE_ROWS"

reset_fixture
mysql_exec < "$FORWARD_SQL"

MYSQL_PORT=$(docker port "$CONTAINER" 3306/tcp | sed -n '1s/.*://p')
if [ -z "$MYSQL_PORT" ]; then
  echo "cannot resolve disposable MySQL port" >&2
  exit 1
fi

(
  cd "$PROJECT_ROOT/camera-rental-server"
  RENTAL_FULFILLMENT_MYSQL_JDBC_URL="jdbc:mysql://127.0.0.1:$MYSQL_PORT/$DATABASE?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  RENTAL_FULFILLMENT_MYSQL_USER=root \
  RENTAL_FULFILLMENT_MYSQL_PASSWORD="$MYSQL_PASSWORD" \
    "$MAVEN" -o \
      -pl yudao-module-rental/yudao-module-rental-biz -am test \
      -Dmaven.repo.local="$MAVEN_REPO" \
      -Dtest=RentalFulfillmentFactsMigrationTest,RentalFulfillmentLockOrderMysqlConcurrencyTest \
      -Dsurefire.failIfNoSpecifiedTests=false
)

echo "FULFILLMENT_LOCK_ORDER_MYSQL_PASS"
