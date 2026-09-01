# Quality Review: 005-historical-reconciliation

## Verdict

approved

## Separation Of Concerns

- `RentalHistoricalOrderBackfillService` remains focused on historical-run
  coordination: durable state transitions, bounded paging, transaction
  boundaries, counters, pause/resume, fencing, and failure recording. Channel
  order conversion, configuration policy, model resolution, and
  fulfillment-safe mutation remain in
  `RentalChannelOrderReconciliationService` and its domain collaborators.
- Dry-run uses the same production reconciliation path inside a rollback-only
  business transaction, then persists only checkpoint and counter state in a
  separate transaction. No parallel conversion implementation was introduced.
- Run ownership helpers (`requireOwnedRunForUpdate`, `renewExecution`,
  `clearExecution`, `isCurrentExecution`, and `isActiveExecution`) make the
  state-machine invariants explicit rather than distributing token checks
  through unrelated business code.

## Component Cohesion / Coupling

- The previous unrecoverable-`RUNNING` blocker is closed. Every execution gets a
  UUID token and a five-minute lease; active `RUNNING` and `PAUSE_REQUESTED`
  records reject takeover, expired executions can be explicitly resumed, and
  all later checkpoint, pause, success, and failure writes re-lock the run and
  verify the current token.
- Fencing is transactionally sound for both modes. A real batch holds the run
  row lock while reconciliation and checkpoint writes commit together. A
  dry-run releases the rolled-back business transaction, then revalidates the
  token before its separate checkpoint; an old worker cannot overwrite a
  takeover.
- Stable paging remains tenant-scoped, ordered by ascending internal order ID,
  and bounded by the end ID frozen when the run is created. Execution ownership
  does not weaken the durable cursor contract.
- `RentalChannelOrderReconciliationService` now produces `mutationKind` while
  holding the channel-order lock. The backfill coordinator no longer depends on
  a separate unlocked pre-state query, closing the prior count-attribution
  race.

## Test Quality

- The independently rerun focused suite passed 38 tests with 0 failures,
  0 errors, and 0 skipped:
  `RentalHistoricalOrderBackfillServiceIntegrationTest` (14),
  `RentalChannelOrderReconciliationServiceTest` (19),
  `RentalHistoricalReconciliationMigrationTest` (3), and
  `RentalConfigurationControllerTest` (2).
- State-machine coverage now includes stale lease takeover, active lease
  rejection, candidate-query failure recovery, dry-run checkpoint failure
  recovery, the pause-between-rollback-and-checkpoint race, safe
  `ServiceException` codes, durable cursor resume, batch rollback, and terminal
  state rejection.
- Mutation classification tests prove `CREATED`, `UPDATED`, and `UNCHANGED`
  originate from the production reconciliation result. Tenant assertions cover
  new rental orders, order items, and manual-review records.
- The disposable MySQL 8.4 verifier executed
  `RentalHistoricalOrderBackfillMysqlIntegrationTest` through real Spring
  transactions, production Services, and MyBatis Mappers. Its one test passed
  with 0 failures, 0 errors, and 0 skipped and covered ordinary creation,
  exact configuration skip, returned-fulfillment protection, tenant-scoped
  inserts, and a second idempotent run without duplicate orders or items.

## Error Handling

- The previous peripheral-exception blocker is closed. Exceptions outside the
  per-order wrapper, including candidate selection and dry-run checkpoint
  persistence failures, are converted to durable `FAILED` state when the caller
  still owns the token. If ownership has changed, the old caller returns the
  current run without clobbering the new execution.
- A crashed process leaves a bounded lease instead of a permanently
  non-runnable status. After lease expiry, explicit resume installs a new token
  and restarts from the last committed cursor.
- The previous dry-run pause blocker is closed.
  `persistDryRunOutcome(...)` returns the persisted run, and the caller stops
  immediately when token ownership or active status is cleared. The
  deterministic concurrency test verifies only one order is reconciled before
  returning `PAUSED`.
- Failure persistence does not include exception messages, payloads, customer
  data, or credentials. `safeErrorCode(...)` extracts a bounded numeric
  `ServiceException` code as `SERVICE_<code>` and otherwise records only the
  root exception class.

## Reuse / Duplication

- Historical execution reuses the centralized reconciliation service,
  tenant-aware Mappers, fulfillment guard, preparation policy, transaction
  manager, and shared reconciliation result. No duplicate mapping, remark,
  order-creation, or fulfillment-update path was found.
- The same lock-derived `mutationKind` supports live callers and historical
  counters. Compatibility constructors retain existing call sites without
  duplicating classification in the backfill service.
- Production and development migration 055 copies are byte-identical. The
  disposable verifier applies the production SQL and the real application
  service path rather than substituting test-only migration behavior.

## Complexity Delta

- Execution leasing and fencing necessarily increase the coordinator's size,
  but the complexity is linear and localized around explicit transition
  helpers. The critical transitions are covered by deterministic tests and
  protected by row locks plus token comparison.
- The five-minute lease and heartbeat use the application clock and persist at
  transaction/batch boundaries. This is acceptable with the existing run-row
  lock protocol, but production nodes must keep clocks synchronized and
  operators should choose batch sizes that normally finish within the lease
  window.
- Migration 055 is additive and indexed for tenant/status/lease and range
  inspection. Its rollback deletes run and failure evidence; the runbook
  correctly treats that as a data-retention and deployment-approval risk rather
  than an application-code rollback mechanism.

## Required Fixes

- None. The previous execution-recovery, dry-run pause, count consistency,
  diagnostic-code, tenant propagation, and real-service test-depth findings are
  independently verified as resolved.

## Validation Performed

- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o
  -Dmaven.repo.local=/Volumes/zwl/maven-repository
  -pl yudao-module-rental/yudao-module-rental-biz -am
  -Dtest=RentalHistoricalOrderBackfillServiceIntegrationTest,RentalChannelOrderReconciliationServiceTest,RentalHistoricalReconciliationMigrationTest,RentalConfigurationControllerTest
  -Dsurefire.failIfNoSpecifiedTests=false test` passed 38 tests with no
  failures, errors, or skips.
- `openspec/changes/add-rental-configuration/development/migrations/verify-20260901_055-disposable-mysql.sh`
  passed five-index and lease round trips, failure-boundary persistence,
  rollback, and the real Service/Mapper MySQL test; output ended with
  `HISTORICAL_RECONCILIATION_REAL_SERVICE_MYSQL_PASS` and
  `DISPOSABLE_MYSQL_055_PASS`.
- Migration 055 production/development copies compare byte-identical, and the
  verifier passes `sh -n`.
- The temporary `codex-rental-mysql-055-35137` container and its volume were
  absent after verifier cleanup.
