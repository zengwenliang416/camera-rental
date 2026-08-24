# Spec Review: 004-regression-evidence

## Verdict

approved

## Missing Requirements

- No development evidence requirement is missing from the scoped regression
  task. The focused service class contains the requested eligibility, refund,
  tenant, device, business-key, conversion, idempotency, persistence-failure,
  Delivery-failure, assignment-reuse, and no-remote-write cases.
- Persisted database rollback/concurrency, red-team, browser E2E, and sensory
  execution are not provided by this task's unit/static commands and are
  explicitly handed to Verification 2.0.

## Extra Behavior

- The regression diff remains bounded to the focused service tests and declared
  development evidence. It adds no production abstraction, external
  credential, remote write, migration, or unrelated formatting.

## Misunderstood Requirements

- The earlier 28-test self-reported records are historical log entries. The
  authoritative current evidence is the appended signed receipt set bound to
  HEAD `1ac3c96ecedaaff8671694d7ff8681c7c6e9911e` and tree
  `46d8ae8c9ed6e6c8d78844c5d45fddb60f3d8455`; it records 35 passing backend
  tests and current admin checks.
- The 35-test interaction suite and `@Transactional` reflection assertion are
  not persisted rollback or concurrency proof. The handoff does not claim that
  they are.

## Cannot Verify From Diff

- Signed receipts `receipt-9a25bf1b...5d17`,
  `receipt-b756e249...aee5`, `receipt-28c4100d...7550`,
  `receipt-a70a0b20...1330`, and `receipt-5a0ab8c9...9ab8` are
  current-head `system-executed` passes for Maven, Vue type checking,
  targeted ESLint, targeted Prettier, and `git diff --check`.
- The current evidence does not observe persisted post-failure database state,
  concurrent device allocation, red-team execution, production E2E, or sensory
  states. Those are verification-surface limitations, not an unimplemented
  development test command.

## Acceptance Assertions Verified

- A5: focused tests reject pending/refunded/closed/cancelled and non-shippable
  paths before local mutation or remote calls.
- A6: the success and conversion tests cover the local aggregate wiring at the
  service boundary.
- A7: focused tests assert zero `XianyuWriteClient` calls and zero write-config
  reads on backfill paths.
- A8: matching replay and conflicting idempotency requests are covered.
- A9: same-waybill/different-device business-key conflict is covered.
- A10: cross-tenant shop and non-shippable-device rejection paths are covered.
- A11: Delivery/persistence failure propagation and transaction metadata are
  covered; persisted rollback remains unverified.
- A12: current backend Maven, admin type/lint/format, and diff checks all have
  passing signed receipts.

## Required Fixes

- None for this development regression slice. Preserve the signed receipts as
  Verification 2.0 inputs and run the database rollback/concurrency, red-team,
  E2E, and sensory cases there.
