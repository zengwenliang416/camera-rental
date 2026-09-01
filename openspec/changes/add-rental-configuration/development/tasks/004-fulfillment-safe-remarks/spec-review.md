# Spec Review: 004-fulfillment-safe-remarks

## Verdict

approved

## Missing Requirements

- None within Task 004. Every non-skipped current parse attempt is persisted as
  remark history, while the channel order keeps its previous effective dates
  until reconciliation accepts the candidate
  (`XianyuOrderPersistenceServiceImpl.java:85-108,199-206,325-352`;
  `XianyuOrderRemarkHistoryService.java:21-62`).
- The classifier covers extension, early return, reschedule, replacement,
  damage, loss, overdue and logistics delay, and rejects invalid,
  contradictory or multiple special-case suffixes as invalid or ambiguous
  (`RentalRemarkPlanChangeClassifier.java:17-100`).
- One fulfillment guard owns both plan and model changes. It handles
  unassigned, assigned, dispatched, returned/inspected, canceled and settled
  states, and it uses the same transaction as reconciliation
  (`RentalChannelOrderReconciliationService.java:105-190`;
  `RentalFulfillmentUpdateGuard.java:45-143,232-301`).
- Assigned and dispatched schedule changes lock existing schedules, check
  effective overlaps before mutation and preserve the old schedule on conflict
  (`RentalFulfillmentUpdateGuard.java:145-185`).

## Extra Behavior

- Migration 054 adds explicit expected-send-back, inspection-completion,
  inspection-result and settlement fields. This is consistent with the
  requirement to separate mutable plans from physical and financial facts and
  does not perform a settlement or third-party write
  (`20260831_054_rental_fulfillment_facts.sql:1-18`).
- Assignment and reconciliation now share the order/item/device/schedule lock
  order. The additional concurrency hardening is directly related to the
  requirement that schedule checks and updates be transactional.

## Misunderstood Requirements

- None. Early-return remarks update only the expected send-back date and do not
  shorten the occupied schedule (`RentalFulfillmentUpdateGuard.java:94-99,
  131-135,225-230`).
- Replacement, damage, loss, overdue and logistics-delay remarks return an
  explicit review reason before any plan, model, assignment or schedule
  mutation (`RentalFulfillmentUpdateGuard.java:86-93,123-130,304-311`).
- A replacement remark does not implement a physical swap. That is consistent
  with the task's explicit non-goal; the existing assignment device is never
  overwritten by this reconciliation path.

## Cannot Verify From Diff

- Production migration execution, final cross-repository E2E verification and
  an operational replacement command are outside Task 004 and were not
  performed.
- The task does not add a financial-settlement writer. It does add and enforce
  the immutable `settled_at` boundary, and the reviewed reconciliation path
  never writes refund or settlement values.
- These are non-blocking scope boundaries rather than missing Task 004
  behavior. The server-side A6/A7 invariants are directly verifiable from the
  implementation, tests and system-executed evidence.

## Acceptance Assertions Verified

- A6 - Verified. Later valid candidates are reconciled against the same linked
  internal order and item; unassigned orders may receive the resolved model and
  plan, while invalid candidates do not clear existing effective dates
  (`RentalChannelOrderReconciliationService.java:124-190`;
  `RentalFulfillmentUpdateGuard.java:116-143`;
  `XianyuOrderPersistenceServiceImpl.java:98-108,140-145,199-206,331-352`).
  `XianyuOrderPersistenceServiceImplTest` covers invalid-plan retention,
  successful promotion and review-required non-promotion
  (`:185-315`), while `RentalOrderPreparationPolicyTest` proves a failed latest
  parse does not demote a complete effective internal plan (`:45-64`).
- A7 - Verified. Settled and canceled orders fail closed before assignment
  mutation; returned/inspected and mixed lifecycle histories require review;
  assigned model changes must match the configured model, item and every
  physical device; dispatched extensions require overlap-free locked
  schedules; early return cannot release occupancy; and operational suffixes
  create review only (`RentalFulfillmentUpdateGuard.java:45-113,145-185,
  232-311`). `RentalFulfillmentUpdateGuardTest` exercises these lifecycle,
  conflict, immutable-fact and stable-lock-order cases (`:73-333`).
  `RentalDeviceOpsService` records return and inspection only through the
  authoritative warehouse operation (`RentalDeviceOpsService.java:97-131`).

## Required Fixes

- None for Task 004 development handoff.

## Validation Performed

- Independently reran the focused Task 004 Maven suite: 129 tests, 0 failures,
  0 errors, 0 skipped.
- Verified system-executed evidence for the full rental-biz suite: 631 tests,
  0 failures, 0 errors and 7 environment-gated skips. The skipped fulfillment
  lock-order test passed separately in the disposable MySQL 8.4 fixture.
- Verified the recorded disposable MySQL 8.4 run covered migration forward
  application, nullable defaults, value round trips, rollback, base-row
  retention, cleanup and a real two-thread assign-vs-reconcile lock-order test
  (`development/validation-log.jsonl:43-45`).
