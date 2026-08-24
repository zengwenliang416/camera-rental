# Spec Review: 004-regression-evidence

## Verdict

needs-fix

## Missing Requirements

- The regression suite does not exercise the persisted successful-refund
  (`refundStatus`) rejection, non-shippable/locked/busy device states,
  persistence failure after local writes, or a database-backed rollback oracle.
  The test named `backfillRejectsRefundedClosedOrCancelledOrder` covers order
  statuses `23`/`24` and cancellation, not the documented refund field
  (`XianyuOrderShipServiceTest.java:247-270`).
- The Delivery rollback test verifies exception propagation, mock call order,
  and the `@Transactional` annotation only; it does not verify that assignment,
  schedule, device, Shipment, Delivery, conversion, and channel-order rows are
  absent or restored after rollback (`XianyuOrderShipServiceTest.java:352-384`).
- The task requires exact system-executed validation evidence, but the current
  `validation-log.jsonl` entries are marked `attestation: self-reported`.
  The report's claim that all admin checks passed is not supported by a current
  authoritative receipt.

## Extra Behavior

- The regression changes remain test/evidence scoped and do not add a parallel
  production abstraction or external write. No unrelated runtime behavior is
  attributable to this task.

## Misunderstood Requirements

- A passing Mockito class and a reflection check for `@Transactional` are not
  equivalent to proving transaction rollback or database concurrency behavior.
- Recording a command as passed in a self-reported log is not the same as an
  exact system-executed receipt from the managed development runner.

## Cannot Verify From Diff

- The backend focused Maven command is reproducibly reported as
  `BUILD SUCCESS` with 28 tests, 0 failures, 0 errors, and 0 skipped. That is
  useful unit evidence, but it does not establish A6/A10/A11 at the database
  or runtime boundary.
- `git diff --check` passed, but the admin type/lint/format results cannot be
  accepted as current green evidence: the attempted `pnpm` setup was blocked
  by the repository's `npmmirror` tarball supply-chain policy, and the local
  `vue-tsc`, ESLint, and Prettier binaries were unavailable afterward.
- No authoritative red-team, browser, sensory, or managed SpecNav receipt
  exists for the required final baseline.

## Acceptance Assertions Verified

- A4 and A7 have partial support from the backend source and the 28-test
  Mockito run; A8 and A9 have focused interaction coverage for replay/conflict
  cases.
- A5, A6, A10, and A11 are not fully verified because refund-state,
  non-shippable, persistence-failure, and real rollback/red-team evidence are
  incomplete.
- A12 is only partially verified: backend compilation/tests and
  `git diff --check` are supported, while the admin checks lack a trusted
  receipt.

## Required Fixes

- Add focused refund-state, non-shippable, persistence-failure, and
  database-backed rollback/concurrency tests; assert post-failure persisted
  state, not only Mockito interactions.
- Re-run the declared backend/admin checks after final edits through the
  approved managed evidence path and replace self-reported entries with exact
  system-executed receipts.
- Reconcile the task report with the actual checks and keep unverified
  assertions explicitly open for Verification 2.0.
