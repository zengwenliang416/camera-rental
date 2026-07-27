## Why

Operators need a fast warehouse shipment flow for XianGuanJia orders: capture
the courier waybill code, capture the machine QR/serial number, search the
pending-dispatch order pool, confirm the device-order match, and then ship
through XianGuanJia's official order ship API.

The existing read-only integration can import and manage orders, but it does
not complete channel shipment or persist the final device-order shipment trace.

## What Changes

- Add backend pending-dispatch order search for XianGuanJia orders.
- Add backend courier code OCR/barcode extraction for waybill number and express
  company only.
- Add a new XianGuanJia write client limited to `/api/open/order/ship`.
- Add backend ship orchestration that validates write enablement, shop
  authorization, pending order status, device shippability, idempotency, and
  tenant boundaries before any write call.
- Persist the device-order binding, shipment row, and `DISPATCHED` assignment
  only after the XianGuanJia ship call succeeds.
- Add admin Web and staff app shipment surfaces for code capture, pending-order
  search, association confirmation, and ship.

## Capabilities

### New Capabilities

- `xian-guanjia-order-dispatch-ship`: operator-driven pending-order search,
  device binding, XianGuanJia order shipment, and local shipment trace.
- `shipment-code-extraction`: courier label / courier mini-program code image
  extraction for waybill number and express company.

### Modified Capabilities

- `rental-device-operations`: reuse device QR resolution and dispatch lifecycle
  after successful channel shipment.

## Impact

- Backend: additive rental shipment domain, write client, OCR adapter, ship
  service, admin endpoints, permissions, tests, and configuration.
- Database: additive `rental_device_shipment` table and permission seed SQL.
- Admin: shipment panel on the rental/XianGuanJia operational area.
- Staff: new scan/search/bind/ship screen and rental API module.
- Runtime: `XGJ_WRITE_ENABLED` remains false by default; OCR credentials are
  config-only.
- No customer-facing `camera-rental-uniapp` or `camera-rental-web` change.
