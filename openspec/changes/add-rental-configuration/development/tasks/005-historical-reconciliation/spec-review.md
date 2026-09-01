# Spec Review: 005-historical-reconciliation

## Verdict

approved

## Missing Requirements

- None within Task 005. Run records persist the tenant-scoped fixed range,
  durable cursor, bounded batch size, resume count, execution lease and every
  required outcome counter
  (`RentalHistoricalOrderBackfillService.java:121-146,331-360`;
  `RentalHistoricalReconciliationRunDO.java:22-55`;
  `20260901_055_rental_historical_reconciliation.sql:4-38`).
- Candidate paging applies `tenant_id`, `id > cursor`, `id <= fixed end`,
  ascending primary-key order and a bounded limit
  (`XianyuOrderMapper.java:244-266`). Task creation freezes the requested upper
  bound against the current tenant maximum and normalizes an empty range to
  `startAfterId` (`RentalHistoricalOrderBackfillService.java:331-360`).
- Each real business batch and its checkpoint commit in one transaction. A
  failed record rolls back the whole batch, persists a safe failure boundary
  separately and leaves the durable cursor before the failed batch
  (`RentalHistoricalOrderBackfillService.java:148-159,193-247,284-317`).
- Dry-run invokes the same locked reconciliation path, rolls back its business
  transaction, then persists only the returned counters and cursor in a
  separate transaction. A pause arriving between rollback and checkpoint saves
  that batch before converging to `PAUSED`
  (`RentalHistoricalOrderBackfillService.java:164-178,193-262`).
- Counts use the centralized reconciliation result's lock-consistent
  `mutationKind`, rather than a lock-external before/after snapshot
  (`RentalChannelOrderReconciliationResult.java:3-55`;
  `RentalHistoricalOrderBackfillService.java:227-246,459-470`).
- The management API exposes run, get, pause and resume operations with
  configuration query/update permissions, bounded validated inputs, a
  default-disabled write switch and an explicit confirmation string for real
  execution (`RentalConfigurationController.java:174-216`;
  `RentalHistoricalBackfillCreateReqVO.java:15-49`;
  `RentalHistoricalOrderBackfillService.java:35-58,428-457`).
- The operational runbook documents deployment order, stable range selection,
  dry-run, monitoring, lease-safe pause/resume, one-batch production rollout,
  failure recovery and evidence-preserving rollback
  (`docs/integrations/xianyu/historical-reconciliation.md:17-180`).

## Extra Behavior

- Migration 055 adds dedicated run and failure tables plus five secondary
  operational indexes. This is directly related to durable checkpointing,
  lease takeover and observability and does not mutate historical order or
  fulfillment data
  (`20260901_055_rental_historical_reconciliation.sql:1-57`).
- Execution adds UUID fencing tokens, a five-minute renewable lease and
  explicit stale-run takeover. Active `RUNNING` or `PAUSE_REQUESTED` executions
  reject takeover, while expired executions can resume from the last committed
  cursor with a new token
  (`RentalHistoricalOrderBackfillService.java:121-146,374-426`).
- Real execution has two deliberate safety gates beyond the minimum task text:
  `rental.historical-backfill.write-enabled=false` by default and the fixed
  `EXECUTE_HISTORICAL_RECONCILIATION` confirmation string. Both are consistent
  with the task's production-mutation stop condition.

## Misunderstood Requirements

- None. The batch orchestrator delegates every selected record to the existing
  `RentalChannelOrderReconciliationService`; it does not introduce a competing
  mapping, remark or order-creation path
  (`RentalHistoricalOrderBackfillService.java:215-232`).
- `CONFIG_SKIPPED` is applied only when the exact enabled rule matches and no
  internal rental order exists. Existing internal orders continue through the
  non-destructive reconciliation path instead of being cleared or reversed
  (`RentalChannelOrderReconciliationService.java:123-171,457-460`).
- A fulfilled conflict is classified as both conflict and review-required,
  while the existing linked order and conversion state remain intact
  (`RentalChannelOrderReconciliationResult.java:42-55`;
  `RentalHistoricalOrderBackfillService.java:459-470`;
  `RentalHistoricalOrderBackfillServiceIntegrationTest.java:208-227`).
- New internal orders, items and manual reviews explicitly inherit the source
  channel order tenant, so MySQL defaults cannot silently place Task 005 writes
  in tenant `0`
  (`RentalChannelOrderReconciliationService.java:267-297,406-441`).

## Cannot Verify From Diff

- No production database, 80-server service or real historical run was used.
  Production execution remains correctly subject to separate authorization,
  backup and dry-run review.
- Production eligibility counts, operational duration and lock pressure remain
  unknown until an explicitly authorized production dry-run.
- Task 005 quality review, checklist/ledger state and Tasks 006-007 are separate
  change-level handoff inputs. Their status does not invalidate this Task 005
  spec verdict, but may still block the overall change handoff.

## Acceptance Assertions Verified

- A8 - Verified. Historical reconciliation is bounded, tenant-isolated,
  idempotent and resumable; it creates missing normal rental orders, applies
  `CONFIG_SKIPPED` only before an internal order exists, reports protected
  fulfillment as conflict/review, and never deletes history
  (`RentalHistoricalOrderBackfillService.java:121-360`;
  `RentalChannelOrderReconciliationService.java:116-197`;
  `XianyuOrderMapper.java:244-266`).
- A8 - Fourteen backfill integration cases cover dry-run rollback, fixed tenant
  upper bounds, exclusion of later and other-tenant rows, empty ranges,
  failed-batch rollback/resume, batch-limit pause/resume, protected conflicts,
  terminal-state rejection, confirmation gating, stale lease takeover, active
  lease rejection, candidate-query recovery, the dry-run pause/checkpoint race,
  checkpoint failure recovery and safe domain error codes
  (`RentalHistoricalOrderBackfillServiceIntegrationTest.java:87-428`).
- A8 - Nineteen centralized reconciliation cases cover normal creation,
  lock-consistent mutation classification, exact skip behavior, preservation
  of existing orders, source-tenant propagation and exactly-once retry
  (`RentalChannelOrderReconciliationServiceTest.java:90-491`).
- A8 - The disposable MySQL integration exercises the real Spring transaction,
  reconciliation Service and MyBatis Mapper path for normal creation, exact
  skip, returned-fulfillment preservation, tenant `9` persistence and an
  idempotent second run without duplicate orders or items
  (`RentalHistoricalOrderBackfillMysqlIntegrationTest.java:26-165`).

## Required Fixes

- None for Task 005 development handoff.

## Validation Performed

- Independently ran the focused Maven command for
  `RentalHistoricalOrderBackfillServiceIntegrationTest`,
  `RentalChannelOrderReconciliationServiceTest`,
  `RentalHistoricalReconciliationMigrationTest` and
  `RentalConfigurationControllerTest`: 38 tests, 0 failures, 0 errors,
  0 skipped, `BUILD SUCCESS`.
- Independently ran
  `verify-20260901_055-disposable-mysql.sh` against disposable MySQL 8.4.
  Table/index creation, checkpoint, lease and failure-boundary round trips,
  destructive 055 rollback, and the real Service/Mapper MySQL integration all
  passed. The MySQL integration result was 1 test, 0 failures, 0 errors,
  0 skipped; the script exited successfully with
  `HISTORICAL_RECONCILIATION_REAL_SERVICE_MYSQL_PASS` and
  `DISPOSABLE_MYSQL_055_PASS`.
- Independently verified with `cmp -s` that the production and development 055
  forward migrations are byte-identical.
