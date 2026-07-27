# Task 001: Backend Ship Core

## Goal

An operator can search pending XianGuanJia orders, bind a resolved physical
device, and ship through a backend endpoint that commits local state only after
the channel ship call succeeds.

## Vertical Slice

Operator captures courier/device codes, searches pending orders, confirms one
device-order association, submits shipment, and receives a masked success or a
typed failure without unsafe local mutation.

## In Scope

- Pending-shipment order search endpoint.
- Courier code OCR/barcode endpoint.
- Device QR/serial resolution reuse for shipment.
- `XianyuWriteClient` limited to `/api/open/order/ship`.
- `XianyuOrderShipService` with write flag, shop auth, pending-status recheck,
  device shippability recheck, idempotency, remote call, and local commit order.
- `rental_device_shipment` migration, DO, mapper, and tests.

## Files Allowed

- `camera-rental-server/yudao-module-rental`
- `camera-rental-server/yudao-server`
- `camera-rental-server/sql/mysql/migrations`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1`

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `git diff --check`

## Stop Conditions

- Backend unit tests cover write-disabled, shop-not-authorized, pending-order
  stale, non-shippable device, idempotency, remote failure no-local-mutation,
  success binding/dispatch, unique waybill, and cross-tenant rejection.
- The write client exposes no endpoint other than `/api/open/order/ship`.
