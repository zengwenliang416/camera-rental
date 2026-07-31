# Task Report: 003-xianyu-shipment-delivery

## Status

DONE

## Files Changed

- `XianyuOrderShipService`
- `XianyuOrderShipRespVO`
- `RentalDeviceShipmentDO`
- `XianyuOrderShipServiceTest`
- Task report, validation log, drift check, and task ledger for slice 003.

## What Changed

- Extended the existing Xianyu shipment transaction to create or reuse a local
  outbound Delivery after remote shipment success, device dispatch, and shipment
  audit insertion.
- Bound the converted rental order item, assignment, and physical device through
  the provider-neutral Delivery command.
- Persisted `rental_device_shipment.delivery_id` before updating the local
  Xianyu order shipment fields.
- Added Delivery ID, mapping/subscription/query states, stable reason code, and
  pending Outbox event types to the existing response without changing the
  endpoint or permission.
- Reused the Delivery result's masked waybill for new records while keeping the
  legacy shipment masking fallback for old rows without a Delivery.
- Replayed linked shipments through `getResult` without creating a second
  Delivery or duplicate Outbox tasks.

## TDD Evidence

- `XianyuOrderShipServiceTest` verifies the Delivery command fields, hashed
  source identifier, tracking phone, device relation, remote/dispatch/shipment/
  Delivery/order execution order, shipment `delivery_id` update, and enriched
  response.
- Idempotent replay returns the existing Delivery state and performs no remote
  shipment, dispatch, shipment insert, or Delivery creation.
- Mapping-required and Provider-disabled states preserve shipment success.
- Delivery persistence failure stops subsequent local writes; the surrounding
  transaction remains responsible for rolling back local shipment, relation,
  Outbox, assignment, and dispatch persistence.
- The response uses the Delivery layer's masked waybill rather than duplicating
  masking policy.
- Legacy shipment replay and linked Delivery replay both reuse
  `WaybillPrivacy`; no second masking implementation remains in the shipment
  service.

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='XianyuOrderShipServiceTest,*ShipmentDelivery*' -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='RentalDeliveryServiceImplTest,XianyuOrderShipServiceTest,*ShipmentDelivery*' -Dsurefire.failIfNoSpecifiedTests=false test`
- `git diff --check`
- Static scan confirming `XianyuOrderShipService` has no Kuaidi100 adapter or
  Provider network dependency.

## Concerns

- The existing remote Xianyu call still occurs before local dispatch and local
  Delivery persistence, preserving the established shipment behavior required
  by this slice. A later local failure therefore relies on the shipment
  idempotency contract for safe retry; this slice intentionally does not reorder
  the existing remote operation.
- SpecNav entry remains blocked by `git-baseline:tasks-not-tracked`; no staging
  or commit was performed.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Slice 004 must consume the local Delivery read model from the schedule center
  without direct Provider calls.
- Six-domain verification still needs an integrated database transaction test
  covering shipment, Delivery relation, Outbox, and rollback behavior.

## Adjudication

Independent specification and quality re-reviews approved the current
implementation. The reviewer independently reran the broader focused command
and observed 24 tests with no failures or errors, verified both replay paths use
the shared masking policy, and confirmed `git diff --check` passes.
