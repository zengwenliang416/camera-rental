# Development Migrations: add-rental-configuration

## Execution Order

1. Apply `camera-rental-server/sql/mysql/migrations/20260831_052_rental_configuration_foundation.sql`.
2. Apply `camera-rental-server/sql/mysql/migrations/20260831_053_rental_configuration_backend.sql`.
3. Apply `camera-rental-server/sql/mysql/migrations/20260831_054_rental_fulfillment_facts.sql`.
4. Apply `camera-rental-server/sql/mysql/migrations/20260901_055_rental_historical_reconciliation.sql`.
5. Apply `camera-rental-server/sql/mysql/migrations/20260901_056_rental_channel_reconciliation_run.sql`.
6. Deploy backend code that writes the explicit identifier and fulfillment-fact columns.
7. Resynchronize authorized shops before product detail synchronization so
   `xianyu_user_name` is populated.
8. Run the post-migration verification queries before enabling configuration APIs.
9. Keep `rental.historical-backfill.write-enabled=false` until an approved
   historical dry-run has been reviewed.
10. Use `docs/integrations/xianyu/rental-configuration-release.md` as the
    release, monitoring, stop-condition and rollback checklist.

The development copies under this directory are byte-identical to their
production-path SQL files and exist for SpecNav review and checksum validation.
Deploy only the production-path migration files, in order.

## Controlled Seed

`20260831_rental_configuration_skipped_items.sql` is a separately controlled
data seed, not a normal migration. It is not automatically deployed and does
not select or connect to a production database.

Before using it, obtain the required backup and approval, identify one tenant,
and verify that the tenant has exactly one active, unexpired `VALID` shop whose
trimmed name is `小疆` and exactly one whose trimmed name is `发发`. A non-null
authorization expiry at or before the current `Asia/Shanghai` time is rejected.
Set the tenant and fixed confirmation sentinel in the same MySQL session that
sources the file:

```sql
SET @rental_configuration_seed_tenant_id = <positive tenant id>;
SET @rental_configuration_seed_confirmation =
  'SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS';
SOURCE development/migrations/20260831_rental_configuration_skipped_items.sql;
```

The seed inserts only the 29 approved string item identifiers with
`CONFIG_SKIPPED`, `NONE`, and `enabled = b'1'`. A zero or duplicate exact shop
match fails before any rule write. A conflicting existing rule also fails and
rolls back the entire seed; the file never silently overwrites rules. It does
not call Xianyu, XianGuanJia, or any other third-party write API.

The production-path seed and this development copy are byte-identical. Their
current SHA-256 is
`26f9bfccb8bc3c50b09f489b3837be90bca957c3ab3823522870329a054cbc46`.

## Validation

- Confirm `xianyu_shop.xianyu_user_name` is populated for every enabled shop.
- Confirm channel orders with valid `goods_json` have separate `xgj_product_id`,
  `xianyu_item_id`, and `xgj_sku_id` values where those source fields exist.
- Count rows where only legacy `external_product_id` or `external_sku_id` is present;
  these remain unresolved and require detail resynchronization rather than guessing.
- Confirm no duplicate active rule exists for the same tenant, shop, and Xianyu item.
- Confirm `rental_device_category.lock_version` and
  `rental_device_model.lock_version` are `NOT NULL` integers with default `0`.
- Confirm `rental_order.expected_send_back_date`, `rental_order.settled_at`,
  `rental_order_item.expected_send_back_date`,
  `rental_device_assignment.inspection_completed_at`, and
  `rental_device_assignment.inspection_result` are nullable and existing rows
  remain null until an authoritative workflow records those facts.
- Confirm `rental_channel_reconciliation_run` stores tenant-scoped rule-change
  runs with lifecycle status, all eight result counters, timestamps and error code.
- Run `./verify-20260831_052-disposable-mysql.sh` with Docker Desktop available.
  The script uses the local `mysql:8.4` image with no network or published port,
  executes the forward migration and destructive rollback against a temporary
  volume, and removes both the container and volume on exit.
- Run `./verify-20260831_053-disposable-mysql.sh` with Docker Desktop available.
  It verifies the 053 DDL, optimistic-lock stale-update rejection on both
  catalog tables, all controlled-seed success and rejection cases, DDL-only
  rollback, retained rule data, and container/volume cleanup.
- Run `./verify-20260831_054-disposable-mysql.sh` with Docker Desktop available.
  It verifies the five additive fulfillment columns, existing-row null defaults,
  value round trips, destructive rollback and base-row retention. It then
  recreates the fixture and exposes MySQL only on a random loopback port while
  the Java assign-vs-reconcile two-thread lock-order test runs. The temporary
  container and volume are removed on exit.
- Run `./verify-20260901_055-disposable-mysql.sh` with Docker Desktop available.
  It verifies the two historical-reconciliation tables, five operational
  indexes, checkpoint and failure-boundary value round trips, destructive
  rollback and container/volume cleanup. It publishes MySQL only on a random
  loopback port while `RentalHistoricalOrderBackfillMysqlIntegrationTest`
  exercises the real Spring transaction, Service and MyBatis Mapper path for
  normal creation, `CONFIG_SKIPPED`, fulfilled-conflict preservation and an
  idempotent second run; the temporary port, container and volume are removed
  on exit.
- Run `RentalChannelReconciliationRunMigrationTest` through the focused Maven
  suite. It verifies that the 056 production SQL and review copy are
  byte-identical and that the tenant, scope, status, counters, lifecycle fields
  and operational indexes are all declared.
- Run the migration fixture test and full rental module test commands recorded
  in `manifest.json`.
- The full module command targets
  `yudao-module-rental/yudao-module-rental-biz`; targeting only the
  `yudao-module-rental` aggregator does not execute the biz tests.
- In offline mode, install the reactor dependencies into
  `/Volumes/zwl/maven-repository` with the recorded `-DskipTests install`
  command before running the standalone biz-module suite.

Recommended read-only verification:

```sql
SELECT COUNT(*) AS enabled_shops_missing_user_name
FROM xianyu_shop
WHERE deleted = b'0'
  AND authorization_status = 'VALID'
  AND (xianyu_user_name IS NULL OR xianyu_user_name = '');

SELECT COUNT(*) AS unresolved_order_identifiers
FROM xianyu_order
WHERE deleted = b'0'
  AND (xgj_product_id IS NULL OR xianyu_item_id IS NULL);

SELECT tenant_id, shop_id, xianyu_item_id, COUNT(*) AS duplicate_count
FROM rental_channel_product_rule
WHERE deleted = b'0'
GROUP BY tenant_id, shop_id, xianyu_item_id
HAVING COUNT(*) > 1;

SELECT status, COUNT(*) AS run_count
FROM rental_channel_reconciliation_run
WHERE deleted = b'0'
GROUP BY status;

SELECT status, COUNT(*) AS run_count
FROM rental_historical_reconciliation_run
WHERE deleted = b'0'
GROUP BY status;
```

Stop the rollout if identifier fallback, duplicate internal orders, duplicate
active rules, protected fulfillment mutation, advancing past a failed backfill
boundary, stalled leases/heartbeats, or ordinary-log privacy leakage is
observed. Do not treat local tests or a successful migration as authorization
for the controlled seed, real historical reconciliation, 80-server access, or
third-party writes.

## Rollback

- Prefer rolling back the application while retaining additive tables and columns.
- The supplied rollback SQL is destructive and is limited to disposable or
  pre-production schemas.
- A production schema rollback requires an export of product rules, SKU mappings,
  remark history, and explicit identifiers plus separate approval.
- Migration 052 and its complete rollback passed against disposable MySQL
  8.4.10. This does not authorize running either SQL file against production or
  the 80 server.
- The 053 rollback removes only the two `lock_version` columns. It does not and
  must not imply that seeded or manually configured business rules are safe to
  delete; retaining or removing those rules requires a separate data decision.
- The 054 rollback removes only the five fulfillment-fact columns introduced by
  that migration, but doing so permanently discards any values written to them.
  Production rollback therefore requires a backup, data-retention review, and
  separate approval.
- The 055 rollback deletes historical run and failure evidence. Prefer an
  application rollback that retains both tables. Production table removal
  requires an export, data-retention review and separate approval.
- The 056 rollback deletes asynchronous rule-reconciliation result history.
  Prefer an application rollback that retains the run ledger. Production table
  removal requires an export, data-retention review and separate approval.
