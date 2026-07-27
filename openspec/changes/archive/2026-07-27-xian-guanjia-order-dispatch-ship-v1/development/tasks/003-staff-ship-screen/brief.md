# Task 003: Staff Ship Screen

## Goal

A staff user can complete the same courier/device code capture, pending-order
search, association confirmation, and shipment submission flow from the mobile
staff app.

## Vertical Slice

Staff opens the shipment screen, captures the courier waybill code image, scans
the device QR, searches pending orders, confirms the order-device association,
and submits shipment through the same backend ship endpoint.

## In Scope

- New staff rental API module.
- New staff shipment page using `uni.scanCode`, staff upload/http conventions,
  and permission-aware menu entry.
- Mobile states for scan rejected, empty search, stale order, write disabled,
  shop not authorized, OCR failed, ship failed, and success.
- H5 and WeChat Mini Program build compatibility.

## Files Allowed

- `camera-rental-staff/src`
- `camera-rental-staff/package.json`
- `camera-rental-staff/pnpm-lock.yaml`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1`

## Verification Commands

- `cd camera-rental-staff && pnpm build:h5`
- `cd camera-rental-staff && pnpm build:mp-weixin`
- `git diff --check`

## Stop Conditions

- Staff screen uses the shared backend endpoints and does not duplicate QR decode
  or device availability logic.
- H5 and WeChat Mini Program builds complete or failures are documented with
  exact missing dependency/script evidence.
