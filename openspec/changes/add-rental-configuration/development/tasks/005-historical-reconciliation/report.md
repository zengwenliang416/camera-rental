# Task Report: 005-historical-reconciliation

## Status

DONE

## Files Changed

- `camera-rental-server/sql/mysql/migrations/20260901_055_rental_historical_reconciliation.sql`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/rental/configuration/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/dataobject/rental/RentalHistoricalReconciliation*.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/rental/RentalHistoricalReconciliation*.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/xianyu/XianyuOrderMapper.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/reconciliation/RentalHistorical*.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/{controller,integration,service}/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/resources/sql/rental_historical_reconciliation.sql`
- `docs/integrations/xianyu/historical-reconciliation.md`
- `docs/integrations/xianyu/source.md`
- `docs/domain/xianyu-integration.md`
- `openspec/changes/add-rental-configuration/development/migrations/{20260901_055_*,rollback-20260901_055_*,verify-20260901_055-disposable-mysql.sh,manifest.json,README.md}`

## What Changed

- Added durable run and failure records for historical reconciliation, including
  fixed start/end boundaries, checkpoint cursor, batch size, lifecycle status,
  resume count, safe failure boundary, all required counters, a UUID execution
  token, heartbeat and five-minute execution lease.
- Task creation now resolves the current tenant once and freezes
  `endIdInclusive` to the smaller of the requested end and the tenant's current
  maximum `xianyu_order.id`. Other tenants and orders inserted later with larger
  IDs cannot expand the task. Empty current-tenant ranges normalize to
  `startAfterId` and complete with zero counts.
- Candidate paging is ascending by the internal `xianyu_order.id` and always
  constrained by tenant, durable cursor and frozen inclusive end.
- Each real batch executes reconciliation and checkpoint/counter persistence in
  one transaction. A record failure rolls back the whole current batch, leaves
  the durable cursor before that batch and writes a separate failure record
  containing only task/order IDs, attempt, boundary and a safe exception class.
- Dry-run executes the real centralized reconciliation path, rolls back all
  business writes in the batch transaction, then persists only the task cursor
  and counters in a separate transaction.
- Pause, pause-request convergence, batch-limit pause, resume and terminal
  success are explicit states. Active `RUNNING` and `PAUSE_REQUESTED` leases
  cannot be claimed twice; an expired execution can be explicitly resumed with
  a new fencing token from the last committed cursor, while a succeeded task
  cannot be resumed.
- A pause arriving in the dry-run rollback/checkpoint gap persists that batch's
  dry-run cursor and counters, converges to `PAUSED` and returns without starting
  another batch.
- Classification reports skipped, created, updated, unchanged, conflict,
  failed and review-required counts. Mutation counts are returned by the
  centralized reconciliation while it owns the order lock instead of being
  inferred from an unlocked snapshot. Existing internal orders and fulfilled
  conflicts are delegated to the same fulfillment-safe reconciliation guard and
  are never deleted or reversed by the backfill coordinator.
- New `rental_order`, `rental_order_item` and `rental_manual_review` records
  explicitly inherit the source channel order's `tenant_id`; this closes a
  MySQL-only defect where database defaults could otherwise assign tenant `0`.
- Added configuration-admin endpoints for create/run, query, pause and resume.
  Query uses `rental:configuration:query`; all state-changing operations use
  `rental:configuration:update`.
- Real execution is disabled by default with
  `rental.historical-backfill.write-enabled=false` and additionally requires
  `EXECUTE_HISTORICAL_RECONCILIATION`. Dry-run does not require this write
  authorization.
- Added an operations runbook covering frozen boundaries, dry-run, counters,
  monitoring, pause/resume, failure recovery, migration ordering and rollback
  restrictions.

## TDD Evidence

- `RentalHistoricalOrderBackfillServiceIntegrationTest` executes 14
  Spring/MyBatis/H2 cases for dry-run rollback, tenant-scoped upper-bound
  freezing, exclusion of later inserts, empty-range completion, per-batch
  rollback, durable resume, batch-limit pause, fulfilled conflict reporting,
  terminal-state rejection, real-run confirmation, infrastructure failure
  recovery, stale lease takeover, active lease rejection and the dry-run pause
  boundary.
- The failure test proves the first partially processed row is rolled back with
  the rest of the failed batch, `cursorAfterId` remains at the previous durable
  checkpoint, the failure row records the exact failed order and retrying the
  same task creates all three internal-order outcomes once.
- The frozen-range test requests an end of `10000`, has current-tenant maximum
  ID `2` and another tenant's ID `9999`, then inserts current-tenant ID `3`
  after pausing. The persisted end remains `2`, and resume scans only IDs `1`
  and `2`.
- `RentalConfigurationControllerTest` proves all historical endpoints stay
  under the standalone configuration route and use the approved query/update
  permissions.
- `RentalHistoricalReconciliationMigrationTest` verifies the two durable tables,
  required checkpoint/counter columns, operational indexes, byte-identical
  production/development SQL and a rollback limited to the 055 tables.
- `RentalChannelOrderReconciliationServiceTest` executes 19 cases, including
  lock-consistent `CREATED`/`UPDATED`/`UNCHANGED` results and tenant propagation
  to newly inserted orders, items and manual reviews.
- Focused reconciliation/backfill regression: 33 tests, 0 failures, 0 errors,
  0 skipped.
- Complete Task 005 focused suite including migration and controller coverage:
  38 tests, 0 failures, 0 errors, 0 skipped.
- `RentalHistoricalOrderBackfillMysqlIntegrationTest` passed separately on the
  disposable MySQL fixture: 1 test, 0 failures, 0 errors, 0 skipped. It uses the
  real Spring transaction, reconciliation Service and MyBatis Mappers for
  ordinary creation, exact `CONFIG_SKIPPED`, fulfilled-conflict preservation
  and a second idempotent run without duplicate internal orders.
- Full rental-biz regression after installing the current reactor artifacts:
  651 tests, 0 failures, 0 errors, 8 skipped. The skips are environment-gated
  MySQL tests; the Task 004 fulfillment test and Task 005 historical
  reconciliation test passed separately through their disposable fixtures.
- Disposable MySQL 8.4 verification passed table/index creation, run checkpoint
  round trip, failure-boundary round trip, five indexes, real Service/Mapper
  execution, 055 rollback and cleanup. Output ended with
  `HISTORICAL_RECONCILIATION_REAL_SERVICE_MYSQL_PASS` and
  `DISPOSABLE_MYSQL_055_PASS`.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalHistoricalOrderBackfillServiceIntegrationTest,RentalChannelOrderReconciliationServiceTest,RentalHistoricalReconciliationMigrationTest,RentalConfigurationControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests install`
- `cd camera-rental-server/yudao-module-rental/yudao-module-rental-biz && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository test`
- `openspec/changes/add-rental-configuration/development/migrations/verify-20260901_055-disposable-mysql.sh`
- `cmp -s` and `shasum -a 256` for the production/development 055 migration,
  rollback and disposable verification script
- `git diff --check`
- JSON/JSONL parsing for the complete change directory
- `sh -n` for every development migration verification script
- `openspec validate add-rental-configuration --strict`

## Concerns

- Migration 055 has not been applied to production or the 80 server. The
  rollback drops run and failure evidence and therefore requires export,
  retention review, backup and separate approval in any persistent environment.
- No production dry-run or real historical reconciliation was executed. Local
  H2 tests use synthetic records, and the disposable MySQL test uses synthetic
  records through the real Service/Mapper path; current production eligibility
  counts remain unknown until an explicitly authorized dry-run.
- Real execution must keep the write flag disabled except during an approved
  change window, begin with one small batch and restore the flag to `false`
  after completion or pause.
- The first direct `rental-biz` full-suite attempt loaded a stale locally
  installed `rental-api` and failed with `NoSuchFieldError`. Installing the
  current reactor artifacts resolved the environment mismatch; the repeated
  full suite then passed 651 tests.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Task 006 must add the typed admin client and visible run/dry-run monitoring
  experience to the standalone Rental Configuration page.
- Task 007 must complete cross-repository documentation, final Verification
  evidence and deployment planning.
- Production migration, dry-run, write-flag change and real backfill require
  separate explicit authorization and current database-safe evidence.

## Adjudication

The implementation satisfies checklist items 5.1-5.3 and supplies the
server-side unit evidence for A8. It provides bounded, tenant-isolated,
resumable and dry-run-capable historical reconciliation without a second
conversion path or any reversal/delete behavior. It does not claim production
execution, production counts, deployment or third-party writes.
