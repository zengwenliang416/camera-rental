# Spec Review: 002-transactional-dispatch-backfill

## Verdict

approved

## Missing Requirements

- No implementation requirement is missing from the scoped backend diff. The
  Controller, typed request, local-only service branch, tenant/shop check,
  status/refund gate, conversion, assignment, occupied scheduling, dispatch,
  Shipment, Delivery, logistics update, idempotency, and typed conflict paths
  are implemented.
- A database-backed rollback/concurrency oracle is not present in the Mockito
  suite. The task provides the required transactional boundary and failure
  propagation evidence, while the handoff explicitly assigns persisted
  rollback and concurrency execution to Verification 2.0.

## Extra Behavior

- No out-of-scope remote XianGuanJia write, write-enabled configuration read,
  migration, new permission, multi-device waybill behavior, or parallel
  inventory/scheduling implementation is introduced by this task.

## Misunderstood Requirements

- The current implementation rejects cancellation, closed statuses `23`/`24`,
  and persisted successful refund status `5` before device lookup or mutation.
- Business-key replay requires `ADMIN_BACKFILL`, the same device, matching
  request hash, and a linked Delivery. Idempotency-key mismatches are rejected
  before replacement-device lookup.
- The backfill method is separate from the formal `ship` method. The formal
  path may read runtime write configuration and call `XianyuWriteClient`; the
  backfill path does neither.

## Cannot Verify From Diff

- The signed Maven receipt `receipt-eb6606d9...a171` is
  `system-executed`, passes, and binds to the reviewed HEAD/tree. Its focused
  35-test result verifies the service interaction boundary.
- Mockito tests and the `@Transactional(rollbackFor = Exception.class)`
  annotation cannot prove persisted post-failure row state, database row-lock
  behavior, or concurrent allocation outcomes.
- Frontend E2E, sensory checks, and the admin toolchain are outside this
  backend task.

## Acceptance Assertions Verified

- A4: `POST /admin-api/rental/xianyu/order/dispatch-backfill` is protected by
  `rental:xianyu:ship`, and the request VO validates the typed fields plus
  `deviceId`/`deviceNo`.
- A5: pending, refunded, closed, and cancelled orders are rejected before
  device lookup or local mutation in the focused tests.
- A6: the service coordinates conversion, assignment, occupied schedule,
  dispatch, `ADMIN_BACKFILL` Shipment, outbound Delivery, and channel-order
  logistics in one annotated aggregate method.
- A7: the backfill path has zero `XianyuWriteClient` calls and zero runtime
  write-configuration reads in source and focused tests.
- A8: normalized request hashing, matching replay, changed-request rejection,
  and different-order idempotency reuse are covered.
- A9: same-order/same-waybill binding to another device is rejected as
  `XIANYU_DISPATCH_BACKFILL_CONFLICT`.
- A10: tenant-shop ownership and non-shippable-device checks precede local
  mutation.
- A11: Delivery/persistence failure propagation and the explicit rollback
  annotation are covered at the service boundary; persisted rollback remains a
  Verification 2.0 obligation.

## Required Fixes

- None for this development implementation slice. Before Verification 2.0
  promotion, run the database-backed rollback/concurrency and red-team cases
  without describing the current Mockito result as persisted-state proof.
