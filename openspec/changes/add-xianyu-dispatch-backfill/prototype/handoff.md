# Prototype Handoff: add-xianyu-dispatch-backfill

## Approved Branch Variant

- Branch: `ui-html`
- Variant: `admin-dialog-v1`
- Approval: the user explicitly approved `admin-dialog-v1` on August 23, 2026.
- Entry: `prototype/artifact/index.html`
- Verifier: `prototype/verifier-report.json` with status `green`

## Screens Or Flows

- Admin route: `/rental/order`
- Primary screen: the existing channel-order list in the current `camera-rental-admin` classic sidebar shell.
- Primary flow: eligible status `21`/`22` order row -> `补录出库设备` -> bounded dialog -> keyboard-entered device and logistics facts -> local-only API -> accepted response -> dialog close and list refresh.
- Negative flow: validation or backend conflict keeps the dialog and draft available.
- Permission flow: without `rental:xianyu:ship`, the action is absent and the dialog cannot be opened.

## Components To Create

- `camera-rental-admin/src/views/rental/order/components/XianyuDispatchBackfillDialog.vue`
- `camera-rental-server/.../controller/admin/xianyu/vo/XianyuOrderDispatchBackfillReqVO.java`

## Components To Reuse

- Existing admin channel-order table and `v-hasPermi` permission directive.
- Element Plus dialog, alert, descriptions, form, input, date picker, textarea, and buttons.
- Existing `useI18n`, `useMessage`, theme switch, locale switch, and classic admin shell.
- Existing typed Xianyu API module and `XianyuOrderShipRespVO`.
- Existing `XianyuOrderController` and `XianyuOrderShipService` domain boundary.
- Existing conversion, device assignment, occupied schedule, local dispatch, Shipment, and Delivery services.
- Existing `rental:xianyu:ship` permission and tenant context.

## Extraction Targets

- Keep form state, validation, loading state, per-open idempotency key, and completion event inside the bounded dialog.
- Do not extract a shared hook or form for this single admin use site.
- If a later staff or second admin surface reuses this flow, extract a same-client form and idempotent submit guard in that later change.
- If multi-item allocation is added, introduce an explicit order-item selection policy instead of extending first-item behavior implicitly.

## API Contracts

- Endpoint: `POST /admin-api/rental/xianyu/order/dispatch-backfill`
- Permission: `rental:xianyu:ship`
- Request fields: `channelOrderId`, `deviceId` or `deviceNo`, `idempotencyKey`, `expressCode`, `expressName`, `waybillNo`, `consignTime`, and `reason`.
- Response: reuse `XianyuOrderShipRespVO` for local shipment, Delivery, device, source, and assignment status.
- Side-effect boundary: this endpoint is local-only. It must not read Xianyu write-enabled configuration or invoke `XianyuWriteClient` or another remote shipment API.

## Data Flows

- `FLOW-XIANYU-DISPATCH-BACKFILL`: eligible channel order -> Web dialog -> typed admin API -> tenant/order/device/idempotency validation -> optional rental conversion -> assignment and occupied schedule -> local device dispatch -> `ADMIN_BACKFILL` Shipment -> outbound Delivery -> channel-order logistics update -> refreshed order list.
- All local mutations execute transactionally.
- The backfill flow contains no external-channel request.

## State Behavior

- Default: show the populated channel-order context and the `620px` bounded dialog.
- Validation: show field-level errors for device number, waybill, and reason without requiring scanner hardware.
- Loading: disable the form interaction and show progress on the primary submit button.
- Empty: not applicable to the selected-order correction dialog; the action requires a selected populated row.
- Error and conflict: preserve entered values, keep the dialog open, and surface non-mutating error feedback.
- Success: show success feedback, close the dialog, emit completion, and refresh the order list.
- Disabled: the loading submit guard prevents duplicate interaction while the request is active.
- Permission: hide the row action when `rental:xianyu:ship` is absent; backend permission enforcement remains authoritative.

## Theme And Locale Policy

- Theme support: existing admin light/dark support.
- Theme modes shown in prototype: `light`, `dark`.
- Theme toggle: present and inherited from the current admin shell.
- Internationalization: enabled through existing locale dictionaries and `useI18n`.
- Locales shown in prototype: `zh-CN`, `en`.
- Default locale: `zh-CN`.
- Locale switcher: present and inherited from the current admin shell.

## Out Of Scope Items

- Pending-order shipment or any XianGuanJia remote write.
- Physical scanner, browser camera, OCR, staff app, customer app, or customer Web changes.
- New tables, database migrations, configuration, roles, or permission codes.
- User-to-shop data-scope authorization beyond current tenant isolation.
- Multiple devices on one waybill.
- Explicit rental-item selection for multi-item channel orders.

## Required Tests

- Backend success: conversion/assignment/schedule/dispatch, `RENTED` device, `DISPATCHED` assignment, `ADMIN_BACKFILL` Shipment, outbound Delivery, and channel-order logistics are committed together.
- Backend eligibility: pending, refunded, cancelled, and closed orders reject before mutation.
- Backend security: cross-tenant shop/order and non-shippable device references reject without partial writes.
- Backend idempotency: matching replay returns the original result; mismatched key reuse and same-waybill different-device cases return typed conflicts.
- Backend rollback: Delivery or persistence failure rolls back assignment, schedule, device, Shipment, and channel-order changes.
- Backend remote-write proof: the path calls neither runtime write configuration nor `XianyuWriteClient`.
- Admin: action visibility is restricted to uncancelled status `21`/`22` plus permission.
- Admin: validation, loading, error draft preservation, success close, and list refresh.
- Admin: `zh-CN`/`en`, light/dark, desktop/narrow visual checks.
- Repository checks: targeted backend tests, admin type check, targeted ESLint, targeted Prettier, and `git diff --check`.

## Open Risks

- Authorization remains tenant-level rather than user-to-shop data-level.
- The existing Shipment business key permits only one device per waybill.
- Multi-item channel orders currently select the first rental order item.
- These are approved limitations for this change and require separate future changes before expansion.
