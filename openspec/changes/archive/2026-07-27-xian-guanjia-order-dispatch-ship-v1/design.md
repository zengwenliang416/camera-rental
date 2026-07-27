## Context

This change builds on `xian-guanjia-data-integration-v1`, which keeps the
XianGuanJia read client closed and read-only. Shipment is a separate write path
with stricter gates, explicit permissions, and audit logging.

The current XianGuanJia documentation for `POST /api/open/order/ship` was
checked on 2026-07-26 from:

- `https://s.apifox.cn/3ac13d69-5a38-4536-ae9b-a54001854ef8/llms.txt`
- `https://s.apifox.cn/3ac13d69-5a38-4536-ae9b-a54001854ef8/api-93586386.md`

The documented required body fields are `order_no`, `waybill_no`,
`express_code`, and `express_name`. Sender fields are optional when XianGuanJia
has a default sender address configured; the first implementation will not
collect sender PII in the shipment UI.

## Goals / Non-Goals

**Goals**

- Let operators capture courier waybill code + machine QR/serial first.
- Let operators search the XianGuanJia "待发货" order pool and manually confirm
  the selected order-device association.
- Ship the confirmed order through `/api/open/order/ship`.
- Persist device-order binding and shipment evidence only after channel ship
  success.
- Keep write access disabled by default and permission-gated.

**Non-Goals**

- No automatic order matching or automatic shipment.
- No other XianGuanJia write endpoints.
- No recipient name, phone, address, or sender PII extraction from images.
- No customer-facing logistics display.

## Decisions

- Introduce `XianyuWriteClient` and `XianyuWriteEndpoint.ORDER_SHIP`; do not add
  write methods to the existing read client or read endpoint allowlist.
- Add `GET /admin-api/rental/xianyu/order/pending-ship/page` for candidate order
  search. It returns masked/limited fields and does not persist anything.
- Add `POST /admin-api/rental/xianyu/order/ship/ocr` for courier-code image
  extraction. It extracts only `waybillNo`, `expressName`, confidence, and
  source, with manual entry fallback.
- Add `POST /admin-api/rental/xianyu/order/ship` using `channelOrderId`,
  `deviceId` or `deviceNo`, `idempotencyKey`, `expressCode`, `expressName`,
  `waybillNo`, and `source`.
- The ship service rechecks pending shipment status and device shippability at
  submit time, then calls XianGuanJia. If the channel write succeeds, it creates
  or reuses the local assignment, persists `rental_device_shipment`, and advances
  the assignment to `DISPATCHED`.
- If the channel write fails, the durable binding, shipment row, and assignment
  state change are not committed.
- OCR and barcode extraction live under `integration/ocr`, not in the XianGuanJia
  client package.
- The courier code extractor treats a large visible waybill number next to a QR
  or one-dimensional barcode as authoritative and ignores unrelated background
  order numbers.

## Data Model

- `rental_device_shipment` stores tenant id, channel order id, assignment id,
  device id, waybill number, express code/name, request hash, response code/msg,
  OCR-confirmed flag, source, creator/updater, timestamps, and soft-delete bit.
- Unique `(tenant_id, idempotency_key)` prevents duplicate submission for one
  client operation.
- Unique `(tenant_id, channel_order_id, waybill_no, express_code)` prevents the
  same waybill from being shipped twice for one channel order.

## API / Data Flow Contracts

- `FLOW-SHIPMENT-OCR`: image upload -> OCR/barcode extraction -> editable draft.
- `FLOW-DEVICE-CODE-RESOLVE-FOR-SHIP`: device QR/serial input ->
  `/rental/device/resolve-qr` -> resolved device and shippability summary.
- `FLOW-XIANYU-PENDING-ORDER-SEARCH`: keyword search ->
  `/rental/xianyu/order/pending-ship/page` -> candidate pending orders.
- `FLOW-XIANYU-ORDER-SHIP`: confirmed device + order + waybill ->
  `/rental/xianyu/order/ship` -> write gate + shop auth + status/device recheck
  -> XianGuanJia ship -> local binding/shipment/dispatch commit.

## State / Error / Empty / Loading Behavior

- Empty search: no matching pending-dispatch order.
- Scan rejected: invalid device QR/serial or unknown device.
- Device not shippable: backend rejects before any write call.
- Stale order: search result is no longer pending shipment at submit time.
- Write disabled: `rental.xianyu.write-enabled=false`.
- Shop not authorized: reuse `XIANYU_SHOP_AUTHORIZATION_INVALID`.
- OCR failed: show empty draft; manual entry remains allowed.
- Ship failed: show channel error; local state remains unchanged.

## Risks / Trade-offs

- The operator still confirms the order-device match manually. This avoids unsafe
  auto-matching from image text but requires a clear search UI.
- OCR confidence can be wrong. Manual edit and confirmation are mandatory.
- Sender information is not collected in v1. Deployment depends on XianGuanJia
  default sender address configuration unless sender fields are added later.

## Migration Plan

1. Add additive shipment table and permission seed SQL.
2. Deploy write disabled.
3. Configure OCR and XianGuanJia credentials through runtime configuration.
4. Grant `rental:xianyu:ship` and `rental:xianyu:ship:ocr` to warehouse roles.
5. Enable `XGJ_WRITE_ENABLED` only after mock and controlled real-shop checks.

Rollback disables the write flag first. Shipment rows and assignments are
retained for audit; destructive rollback needs a separate approval.
