# Quality Review: 003-admin-backfill-dialog

## Verdict

needs-fix

## Separation Of Concerns

- The order page owns only action visibility, selected order state, and list
  refresh (`index.vue:250-288`, `index.vue:556-574`). The dialog owns draft
  state, validation, loading, the per-open idempotency key, and completion
  emission (`XianyuDispatchBackfillDialog.vue:72-180`).
- The typed API stays in `src/api/rental/xianyu.ts`; the dialog does not access
  a mapper, database, or remote XianGuanJia client.

## Component Cohesion / Coupling

- The single-use form is appropriately kept inside
  `XianyuDispatchBackfillDialog`, and the page reuses `v-hasPermi`, the existing
  order row, `useI18n`, `useMessage`, and Element Plus primitives.
- The production dialog hard-codes `width="620px"` (`XianyuDispatchBackfillDialog.vue:2`),
  while the approved prototype requires a bounded responsive width
  `min(620px, calc(100vw - 32px))` (`prototype/artifact/styles.css:124-126`).
  On a narrow viewport this can overflow or make the form unusable.

## Test Quality

- The source visibly implements a loading guard, server-success close/refresh,
  and draft preservation on rejected requests because the dialog is not closed
  in the failure path (`XianyuDispatchBackfillDialog.vue:155-179`).
- No production-dialog unit, E2E, or sensory evidence is recorded in the task
  validation log. Prototype screenshots are not evidence that the Vue
  component passes the required type, lint, format, and narrow-layout checks.
- The attempted pnpm checks could not be accepted as current green evidence:
  the initial non-interactive runs aborted during dependency cleanup, and the
  task's `validation-log.jsonl` has no system-executed frontend receipts.

## Error Handling

- Backend errors are allowed to reject the API promise and the `finally` block
  only clears `submitting`, so the dialog remains open with its draft. This is
  compatible with the stated preservation behavior and the repository's global
  Axios error presentation.
- The dialog displays the raw numeric order status (`order.orderStatus` at
  `XianyuDispatchBackfillDialog.vue:20-21`) instead of the existing localized
  `rentalLabel('channelOrder', ...)` path used by the table. This weakens
  zh-CN/en readability and makes the context inconsistent with the surrounding
  page.

## Reuse / Duplication

- The API type and response reuse are correct, and no second HTTP client or
  shared form abstraction was introduced. The page action reuses the existing
  permission code rather than inventing a new permission.

## Complexity Delta

- The UI delta is small and localized: one typed API method, one bounded dialog,
  one row action, and locale entries. The action-column width increase is
  contained, but the fixed dialog width adds avoidable responsive risk.

## Required Fixes

- Make the dialog width responsive to the approved prototype contract and
  verify it at the narrow viewport.
- Render the order status through the existing localized rental label formatter.
- Run and record `pnpm ts:check`, targeted ESLint, targeted Prettier, and the
  required browser/sensory states after the final component changes.
- Populate the task report and validation evidence before treating the dialog
  task as complete.
