# Task Brief: 003-admin-backfill-dialog

## Goal

Let an authorized shop administrator use the existing Web channel-order page to
record the actual device and logistics facts for an already-shipped order.

## Parent Artifacts

- `openspec/changes/add-xianyu-dispatch-backfill/requirements.md`
- `openspec/changes/add-xianyu-dispatch-backfill/acceptance.md`
- `openspec/changes/add-xianyu-dispatch-backfill/prototype/handoff.md`

## Vertical Slice

For eligible status `21` or `22`, the row action opens the approved
`admin-dialog-v1`, validates keyboard input, preserves the draft on failure,
and closes plus refreshes the order list on success.

## In Scope

- Checklist items `3.1` through `3.4`.
- Typed API, permission/status action visibility, bounded Element Plus dialog,
  validation, idempotency key, loading/error/success behavior, and `zh-CN`/`en`
  copy.

## Out Of Scope

- Scanner, browser camera, staff/mobile/customer clients, new UI framework, or
  frontend-owned inventory and scheduling rules.

## Files Allowed

- `camera-rental-admin/src/api/rental/xianyu.ts`
- `camera-rental-admin/src/views/rental/order/index.vue`
- `camera-rental-admin/src/views/rental/order/components/XianyuDispatchBackfillDialog.vue`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/locales/en.ts`

## Components To Create

- `XianyuDispatchBackfillDialog`.

## Components To Reuse

- Existing Xianyu API module, order table, `v-hasPermi`, `useI18n`,
  `useMessage`, and Element Plus form/dialog components.

## Components To Extract

- Keep the single-use form state and submit guard inside the dialog.
- Extract a shared same-client form only if a second production surface is
  introduced; that is outside this change.

## TDD Requirement

- Treat permission/status visibility, validation, loading guard, error draft
  preservation, and success refresh as explicit behavior assertions.

## Verification Commands

- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-admin && pnpm exec eslint src/api/rental/xianyu.ts src/views/rental/order/index.vue src/views/rental/order/components/XianyuDispatchBackfillDialog.vue src/locales/zh-CN.ts src/locales/en.ts`
- `cd camera-rental-admin && pnpm exec prettier --check src/api/rental/xianyu.ts src/views/rental/order/index.vue src/views/rental/order/components/XianyuDispatchBackfillDialog.vue src/locales/zh-CN.ts src/locales/en.ts`

## Stop Conditions

- Scope lock mismatch.
- The frontend attempts to decide authoritative device availability or local
  transaction success.
- The action is visible without `rental:xianyu:ship` or for an ineligible
  order status.

## Unsafe Assumptions

- Hiding the action is not authorization; the backend permission remains
  authoritative.
- Existing order logistics values may be absent and the dialog must still
  provide a valid keyboard-first path.
