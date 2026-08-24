# Spec Review: 002-transactional-dispatch-backfill

## Verdict

needs-fix

## Missing Requirements

- `requireBackfillEligible` checks cancellation and order statuses `21`/`22`
  versus `23`/`24`, but does not inspect the persisted refund state
  (`XianyuOrderDO.refundStatus`). A successfully refunded order can therefore
  still pass the backfill gate when its order status remains `21` or `22`
  (`XianyuOrderShipService.java:334-340`).
- The same-order/waybill/carrier lookup returns any same-device Shipment as a
  replay without checking that it is an `ADMIN_BACKFILL` row, has the current
  normalized request hash, or has the required Delivery
  (`XianyuOrderShipService.java:227-234`). A historical formal-shipment row can
  consequently bypass the backfill contract and return a non-backfill result.
- An existing idempotency key is checked for order/waybill/carrier before the
  request device is resolved, but a missing or changed device can fail as
  `RENTAL_DEVICE_NOT_EXISTS` instead of the required
  `XIANYU_SHIP_IDEMPOTENT_KEY_REUSED` conflict
  (`XianyuOrderShipService.java:207-216`).
- Assignment exceptions other than the assignment service's own idempotency
  error are all remapped to `XIANYU_SHIP_DEVICE_NOT_SHIPPABLE`; schedule,
  model, lock, and incompatible-assignment conflicts therefore lose the
  required backfill conflict classification
  (`XianyuOrderShipService.java:424-435`).

## Extra Behavior

- No remote XianGuanJia write is introduced in the backfill branch, and the
  controller remains a thin permission-gated entry point. The extra behavior
  found is not an intentional feature addition; it is the replay/error
  handling above, which broadens existing Shipment reuse and changes typed
  failure semantics.

## Misunderstood Requirements

- Treating every same-device business-key hit as a valid backfill replay
  misunderstands the requirement that successful records have
  `source=ADMIN_BACKFILL` and match the normalized request/audit hash.
- Treating only order status as refund/closure eligibility misunderstands the
  separate persisted refund-state contract.
- Treating `@Transactional` plus mocked call ordering as proof of rollback
  misunderstands the requirement for all assignment, schedule, device,
  Shipment, Delivery, conversion, and channel-order rows to roll back.

## Cannot Verify From Diff

- The focused Mockito suite passes 28 tests, but it cannot prove database row
  rollback, row-lock/concurrency behavior, or persistence failure after local
  mutations. The Delivery failure test observes thrown exceptions and mock
  calls, not post-rollback database state
  (`XianyuOrderShipServiceTest.java:352-384`).
- The diff does not provide a real red-team/runtime receipt for cross-tenant
  references, non-shippable devices, refund-state rejection, or conflicting
  normalized replays. These remain Verification 2.0 work even where a mock
  interaction test exists.
- The frontend and browser surfaces are outside this task; A1-A3, A13, and A14
  cannot be promoted from backend static/unit evidence.

## Acceptance Assertions Verified

- A4 is statically supported by the Controller permission and validated Request
  VO (`XianyuOrderController.java:89-95`,
  `XianyuOrderDispatchBackfillReqVO.java:19-53`).
- A7 has partial static/Mockito support: the backfill method does not call the
  remote client or runtime write configuration, and the focused tests verify
  `never()` interactions. This is not a managed or runtime proof.
- A9 has partial unit support through the same-waybill/different-device test,
  but the replay and existing-Shipment branches still need the contract fixes
  above.
- A5, A6, A8, A10, and A11 are not fully verified.

## Required Fixes

- Add a refund-state gate and tests before device lookup or mutation.
- Require `ADMIN_BACKFILL`, matching request hash, and compatible Delivery
  state before treating an existing business-key row as a replay; distinguish
  formal shipment history from backfill idempotency.
- Validate idempotency-key conflicts against the persisted request before
  resolving a potentially invalid replacement device, and return the typed
  reuse error for all mismatched device/order/logistics/reason inputs.
- Preserve typed assignment failures or map schedule/device/model conflicts to
  `XIANYU_DISPATCH_BACKFILL_CONFLICT` instead of the generic ship error.
- Add a database-backed rollback/concurrency test and record authoritative
  validation receipts before approving this task.
