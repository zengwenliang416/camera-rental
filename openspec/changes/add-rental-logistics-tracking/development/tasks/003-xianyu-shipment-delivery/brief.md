# Task Brief: 003-xianyu-shipment-delivery

## Goal

Shipping operators can complete Xianyu shipment and receive a linked local
Delivery plus queued tracking state without Provider failures changing shipment
success.

## Parent Artifacts

- `openspec/changes/add-rental-logistics-tracking/requirements.md`
- `openspec/changes/add-rental-logistics-tracking/acceptance.md`
- `openspec/changes/add-rental-logistics-tracking/prototype/handoff.md`

## Vertical Slice

Extend the existing shipment command from validated device and order through
remote Xianyu success, device dispatch, shipment audit, Delivery relation,
Outbox enqueue, and an enriched response.

## In Scope

- Inject Delivery and Outbox services into `XianyuOrderShipService`, link
  shipment to Delivery, create `SUBSCRIBE` and `INITIAL_QUERY`, expose masked
  tracking state in the existing response, and add transaction/degradation tests.

## Out Of Scope

- No reordering of existing remote shipment and dispatch rules without evidence,
  no synchronous Kuaidi100 call, no OCR auto-dispatch, and no multi-device UI
  redesign.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `openspec/changes/add-rental-logistics-tracking`

## Interfaces / Seams

- Existing `XianyuOrderShipService`, `RentalDeviceShipmentDO`, Delivery service,
  Outbox service, carrier mapping, and response VO.

## Components To Create

- Shipment-to-Delivery command assembler and stable tracking-result response
  fields where extraction improves testability.

## Components To Reuse

- Existing pending-order, assignment, Xianyu write client, dispatch, shipment
  audit, idempotency, masking, and transaction behavior.

## Components To Extract

- Delivery command assembly and shipment tracking response mapping must not
  duplicate mapping or masking policies.

## API / Data Flow Contracts

- Existing `POST /admin-api/rental/xianyu/order/ship` remains compatible and
  adds local Delivery ID, state, reason, and queued-event summary.

## State / Error / Empty / Loading Behavior

- Loading: not applicable to the synchronous command response.
- Empty: no missing local Delivery is allowed after successful local commit.
- Error: local transaction failures roll back local shipment/Delivery writes.
- Disabled: mapping/config absence returns a stable degraded Delivery state.
- Permission: existing `rental:xianyu:ship` remains authoritative.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='XianyuOrderShipServiceTest,*ShipmentDelivery*' test`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- A change would call Kuaidi100 before transaction commit.
- Existing Xianyu remote-call behavior would be changed without a failing test.

## Unsafe Assumptions

- Do not assume Provider success or mapping availability.
- Do not assume an idempotent shipment replay needs a new Delivery or Outbox.
- Do not assume one shipment response may expose a full waybill.
