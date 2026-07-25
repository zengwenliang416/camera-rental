# Frontend-Backend Data Flow Spec

## Overview

All rental flows treat the backend as the source of truth. A client may validate
required fields or show a provisional availability result, but price, occupied
range, conflict detection, assignment, authorization, and state transitions are
recomputed by the backend before persistence.

Every write carries an actor context and an idempotency or version strategy
appropriate to the operation. Every external-channel flow preserves raw input,
normalized output, conversion version, and a redacted failure reason.

## Flow Index

| Flow ID | Trigger | Entry UI | API/Service | Persistence | User Result |
| --- | --- | --- | --- | --- | --- |
| `FLOW-CATALOG-AVAILABILITY` | customer selects product and rental dates | customer web or uni-app product page | app rental catalog/availability service | read product configs, devices, effective schedules | provisional price and availability, or unavailable reason |
| `FLOW-RENTAL-ORDER-CREATE` | customer submits rental order | customer web or uni-app confirmation page | app rental order service | create rental order/items after server revalidation | created order or explicit conflict/validation result |
| `FLOW-DEVICE-ASSIGNMENT` | operator allocates equipment | admin schedule/order page | admin rental assignment service | lock/read devices and schedules, then create assignment and occupied schedule | confirmed assignment or conflicting device result |
| `FLOW-DEVICE-HANDOVER` | staff scans equipment for outbound/return | staff mobile operation page | admin rental operation service | transition assignment, delivery, device, order, inspection records | validated operation result and next required action |
| `FLOW-XIANYU-SHOP-READ` | operator checks connected shops | admin integration page | admin XianGuanJia shop query service | refresh or read channel application/shop authorization state | shop list with authorization health |
| `FLOW-XIANYU-ORDER-SYNC` | scheduled job or authorized manual sync runs | admin sync page or scheduler | XianGuanJia order list/detail client and normalization service | raw payload, channel order, cursor, sync record, optional rental-order link | run counts, review queue, and redacted failures |
| `FLOW-XIANYU-ORDER-PUSH` | XianGuanJia sends an order event | backend callback only | verified webhook receiver and detail-fetch service | raw event, dedupe key, channel order/detail processing record | prompt platform response; later visible sync/review status |
| `FLOW-RETURN-INSPECTION` | returned device reaches inspection | staff inspection page | admin rental inspection service | inspection result, device state, occupied schedule release or maintenance record | device becomes available only after successful completion |

## Boundary Contracts

- UI event contract: the client sends user intent and typed input, not a
  client-computed authoritative amount, device availability, or final state.
- Client state contract: filters, draft form values, selected dates, loading,
  and display cache may be local; persisted order/device/channel records are
  refreshed from responses.
- Request schema: request VOs contain actor-appropriate identifiers, typed dates,
  expected version/idempotency key where required, and no third-party secret.
- Response schema: existing common result wrapper plus response VO; monetary
  fields are integer cents, external long identifiers are strings, and business
  dates follow project serialization.
- Error schema: distinguish validation, unauthenticated, forbidden, not found,
  stale version, schedule conflict, duplicate request, external unavailable, and
  manual-review-required results.
- Permission contract: admin and staff calls use backend permissions and data
  scope; app calls use member identity for private operations; callbacks use the
  official verification mechanism.

## State Ownership

- URL state: route identity, pagination, sortable filters, and shareable
  non-sensitive query state.
- Local component state: dialog visibility, scan input, form drafts, and
  transient selection.
- Shared client cache: authentication, stable dictionaries, user preferences,
  and invalidatable server read models.
- Server state: product rental configuration, quote result, availability,
  orders, devices, schedules, assignments, channel status, and sync progress.
- Database state: authoritative records and audit history.
- Derived state: display status, formatted currency/date, utilization, and
  review badges derived from server fields; clients do not persist competing
  truth.

## Validation Ownership

- Client-side validation: required fields, basic type/range checks, and immediate
  input guidance.
- Server-side validation: permissions, state transitions, pricing, date rules,
  shop authorization, channel configuration, and all cross-entity invariants.
- Database constraints: unique order/device/channel identities, referential
  integrity, optimistic version where used, and concurrency support for
  schedule allocation.
- Cross-field or cross-entity rules: rent date order, occupied range, device
  allocatability, order/assignment state, deposit/refund conservation, and
  channel mapping idempotency.
- Error copy source: backend returns stable error codes and safe context; each
  client maps them to complete `zh-CN` and `en` product copy according to the
  approved locale policy.

## Error & Empty States

- Empty state: state that no matching data exists and offer the permitted next
  action; never present empty as a loading failure.
- Permission denied: hide prohibited action affordances where possible and
  still handle backend `403` explicitly.
- Validation error: associate safe field errors with inputs; keep valid draft
  data.
- Network error: retain draft/filter state and offer retry without duplicating
  writes.
- Server error: show a correlation-safe message and avoid raw exception text.
- Conflict/stale data: identify the device/order that changed when safe, refresh
  server state, and require the user to reconfirm.

## Loading / Optimistic / Retry Behavior

- Initial loading: use the current client's skeleton or loading component and
  prevent duplicate primary actions.
- Partial loading: lists and detail panels fail independently when their APIs
  are independent.
- Optimistic update: prohibited for assignment, device handover, inspection,
  money, and channel authorization state. Low-risk UI preferences may remain
  optimistic.
- Retry rule: reads may retry with bounded backoff; writes retry only with an
  idempotency key and a classified retryable failure.
- Cancellation rule: superseded searches may be cancelled; an accepted
  transactional operation is not represented as cancelled only because the
  client disconnected.
- Idempotency rule: order submission, scan actions, callbacks, sync pages, and
  external-order conversion use stable keys and return the already-accepted
  result when replayed.

## End-to-End Flow Details

### `FLOW-CATALOG-AVAILABILITY`

1. User action: choose SKU/package and requested rent dates.
2. UI state transition: debounce or submit the query and show provisional
   loading.
3. Request payload: product/package identifier, quantity, rent start/end, and
   location or delivery context when required.
4. Backend validation: normalize inclusive business dates in `Asia/Shanghai`
   to `[start, endExclusive)`, validate product rental status, calculate the
   integer-cent quote, and query SKU capacity against effective reservations
   and device blocks.
5. Database read/write: read configs, devices, and effective schedules; no
   reservation is created by a read-only availability request.
6. Response payload: quote breakdown, rent range, occupied-range explanation,
   availability count/result, and expiry/version metadata if used.
7. UI render result: show amount and availability as provisional until order
   creation.
8. Retry/idempotency behavior: safe read retry; stale responses are discarded.
9. Rollback behavior on failure: none because no persistent write occurs.
10. Logging/metrics/audit event: latency and safe product/date dimensions, with
    no customer-private data.

### `FLOW-RENTAL-ORDER-CREATE`

1. User action: submit the confirmed rental form.
2. UI state transition: disable duplicate submission and show processing.
3. Request payload: member intent, items, dates, fulfillment choice, accepted
   quote/version if applicable, and idempotency key.
4. Backend validation: authenticate member, rebuild quote, validate addresses or
   pickup data, and recheck capacity/scheduling rules.
5. Database read/write: transactionally create order and items plus an expiring
   SKU quantity reservation for the occupied interval; do not bind a physical
   device during customer order creation.
6. Response payload: order number, authoritative amounts/dates/state, and next
   action.
7. UI render result: navigate to the server-created order or show a specific
   conflict that requires reconfirmation.
8. Retry/idempotency behavior: the same member and idempotency key return the
   existing order result.
9. Rollback behavior on failure: no partial order/items/schedule writes remain.
10. Logging/metrics/audit event: order result and correlation id without full
    contact/address data.

### `FLOW-DEVICE-ASSIGNMENT`

1. User action: before picking, select an order item and proposed physical
   device, or scan a candidate device against the confirmed SKU reservation.
2. UI state transition: show candidate loading and disable confirmation while
   saving.
3. Request payload: order item, device or policy input, occupied dates,
   expected versions, and idempotency key.
4. Backend validation: permission, paid/confirmed reservation state, device
   SKU/status, warehouse, half-open occupied interval, maintenance block, and
   existing effective schedules.
5. Database read/write: in one transaction acquire the selected concurrency
   guard, recheck overlap using `newStart < existingEndExclusive` and
   `newEndExclusive > existingStart`, consume the reserved quantity, create the
   assignment/schedule, and transition state.
6. Response payload: confirmed device, schedule, updated order/device versions,
   or conflicting range.
7. UI render result: render confirmed assignment from response; on conflict,
   refresh candidates.
8. Retry/idempotency behavior: duplicate confirmation returns the existing
   assignment; a competing assignment returns conflict.
9. Rollback behavior on failure: assignment, schedule, and state transition all
   roll back together.
10. Logging/metrics/audit event: actor, device, order item, result, and conflict
    category.

### `FLOW-DEVICE-HANDOVER`

1. User action: staff scans a device for outbound, return receipt, or next
   operation.
2. UI state transition: resolve the scan, show expected order/action, then ask
   for confirmation where required.
3. Request payload: scan code, operation type, order/assignment context,
   idempotency key, and optional safe evidence references.
4. Backend validation: permission, device identity, current assignment and
   state, expected operation sequence, and duplicate scan.
5. Database read/write: transactionally record operation/delivery and transition
   order, assignment, device, and inspection requirement.
6. Response payload: accepted operation, updated state, and next required task.
7. UI render result: clear success or actionable mismatch; never mutate local
   status without acceptance.
8. Retry/idempotency behavior: replay returns the accepted operation result.
9. Rollback behavior on failure: no partial state transition.
10. Logging/metrics/audit event: operator, device, operation, result, and
    redacted exception category.

### `FLOW-XIANYU-ORDER-SYNC`

1. User action: scheduler starts a due run or an authorized operator requests a
   bounded read sync for a selected shop.
2. UI state transition: admin page shows queued/running status from server state.
3. Request payload: shop id, sync type, optional safe starting point, and
   bounded upper time; credentials remain server-side.
4. Backend validation: integration enabled, permission for manual runs, selected
   shop belongs to the current tenant/application, and authorization is valid.
5. Database read/write: create sync record, read durable cursor, fetch bounded
   `POST /api/open/order/list` pages, upsert raw and normalized channel orders by
   external identity, pull `POST /api/open/order/detail` when required, update
   counts, and advance cursor only after durable processing. The list query uses
   a fixed `update_time` window within the documented six-month range and splits
   before a query would exceed 10,000 rows.
6. Response payload: run id/status and received, deduplicated, succeeded,
   review-required, and failed counts.
7. UI render result: show run outcome and review queue without exposing raw PII
   or secrets.
8. Retry/idempotency behavior: repeated pages and detail pulls upsert the same
   channel order; tie-breaker cursor handles identical update timestamps.
9. Rollback behavior on failure: committed records remain replay-safe; cursor
   does not advance past unhandled data.
10. Logging/metrics/audit event: application/shop safe ids, run id, page/count
    metrics, HTTP status/error category, and no secret/header/body dump.

## Async / Realtime Flows

- Queue/event source: scheduler or verified callback creates a local processing
  record; transport choice must reuse approved backend infrastructure.
- Subscriber: rental integration service loads channel detail, normalizes it,
  and invokes rental conversion under an idempotent transaction.
- Retry/dead-letter behavior: bounded exponential retry for transient external
  errors; permanent schema/auth/validation errors enter a visible review or
  failed state with manual replay.
- Realtime update channel: not required for the first baseline; clients poll or
  refresh run/order state unless a later change approves SSE/WebSocket.
- Consistency expectation: eventual consistency for external channel sync;
  strong transactional consistency for local assignment and state transitions.

## Flow Do's and Don'ts

- Do keep every feature requirement traceable to a named flow.
- Do record loading, empty, error, disabled, and permission states.
- Do include database and integration side effects.
- Do preserve raw and normalized channel evidence.
- Do treat external identifiers as strings across JSON and frontend boundaries.
- Don't let frontend and backend disagree on validation ownership.
- Don't introduce implicit data transformations that are not documented here.
- Don't retry third-party or local writes without a stable idempotency strategy.
