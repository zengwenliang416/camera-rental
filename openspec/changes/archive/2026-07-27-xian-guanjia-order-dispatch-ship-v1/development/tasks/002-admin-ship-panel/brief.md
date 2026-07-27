# Task 002: Admin Ship Panel

## Goal

An admin user can capture courier/device codes, search pending XianGuanJia
orders, confirm the selected device-order association, and submit shipment from
the Web admin panel.

## Vertical Slice

Admin opens the shipment panel, uploads the courier waybill code image, scans or
enters the device QR/serial, searches pending orders, confirms the match, and
submits shipment with typed success/failure feedback.

## In Scope

- Typed admin API methods for OCR, pending-order search, device resolution, and
  ship.
- `RentalXianyuOrderShipPanel` UI section using existing Element Plus patterns.
- Confirm-before-submit guard for courier values and order-device association.
- Loading, empty search, stale-order, write-disabled, shop-not-authorized,
  OCR-failed, scan-rejected, ship-failed, and success states.

## Files Allowed

- `camera-rental-admin/src`
- `camera-rental-admin/package.json`
- `camera-rental-admin/pnpm-lock.yaml`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1`

## Verification Commands

- `cd camera-rental-admin && pnpm ts:check`
- `git diff --check`

## Stop Conditions

- Admin UI uses typed API methods only.
- Submit remains disabled until courier values, device, pending order, and
  association confirmation are present.
- Type check covers the new panel and request/response types.
