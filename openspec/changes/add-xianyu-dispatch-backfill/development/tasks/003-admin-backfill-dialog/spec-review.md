# Spec Review: 003-admin-backfill-dialog

## Verdict

approved

## Missing Requirements

- No implementation requirement is missing from the scoped admin diff. The
  typed API, status and permission-gated row action, bounded responsive dialog,
  existing logistics defaults, keyboard fields, validation, loading guard,
  local-only warning, error-preserving draft, success result, list refresh, and
  both locale dictionaries are present.
- The carrier-code field uses the existing `rental.xianyu.expressCode` key, and
  the earlier missing-key finding is no longer applicable.

## Extra Behavior

- No scanner or camera API, direct database access, frontend inventory or
  schedule calculation, new HTTP client, new permission, or remote XianGuanJia
  write is present.
- The page/dialog split stays within the approved component boundary: the page
  owns visibility and refresh, while the dialog owns form state and submit
  behavior.

## Misunderstood Requirements

- The row action combines uncancelled status `21`/`22` with the existing
  `v-hasPermi` directive; frontend hiding is not being used as the backend
  authorization boundary.
- The dialog width uses `min(620px, calc(100vw - 32px))`, and the order status
  uses the existing localized rental-label helper rather than exposing raw
  numeric status codes.
- The approved prototype is a design/input contract, not production E2E or
  sensory evidence.

## Cannot Verify From Diff

- Signed receipts `receipt-cfa37b...3856`, `receipt-e3c918...d168`, and
  `receipt-52c20b...f063` are current-head `system-executed` passes for
  Vue type checking, targeted ESLint, and targeted Prettier. They do not prove
  browser runtime behavior.
- No production browser E2E or sensory receipt proves permission denial,
  keyboard submission against a live backend, draft preservation after a
  backend failure, success refresh, or the full light/dark x `zh-CN`/`en`
  desktop/narrow matrix. The handoff correctly assigns those checks to
  Verification 2.0.

## Acceptance Assertions Verified

- A1: the source gates the row action to uncancelled status `21`/`22` and
  reuses `rental:xianyu:ship`; live permission/E2E behavior remains follow-on.
- A2: the dialog exposes keyboard-entered device, waybill, carrier, ship-time,
  and reason fields without scanner integration.
- A3: both locale dictionaries provide the explicit local-only/no-remote-write
  warning.
- A13: the dialog submits through the typed API, emits completion, closes only
  after success, and the page refreshes the list; live E2E remains follow-on.
- A14: the source provides responsive width, locale keys, and theme-compatible
  existing Element Plus primitives; sensory usability remains follow-on.

## Required Fixes

- None for this development UI slice. Verification 2.0 must still execute the
  production E2E and sensory matrix rather than promoting static checks or the
  prototype as runtime proof.
