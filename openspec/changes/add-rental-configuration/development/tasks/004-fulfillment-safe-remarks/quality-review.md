# Quality Review: 004-fulfillment-safe-remarks

## Verdict

approved

## Separation Of Concerns

- Remark parsing/history, change classification, reconciliation orchestration, and
  fulfillment mutation are separated into focused services. In particular,
  `RentalFulfillmentUpdateGuard` owns lifecycle-sensitive model/date changes
  instead of duplicating those decisions in persistence or shipping code.
- The authoritative return operation remains in `RentalDeviceOpsService`, and
  remark reconciliation does not synthesize return, inspection, replacement, or
  settlement completion.

## Component Cohesion / Coupling

- `RentalFulfillmentUpdateGuard` is cohesive around one policy and uses explicit
  fail-closed reason codes for assignment, device, schedule, and lifecycle
  inconsistencies.
- The shared lock protocol is necessarily coupled across reconciliation,
  assignment, delivery, shipping, and device operations. The inspected paths use
  the compatible order/item -> device -> assignment -> schedule ordering, and the
  repaired MySQL integration test now exercises the real assignment and
  reconciliation services through their Spring transaction proxies and
  production Mappers.

## Test Quality

- The focused Task 004 command was independently rerun during this review and
  passed 129 tests with no failures, errors, or skips. Surefire confirms that
  `RentalFulfillmentUpdateGuardTest` contributes 19 executed cases and
  `RentalDeviceOpsServiceTest` contributes 9 executed cases.
- Unit coverage is strong for invalid-plan preservation, accepted-plan
  promotion, review-required non-promotion, assignment/model consistency,
  schedule conflicts, early-return occupancy preservation, returned/settled
  immutability, and deterministic multi-device lock ordering.
- The previous test-depth blocker is closed. The disposable MySQL 8.4 verification
  was independently rerun and executed
  `RentalFulfillmentLockOrderMysqlConcurrencyTest` with 1 test, 0 failures,
  0 errors, and 0 skips. The test asserts both services are AOP proxies, pauses
  the real `RentalScheduleMapper.insert(...)`, invokes the production
  `assign(...)` and `reconcile(...)` paths concurrently, and verifies the final
  assignment, schedule, order/item dates, and channel status.
- The previous timezone-coverage blocker is closed.
  `RentalDeviceOpsServiceTest` uses a fixed Shanghai clock while changing the JVM
  default timezone to `America/Los_Angeles`, then asserts exact Shanghai
  `returnedAt`, `inspectionCompletedAt`, and exclusive schedule-end values.

## Error Handling

- Unsafe states fail closed into stable manual-review reasons before order,
  item, or schedule writes. Schedule rows are all validated before the update
  loop, and the reconciliation/persistence paths share rollback-on-exception
  transactions.
- Review-required candidates do not advance the channel effective plan or mark
  the remark-history row effective. Early return changes only the expected
  send-back fields and leaves effective occupancy unchanged.
- The previous correctness blocker is closed. `RentalDeviceOpsService` now uses
  one `Clock.system(Asia/Shanghai)` source for the authoritative return timestamp,
  inspection-completion timestamp, and schedule-narrowing date. The package-level
  clock constructor makes this behavior deterministic without changing the
  production service boundary.

## Reuse / Duplication

- The implementation reuses one fulfillment guard for remark-driven plan
  changes and configuration-driven model changes, one classifier for all remark
  change types, and one history service for parse snapshots.
- Assignment readiness and device-lock checks reuse existing domain services;
  no parallel remark-specific assignment, return, inspection, or settlement
  path was introduced.

## Complexity Delta

- The complexity increase is justified by explicit lifecycle facts, history,
  stable locking, and conflict review. The guard is sizeable but its phases are
  linear and the mutation step occurs only after validation.
- Migration 054 is additive and its production/development copies match. Its
  rollback intentionally drops all five new columns, so any fulfillment facts
  written after deployment would be lost. This remains a deployment/rollback
  operational risk requiring backup and explicit approval, not a Task 004 code
  blocker.

## Required Fixes

- None. Both prior blocking findings were independently verified as resolved.

## Validation Performed

- `openspec/changes/add-rental-configuration/development/migrations/verify-20260831_054-disposable-mysql.sh`
  passed migration forward/rollback assertions and 4 Maven tests with no
  failures, errors, or skips, including the real-service MySQL concurrency test;
  output ended with `FULFILLMENT_LOCK_ORDER_MYSQL_PASS`.
- The temporary `codex-rental-mysql-054-89991` container and its volume were
  absent after the script exited.
- The focused Task 004 Maven suite passed 129 tests with no failures, errors, or
  skips.
- The existing system-executed validation record for
  `RentalDeviceOpsServiceTest` reports 9 tests passing with the fixed-clock,
  cross-JVM-timezone assertions.
