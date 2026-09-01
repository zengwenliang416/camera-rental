#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../../.." && pwd)
FORWARD_SQL="$PROJECT_ROOT/camera-rental-server/sql/mysql/migrations/20260901_055_rental_historical_reconciliation.sql"
DEVELOPMENT_SQL="$SCRIPT_DIR/20260901_055_rental_historical_reconciliation.sql"
ROLLBACK_SQL="$SCRIPT_DIR/rollback-20260901_055_rental_historical_reconciliation.sql"
FULFILLMENT_SQL="$PROJECT_ROOT/camera-rental-server/sql/mysql/migrations/20260831_054_rental_fulfillment_facts.sql"
BASE_SQL="$SCRIPT_DIR/fixture-20260831_054-fulfillment-base.sql"
MAVEN="/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn"
MAVEN_REPO="/Volumes/zwl/maven-repository"

CONTAINER="codex-rental-mysql-055-$$"
VOLUME="${CONTAINER}-data"
MYSQL_PASSWORD="fixture-root-password"
DATABASE="rental_fixture"

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  docker volume rm "$VOLUME" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

for file in "$FORWARD_SQL" "$DEVELOPMENT_SQL" "$ROLLBACK_SQL" "$FULFILLMENT_SQL" "$BASE_SQL"
do
  if [ ! -f "$file" ]; then
    echo "missing migration input: $file" >&2
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
}

reset_fixture

echo "MYSQL_CONTAINER=$CONTAINER"
echo "MYSQL_IMAGE=mysql:8.4"
echo "MYSQL_BIND=127.0.0.1:random"

mysql_exec < "$FORWARD_SQL"

assert_equals "2" "$(mysql_query "
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = '$DATABASE'
    AND TABLE_NAME IN (
      'rental_historical_reconciliation_run',
      'rental_historical_reconciliation_failure'
    );
")" "HISTORICAL_RECONCILIATION_TABLES"

assert_equals "5" "$(mysql_query "
  SELECT COUNT(DISTINCT INDEX_NAME)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = '$DATABASE'
    AND INDEX_NAME IN (
      'idx_rental_history_run_status',
      'idx_rental_history_run_lease',
      'idx_rental_history_run_range',
      'idx_rental_history_failure_run',
      'idx_rental_history_failure_order'
    );
")" "HISTORICAL_RECONCILIATION_INDEXES"

mysql_exec <<SQL
INSERT INTO rental_historical_reconciliation_run (
  tenant_id, dry_run, status, start_after_id, end_id_inclusive,
  cursor_after_id, batch_size, scanned_count, created_count,
  conflict_count, failed_count, review_required_count,
  execution_token, lease_until, heartbeat_at
) VALUES (
  9, b'0', 'FAILED', 100, 200, 150, 25, 50, 40, 2, 1, 3,
  'worker-1', '2026-09-01 00:35:00', '2026-09-01 00:30:00'
);

INSERT INTO rental_historical_reconciliation_failure (
  tenant_id, run_id, channel_order_id, cursor_before_id,
  attempt_no, error_code
) VALUES (
  9, LAST_INSERT_ID(), 151, 150, 1, 'IllegalStateException'
);
SQL

assert_equals "FAILED|100|200|150|25|50|40|2|1|3" "$(mysql_query "
  SELECT CONCAT_WS(
    '|', status, start_after_id, end_id_inclusive, cursor_after_id,
    batch_size, scanned_count, created_count, conflict_count,
    failed_count, review_required_count
  )
  FROM rental_historical_reconciliation_run
  WHERE tenant_id = 9;
")" "RUN_CHECKPOINT_ROUND_TRIP"

assert_equals "worker-1|1|1" "$(mysql_query "
  SELECT CONCAT_WS(
    '|', execution_token, lease_until IS NOT NULL, heartbeat_at IS NOT NULL
  )
  FROM rental_historical_reconciliation_run
  WHERE tenant_id = 9;
")" "RUN_LEASE_ROUND_TRIP"

assert_equals "151|150|1|IllegalStateException" "$(mysql_query "
  SELECT CONCAT_WS(
    '|', channel_order_id, cursor_before_id, attempt_no, error_code
  )
  FROM rental_historical_reconciliation_failure
  WHERE tenant_id = 9;
")" "FAILURE_BOUNDARY_ROUND_TRIP"

mysql_exec < "$ROLLBACK_SQL"

assert_equals "0" "$(mysql_query "
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = '$DATABASE'
    AND TABLE_NAME IN (
      'rental_historical_reconciliation_run',
      'rental_historical_reconciliation_failure'
    );
")" "ROLLBACK_REMOVES_055_TABLES"

reset_fixture
mysql_exec < "$BASE_SQL"
mysql_exec < "$FULFILLMENT_SQL"
mysql_exec < "$FORWARD_SQL"

MYSQL_PORT=$(docker port "$CONTAINER" 3306/tcp | sed -n '1s/.*://p')
if [ -z "$MYSQL_PORT" ]; then
  echo "cannot resolve disposable MySQL port" >&2
  exit 1
fi

(
  cd "$PROJECT_ROOT/camera-rental-server"
  RENTAL_HISTORICAL_MYSQL_JDBC_URL="jdbc:mysql://127.0.0.1:$MYSQL_PORT/$DATABASE?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  RENTAL_HISTORICAL_MYSQL_USER=root \
  RENTAL_HISTORICAL_MYSQL_PASSWORD="$MYSQL_PASSWORD" \
    "$MAVEN" -o \
      -pl yudao-module-rental/yudao-module-rental-biz -am test \
      -Dmaven.repo.local="$MAVEN_REPO" \
      -Dtest=RentalHistoricalOrderBackfillMysqlIntegrationTest \
      -Dsurefire.failIfNoSpecifiedTests=false
)

echo "HISTORICAL_RECONCILIATION_REAL_SERVICE_MYSQL_PASS"
echo "DISPOSABLE_MYSQL_055_PASS"
