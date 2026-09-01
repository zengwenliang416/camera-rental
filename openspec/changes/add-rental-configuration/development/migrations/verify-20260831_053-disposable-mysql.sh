#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../../.." && pwd)
FOUNDATION_SQL="$PROJECT_ROOT/camera-rental-server/sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql"
FORWARD_SQL="$PROJECT_ROOT/camera-rental-server/sql/mysql/migrations/20260831_053_rental_configuration_backend.sql"
ROLLBACK_SQL="$SCRIPT_DIR/rollback-20260831_053_rental_configuration_backend.sql"
SEED_SQL="$SCRIPT_DIR/20260831_rental_configuration_skipped_items.sql"
FOUNDATION_BASE_SQL="$SCRIPT_DIR/fixture-20260831_052-base.sql"
CATALOG_BASE_SQL="$SCRIPT_DIR/fixture-20260831_053-catalog-base.sql"

CONTAINER="codex-rental-mysql-053-$$"
VOLUME="${CONTAINER}-data"
MYSQL_PASSWORD="fixture-root-password"
DATABASE="rental_fixture"

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  docker volume rm "$VOLUME" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

for file in \
  "$FOUNDATION_SQL" \
  "$FORWARD_SQL" \
  "$ROLLBACK_SQL" \
  "$SEED_SQL" \
  "$FOUNDATION_BASE_SQL" \
  "$CATALOG_BASE_SQL"
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
  mysql_exec < "$FOUNDATION_BASE_SQL"
  mysql_exec < "$FOUNDATION_SQL"
  mysql_exec < "$CATALOG_BASE_SQL"
  mysql_exec < "$FORWARD_SQL"
}

run_seed() {
  tenant_id=$1
  confirmation=$2
  {
    printf "SET @rental_configuration_seed_tenant_id = %s;\n" "$tenant_id"
    printf "SET @rental_configuration_seed_confirmation = '%s';\n" "$confirmation"
    cat "$SEED_SQL"
  } | mysql_exec
}

insert_shop() {
  shop_id=$1
  tenant_id=$2
  external_shop_id=$3
  shop_name=$4
  authorization_status=$5
  deleted=$6
  authorization_expires_at=${7:-NULL}
  mysql_query "
    INSERT INTO xianyu_shop (
      id, tenant_id, application_id, external_shop_id,
      shop_name, authorization_status, authorization_expires_at, deleted
    ) VALUES (
      $shop_id, $tenant_id, 1, '$external_shop_id',
      '$shop_name', '$authorization_status', $authorization_expires_at, b'$deleted'
    );
  "
}

expect_seed_failure() {
  tenant_id=$1
  if run_seed "$tenant_id" "SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS" \
      >/tmp/codex-rental-seed-053-$$.log 2>&1; then
    cat /tmp/codex-rental-seed-053-$$.log >&2
    rm -f /tmp/codex-rental-seed-053-$$.log
    echo "expected seed failure for tenant $tenant_id" >&2
    exit 1
  fi
  cat /tmp/codex-rental-seed-053-$$.log
  rm -f /tmp/codex-rental-seed-053-$$.log
}

echo "MYSQL_CONTAINER=$CONTAINER"
echo "MYSQL_IMAGE=mysql:8.4"
echo "MYSQL_NETWORK=none"

reset_fixture
insert_shop 7701 77 "tenant-77-xiaoj" " 小疆 " "VALID" 0
insert_shop 7702 77 "tenant-77-fafa" "发发" "VALID" 0
insert_shop 7703 77 "tenant-77-xiaoj-invalid" "小疆" "EXPIRED" 0
insert_shop 8801 88 "tenant-88-xiaoj" "小疆" "VALID" 0
insert_shop 8802 88 "tenant-88-fafa" "发发" "VALID" 0

run_seed 77 "SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS"

assert_equals "2" "$(mysql_query "
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = '$DATABASE'
    AND TABLE_NAME IN ('rental_device_category', 'rental_device_model')
    AND COLUMN_NAME = 'lock_version'
    AND DATA_TYPE = 'int'
    AND IS_NULLABLE = 'NO'
    AND COLUMN_DEFAULT = '0';
")" "LOCK_VERSION_SCHEMA"

assert_equals "2" "$(mysql_query "
  SELECT COUNT(*)
  FROM (
    SELECT lock_version FROM rental_device_category WHERE id = 1
    UNION ALL
    SELECT lock_version FROM rental_device_model WHERE id = 1
  ) existing_rows
  WHERE lock_version = 0;
")" "LOCK_VERSION_EXISTING_ROWS"

assert_equals "1" "$(mysql_query "
  UPDATE rental_device_category
  SET category_name = 'Fixture category updated',
      lock_version = lock_version + 1
  WHERE id = 1
    AND lock_version = 0;
  SELECT ROW_COUNT();
")" "CATEGORY_OPTIMISTIC_LOCK_FIRST_UPDATE"

assert_equals "0" "$(mysql_query "
  UPDATE rental_device_category
  SET category_name = 'Fixture category stale update',
      lock_version = lock_version + 1
  WHERE id = 1
    AND lock_version = 0;
  SELECT ROW_COUNT();
")" "CATEGORY_OPTIMISTIC_LOCK_STALE_UPDATE"

assert_equals "1" "$(mysql_query "
  UPDATE rental_device_model
  SET model_name = 'Fixture model updated',
      lock_version = lock_version + 1
  WHERE id = 1
    AND lock_version = 0;
  SELECT ROW_COUNT();
")" "MODEL_OPTIMISTIC_LOCK_FIRST_UPDATE"

assert_equals "0" "$(mysql_query "
  UPDATE rental_device_model
  SET model_name = 'Fixture model stale update',
      lock_version = lock_version + 1
  WHERE id = 1
    AND lock_version = 0;
  SELECT ROW_COUNT();
")" "MODEL_OPTIMISTIC_LOCK_STALE_UPDATE"

assert_equals "29" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND deleted = b'0';
")" "SEED_TOTAL"

assert_equals "17" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND shop_id = 7701
    AND deleted = b'0';
")" "SEED_XIAOJ_TOTAL"

assert_equals "12" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND shop_id = 7702
    AND deleted = b'0';
")" "SEED_FAFA_TOTAL"

assert_equals "29" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND handling_policy = 'CONFIG_SKIPPED'
    AND mapping_mode = 'NONE'
    AND enabled = b'1'
    AND single_device_model_id IS NULL
    AND deleted = b'0';
")" "SEED_POLICY"

assert_equals "29" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND xianyu_item_id IN (
      '1062409679830', '1061015327345', '1042851395917',
      '1021749783370', '1015758423054', '996427340341',
      '994967964760', '980821925580', '946426581576',
      '969348191931', '964654687997', '989974832741',
      '984580566155', '930100016211', '983025637118',
      '985707224806', '986580601148', '1024163647751',
      '1022288043626', '1018390062846', '1017700474288',
      '1015971948191', '997210149459', '994824734648',
      '995640812523', '946905413897', '977248345425',
      '957670857301', '942506886325'
    );
")" "SEED_STRING_IDENTIFIERS"

assert_equals "0" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id <> 77
     OR shop_id NOT IN (7701, 7702);
")" "SEED_TENANT_SHOP_SCOPE"

reset_fixture
insert_shop 7711 77 "zero-fafa" "发发" "VALID" 0
expect_seed_failure 77
assert_equals "0" "$(mysql_query "
  SELECT COUNT(*) FROM rental_channel_product_rule;
")" "ZERO_MATCH_ZERO_WRITE"

reset_fixture
insert_shop 7712 77 "expired-xiaoj" "小疆" "VALID" 0
insert_shop 7713 77 "expired-fafa" "发发" "VALID" 0 \
  "TIMESTAMPADD(HOUR, -1, TIMESTAMPADD(HOUR, 8, UTC_TIMESTAMP()))"
expect_seed_failure 77
assert_equals "0" "$(mysql_query "
  SELECT COUNT(*) FROM rental_channel_product_rule;
")" "EXPIRED_AUTHORIZATION_ZERO_WRITE"

reset_fixture
insert_shop 7721 77 "duplicate-xiaoj-a" "小疆" "VALID" 0
insert_shop 7722 77 "duplicate-xiaoj-b" " 小疆 " "VALID" 0
insert_shop 7723 77 "duplicate-fafa" "发发" "VALID" 0
expect_seed_failure 77
assert_equals "0" "$(mysql_query "
  SELECT COUNT(*) FROM rental_channel_product_rule;
")" "DUPLICATE_MATCH_ZERO_WRITE"

reset_fixture
insert_shop 7731 77 "partial-xiaoj" "小疆" "VALID" 0
insert_shop 7732 77 "partial-fafa-a" "发发" "VALID" 0
insert_shop 7733 77 "partial-fafa-b" " 发发 " "VALID" 0
expect_seed_failure 77
assert_equals "0" "$(mysql_query "
  SELECT COUNT(*) FROM rental_channel_product_rule;
")" "NO_PARTIAL_WRITE"

reset_fixture
insert_shop 7741 77 "guard-xiaoj" "小疆" "VALID" 0
insert_shop 7742 77 "guard-fafa" "发发" "VALID" 0
if run_seed 77 "WRONG_SENTINEL" >/tmp/codex-rental-seed-053-$$.log 2>&1; then
  cat /tmp/codex-rental-seed-053-$$.log >&2
  rm -f /tmp/codex-rental-seed-053-$$.log
  echo "expected seed confirmation failure" >&2
  exit 1
fi
cat /tmp/codex-rental-seed-053-$$.log
rm -f /tmp/codex-rental-seed-053-$$.log
assert_equals "0" "$(mysql_query "
  SELECT COUNT(*) FROM rental_channel_product_rule;
")" "CONFIRMATION_GUARD_ZERO_WRITE"

reset_fixture
insert_shop 7761 77 "conflict-xiaoj" "小疆" "VALID" 0
insert_shop 7762 77 "conflict-fafa" "发发" "VALID" 0
mysql_query "
  INSERT INTO rental_channel_product_rule (
    tenant_id, shop_id, xianyu_item_id, handling_policy,
    mapping_mode, single_device_model_id, enabled, rule_note
  ) VALUES (
    77, 7762, '942506886325', 'MANUAL_REVIEW',
    'SINGLE', 1, b'1', 'Pre-existing different rule'
  );
"
expect_seed_failure 77
assert_equals "1" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77;
")" "CONFLICT_RETAINS_ONLY_EXISTING_RULE"
assert_equals "1" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND shop_id = 7762
    AND xianyu_item_id = '942506886325'
    AND handling_policy = 'MANUAL_REVIEW'
    AND mapping_mode = 'SINGLE'
    AND single_device_model_id = 1
    AND enabled = b'1';
")" "CONFLICT_DOES_NOT_OVERWRITE"
assert_equals "0" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND shop_id = 7761;
")" "CONFLICT_ZERO_PARTIAL_INSERT"

reset_fixture
insert_shop 7751 77 "rollback-xiaoj" "小疆" "VALID" 0
insert_shop 7752 77 "rollback-fafa" "发发" "VALID" 0
run_seed 77 "SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS"
mysql_exec < "$ROLLBACK_SQL"

assert_equals "0" "$(mysql_query "
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = '$DATABASE'
    AND TABLE_NAME IN ('rental_device_category', 'rental_device_model')
    AND COLUMN_NAME = 'lock_version';
")" "ROLLBACK_LOCK_VERSION"

assert_equals "29" "$(mysql_query "
  SELECT COUNT(*)
  FROM rental_channel_product_rule
  WHERE tenant_id = 77
    AND deleted = b'0';
")" "ROLLBACK_RETAINS_SEED_DATA"

cleanup
trap - EXIT INT TERM

if docker container inspect "$CONTAINER" >/dev/null 2>&1; then
  echo "fixture container was not removed: $CONTAINER" >&2
  exit 1
fi

if docker volume inspect "$VOLUME" >/dev/null 2>&1; then
  echo "fixture volume was not removed: $VOLUME" >&2
  exit 1
fi

echo "CLEANUP_PASS"
echo "DISPOSABLE_MYSQL_053_PASS"
