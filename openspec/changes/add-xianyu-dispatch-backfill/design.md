## Context

The existing Xianyu shipping workflow handles orders that are still pending shipment and deliberately calls XianGuanJia before committing local shipment state. This change addresses a different case: the order was already shipped outside this platform, while the local rental domain is missing the actual physical-device binding and outbound evidence.

The operator only has the Web admin surface and an existing permission assigned to the shop administrator. The solution must reuse the current rental conversion, device assignment, scheduling, dispatch, shipment, and Delivery capabilities without requiring physical scanning hardware or introducing another external write operation.

The implementation spans `camera-rental-server` and `camera-rental-admin`. It reuses existing database structures and therefore requires no migration.

## Goals / Non-Goals

**Goals:**

- Allow an authorized administrator to backfill the actual device and logistics facts for Xianyu status `21`/`22` orders.
- Restore a consistent local rental aggregate: conversion, order item, assignment, occupied schedule, device/assignment dispatch state, Shipment, Delivery, and channel-order logistics.
- Guarantee that the path never calls the XianGuanJia shipment API.
- Make retries safe through normalized idempotency and business-key conflict detection.
- Preserve tenant isolation and existing backend permission enforcement.

**Non-Goals:**

- Ship pending orders or update any XianGuanJia remote state.
- Add scanner, camera, OCR, staff-app, customer-app, or customer-Web support.
- Add user-to-shop data-scope authorization beyond current tenant isolation.
- Support multiple devices on one waybill or explicit item selection for multi-item orders.
- Add tables, migrations, configuration, roles, or permission codes.

## Decisions

### Use a separate local-only endpoint

`POST /admin-api/rental/xianyu/order/dispatch-backfill` is separate from `/ship`.

Rationale:

- `/ship` has a required remote-write contract and write-enable gate.
- Backfill represents an already-completed external fact and must remain operable when remote authorization or write configuration is unavailable.
- A separate request VO makes the required reason and actual ship time explicit and keeps source `ADMIN_BACKFILL` auditable.

Alternative considered: add a `skipRemote` flag to `/ship`. Rejected because it would mix incompatible security and side-effect contracts and make accidental remote calls harder to prove absent.

### Reuse the existing ship orchestration service with explicit methods

`XianyuOrderShipService` owns both `ship` and `backfillDispatch`, but each method has a distinct entry and control flow. Shared local helpers are reused only where their invariants match.

Rationale:

- Both flows coordinate the same order/device/assignment/shipment aggregate.
- Keeping the methods together avoids duplicating conversion and response mapping.
- Explicit methods keep `XianyuWriteClient` reachable only from the formal ship path.

Alternative considered: create a second large backfill service. Rejected because it would duplicate the same rental aggregate coordination while offering no new domain boundary.

### Gate by local channel state, not remote authorization

Backfill accepts only local channel status `21` or `22`, rejects cancellation/closed status, and verifies that the referenced shop belongs to the current tenant. It does not require external authorization status `VALID`.

Rationale:

- The external shipment has already happened.
- External authorization loss must not prevent repairing local historical truth.
- Tenant ownership is still mandatory to prevent cross-tenant IDOR.

### Reuse assignment and dispatch invariants

For a new assignment, the selected device must be enabled and `AVAILABLE`; the existing assignment service creates the occupied schedule. For an `ASSIGNED` relationship, the existing dispatch operation performs the state transition. An already `DISPATCHED` assignment is reusable only when the device is `RENTED`.

Rationale:

- The backend remains authoritative for scheduling and device state.
- Reuse preserves existing concurrency controls and audit behavior.
- The explicit `DISPATCHED`/`RENTED` consistency check prevents silently accepting partial corruption.

### Use existing Shipment and Delivery records

The operation writes `rental_device_shipment.source=ADMIN_BACKFILL` and creates/reuses an outbound Delivery with `shipment-backfill:<MD5(idempotencyKey)>` as its stable source identifier.

Rationale:

- Existing operational and logistics views already understand these records.
- A distinct source makes historical repair distinguishable from remote shipment.
- Reuse avoids a migration and preserves downstream tracking integration.

### Bind idempotency to normalized business intent

The service trims the idempotency key, waybill, carrier code, and carrier name. It hashes order, resolved device, normalized logistics, actual ship time, and trimmed reason. Replays must match both the idempotency record and request hash.

Rationale:

- Browser retries can safely return the original result.
- Reusing a key with changed facts is rejected rather than silently mutating history.
- The existing business key catches a second device proposed for the same order/waybill/carrier.

### Keep the UI as a bounded dialog

The order page decides whether to expose the action and owns list refresh. `XianyuDispatchBackfillDialog` owns draft values, form validation, loading state, per-open idempotency key, and completion emission. The typed API stays in the existing Xianyu API module.

Rationale:

- The operation is a bounded correction against a selected order.
- The component does not need a new shared store or hook.
- Server failures can preserve the draft while the table remains the source for refreshed order state.

## Risks / Trade-offs

- [Tenant permission is broader than shop-specific access] → Preserve current tenant isolation, require `rental:xianyu:ship`, and document shop-level data scope as a future independent change.
- [One waybill may contain multiple physical devices] → Current Shipment business key treats this as a conflict; require separate future schema and assignment policy before enabling it.
- [Multi-item channel orders use the first rental item] → Keep behavior explicit and verified; require a later item-selection change before supporting ambiguous orders.
- [Historical status may be stale] → Accept only exact local statuses `21`/`22`, reject cancellation timestamps, lock the row, and require an operator-entered reason.
- [Existing dispatched state could be inconsistent] → Reuse only `DISPATCHED` + `RENTED`; return a typed conflict for other combinations.
- [Delivery creation can fail after local dispatch code executes] → Keep the whole method transactional and verify rollback behavior.
- [UI-generated idempotency key changes after reopening] → Within an open dialog, retries reuse the same key; business-key lookup protects reopened equivalent submissions.

## Migration Plan

1. Deploy the backend endpoint and local orchestration changes.
2. Deploy the admin typed API, order action, dialog, and locale changes.
3. No database migration or configuration rollout is required.
4. Assign no new permissions; existing `rental:xianyu:ship` holders receive the action for eligible orders.
5. Rollback is code-only. Existing `ADMIN_BACKFILL` Shipment and Delivery history remains valid and must not be deleted.

## Open Questions

None for this change. Shop-level data scope, multi-device waybills, and multi-item selection are accepted exclusions requiring separate changes.
