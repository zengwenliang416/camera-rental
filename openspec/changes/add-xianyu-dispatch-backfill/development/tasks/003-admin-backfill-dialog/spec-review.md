# Spec Review: 003-admin-backfill-dialog

## Verdict

needs-fix

## Missing Requirements

- The production dialog uses a fixed `width="620px"` rather than the approved
  bounded responsive width (`min(620px, calc(100vw - 32px))`
  contract). This can overflow or make the form unusable on the required
  narrow layout (`XianyuDispatchBackfillDialog.vue:2-7`).
- The order context renders the raw numeric status instead of the existing
  localized rental-label formatter (`XianyuDispatchBackfillDialog.vue:16-22`).
  This does not meet the readable `zh-CN`/`en` status requirement.

## Extra Behavior

- No out-of-scope browser API, scanner integration, direct database access, or
  remote XianGuanJia call is present. The API, row action, dialog, and locale
  additions stay within the approved component boundary.

## Misunderstood Requirements

- The fixed dialog width treats the prototype's desktop width as a literal
  size instead of preserving its narrow-screen constraint.
- Showing `21`/`22` directly treats an internal status code as user-facing
  copy, bypassing the page's established localized status presentation.

## Cannot Verify From Diff

- The row condition and `v-hasPermi` directive statically cover the intended
  status/permission gate (`index.vue:250-259`, `index.vue:556-560`), and the
  completion handler calls `getList` (`index.vue:567-574`), but no authorized
  or unauthorized browser E2E receipt exists.
- The dialog contains fields, validation, loading, warning, and success/error
  paths, but no current browser/sensory evidence proves draft preservation,
  keyboard flow, light/dark contrast, `zh-CN`/`en` rendering, conflict/error
  states, or narrow-layout usability.
- The validation log entries for Vue type checking, ESLint, and Prettier are
  self-reported. The current attempts could not produce trusted green
  receipts because dependency tarball policy blocked installation and the
  local binaries were unavailable after the failed setup.

## Acceptance Assertions Verified

- A1, A2, A3, and A13 have only partial static support:
  the action is status/permission gated, the dialog has keyboard-entered
  fields and the explicit local-only warning, and success emits completion
  for list refresh (`index.vue:250-259`, `XianyuDispatchBackfillDialog.vue:9-14`,
  `XianyuDispatchBackfillDialog.vue:155-179`).
- A14 is not verified and is contradicted by the fixed-width implementation.
- A12 is not verified for the admin checks; the typed API and component source
  are present, but no authoritative type/lint/format receipt is available.

## Required Fixes

- Make the dialog width responsive under the approved narrow-layout contract.
- Render the order status through the existing localized rental-label helper.
- Re-run and record authoritative `vue-tsc`, targeted ESLint, targeted
  Prettier, and `git diff --check` results after the final UI changes.
- Execute the required browser and sensory matrix for permission, validation,
  loading, conflict, error, success, locale, theme, desktop, and narrow
  states; do not use the prototype artifact as production E2E evidence.
- Keep the draft on backend failure and verify that behavior through the real
  page/network path rather than only source inspection.
