# Quality Review: 002-transactional-dispatch-backfill

## Verdict

needs-fix

## Separation Of Concerns

- The controller remains a thin permission-gated entry point
  (`XianyuOrderController.java:88-95`); aggregate coordination is kept in
  `XianyuOrderShipService.backfillDispatch` rather than duplicating assignment
  or scheduling rules in the controller.
- The service does not call `XianyuRuntimeConfigService` or `XianyuWriteClient`
  from the backfill branch (`XianyuOrderShipService.java:201-270`), preserving
  the required local-only side-effect boundary.

## Component Cohesion / Coupling

- Reuse of conversion, assignment, device dispatch, Shipment, and Delivery
  services is directionally correct. The new mapper helper also keeps the
  business-key lock at the persistence boundary.
- The aggregate method is still coupled to several mutable service/mapper
  contracts, but that is the approved domain boundary. The main quality issue
  is not extraction; it is that the orchestration does not fully enforce the
  approved eligibility/error semantics.

## Test Quality

- The focused Maven command passed with 28 tests and zero failures. The tests
  cover success, pending/closed/cancelled paths, tenant-shop rejection,
  same-waybill conflict, conversion, idempotent replay, dispatched reuse, and
  the no-remote-call interaction boundary.
- The rollback test
  (`XianyuOrderShipServiceTest.java:351-384`) only verifies Mockito calls and
  the presence of `@Transactional`; it does not run a transaction against a
  database or assert that assignment, schedule, device, Shipment, and order
  rows are absent after Delivery failure.
- The suite does not cover the documented `refundStatus` field, persistence
  failure after local mutations, non-shippable backfill states, the
  `ASSIGNED -> DISPATCHED` branch, or a conflicting normalized replay with
  changed reason/time/carrier name.

## Error Handling

- `requireBackfillEligible` only checks `orderStatus` `21/22`, statuses `23/24`,
  and `cancelTime` (`XianyuOrderShipService.java:334-341`). It never checks
  `XianyuOrderDO.refundStatus`; the documented status table defines
  `refundStatus=5` as “退款成功” (`docs/integrations/xianyu/order-sync.md:132-143`).
  A row still carrying order status `21` or `22` with a successful refund can
  therefore pass the backfill gate.
- `assignDevice` maps every assignment exception other than idempotency reuse
  to `XIANYU_SHIP_DEVICE_NOT_SHIPPABLE`
  (`XianyuOrderShipService.java:424-435`). Schedule conflicts, locked devices,
  model mismatches, and ineligible order items lose their specific conflict
  classification instead of returning the approved backfill conflict/error
  contract.
- The transaction annotation is appropriate, but the current evidence cannot
  prove rollback across the separately proxied assignment, dispatch, Delivery,
  and conversion services.

## Reuse / Duplication

- The implementation reuses the existing conversion, assignment, dispatch, and
  Delivery services and factors the shared Delivery command builder. No second
  inventory or scheduling implementation was introduced.
- The new backfill-specific state checks are kept in helpers, but their broad
  exception remapping is a semantic duplication of error handling rather than
  reuse of the underlying typed assignment failures.

## Complexity Delta

- The change adds roughly 195 production lines and 309 test lines to an already
  multi-purpose shipping service/test. Helpers keep the method readable and
  the delta remains bounded, but the added branch now carries conversion,
  locking, idempotency, logistics, and audit responsibilities that require
  stronger integration evidence before approval.

## Required Fixes

- Reject refunded orders using the persisted refund-state contract (at minimum
  the documented successful-refund state) and add focused tests for the
  relevant refund states before device lookup or mutation.
- Preserve typed assignment failures or map schedule/device/model conflicts to
  `XIANYU_DISPATCH_BACKFILL_CONFLICT` rather than reporting all of them as
  “device not shippable.”
- Add a real transactional test or equivalent database-backed evidence that
  Delivery/persistence failure rolls back every local mutation, including
  conversion, assignment, schedule, device, Shipment, and channel-order
  updates.
- Populate the task report and validation evidence with exact system-executed
  results; the passing Mockito suite alone is insufficient for approval.
