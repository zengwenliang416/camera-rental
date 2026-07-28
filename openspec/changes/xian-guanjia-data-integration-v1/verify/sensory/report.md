# Sensory Report

## Domain

sensory

## Verdict

green

## Inputs Reviewed

- Current worktree diff for `camera-rental-schedule-center`.
- Approved user test cases, signoff, and domain-case mapping.
- Active shipping, order, context, mapper, and mapper-test sources.
- `verify/e2e/artifacts/local-schedule-center-regression-20260728.json`.
- Existing backend/frontend evidence and an independent read-only browser
  probe against `http://localhost:5175/` on 2026-07-28.

## Evidence

- Direct entry had no selected order, showed `未选择`, kept `确认发货`
  disabled, and displayed `服务器已关闭闲管家写操作`.
- `QuickBindingView.tsx:102-115` accepts only an explicit order handoff and
  does not select the first shippable order.
- Test waybill `SENSORY-LOCAL-20260728` remained unchanged after selecting
  order `5126403888047003436` and changing the carrier from SF Express to JD
  Express. The confirmation button remained disabled.
- `QuickBindingView.tsx:126-138` changes only order/device selection;
  `QuickBindingView.tsx:584-610` keeps carrier and waybill in separate state.
- `QuickBindingView.tsx:140-156` exposes the write-gate reason and includes it
  in the submit blocker. `AppContext.tsx:323-359` repeats permission,
  integration, credential, write-enabled, and waybill checks before an API
  call.
- Order `5126359104930006425` rendered as `租赁中`, with rental period
  `2026-07-29 至 2026-08-05`, recipient `杨*`, phone `188****0175`, and no
  visible full phone number. The browser probe recorded no console errors.
- `mappers.ts:224-244` gives shipped/completed channel states precedence over
  stale conversion-review state. `mappers.ts:157-175` uses backend billable
  period fields. `mappers.ts:255-265` masks recipient name and phone.
- The structured local regression artifact confirms no real ship call,
  deployment, or GitHub push.

## Commands Run

- `npm test` in `camera-rental-schedule-center`: 8 tests passed.
- `npm run lint` in `camera-rental-schedule-center`: TypeScript check passed.
- Independent read-only browser inspection against `http://localhost:5175/`;
  only local input/select state changed and `确认发货` was never invoked.

## Findings

- No blocker was found for the reviewed device-shipping and order-display
  behaviors.
- Device and order selection rows are clickable `div` elements without native
  keyboard semantics. This is a non-blocking accessibility follow-up.
- `QuickBindingView.tsx` is 853 lines and an unmounted 644-line
  `QuickBindingModal.tsx` remains, creating future divergence risk.
- When a receiver snapshot is absent, the mapper can fall back to a seller or
  buyer nickname outside the recipient masking helper. This is a non-blocking
  privacy-consistency follow-up.

## Required Fixes

- None for the 2026-07-28 local sensory gate.

## Residual Risk

- Real XianGuanJia shipment was intentionally not tested because writes were
  disabled and the user prohibited real shipment.
- No mobile viewport, screen-reader, or full keyboard-only certification was
  performed.
- No formal performance budget was measured for the large unpaginated orders
  view.

## Follow-up Domain Routing

- Sensory is green for the 2026-07-28 local schedule-center regression.
- Route keyboard semantics, component extraction/dead-surface cleanup, and
  fallback-identity masking to a later development slice.
