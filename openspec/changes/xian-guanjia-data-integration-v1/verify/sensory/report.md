# Domain Report: sensory

## Domain

sensory

## Verdict

green

## Inputs Reviewed

- requirements.md
- acceptance.md
- development/handoff-to-verify.md
- verify/command-results.md
- verify/sensory/reviewer-independence.md

## Evidence

- Admin routes, locales, and light/dark-compatible component usage pass static
  checks.
- A current Chrome DevTools Protocol pass on 2026-07-25 covered isolated admin
  `5176` against rebuilt backend `48086` with Quartz in standby.
- The pass covered Xianyu integration, channel orders, device schedules,
  manual review, sync-run history, and business reports across `zh-CN` / `en`
  and light / dark states. All 24 route/language/theme states settled on the
  target route, had no loading, denied, or error state, and all observed
  rental APIs returned HTTP 2xx.
- Representative screenshots were recorded with table bodies blurred under
  `verify/e2e/artifacts/2026-07-25T00-44-37-775Z-*`.

## Commands Run

- A temporary Chrome DevTools sensory/transition probe was attempted on
  2026-07-25 against local admin `5174` and backend `48080`, but Chrome exited
  after `DevTools listening` and before CDP target creation. No screenshot,
  DOM, transition, or sensory pass evidence is claimed.
- `pnpm ts:check`
- Chrome DevTools Protocol browser sensory/transition flow against isolated
  admin `5176` and backend `48086`.
- Manual screenshot inspection of the representative artifacts.

## Findings

- Visible admin desktop behavior is proven for the required rental V1 pages in
  the current isolated runtime.
- The report page initially exposed raw external product/SKU identifiers in
  visible table text; `externalProductId` and `externalSkuId` now use the
  shared `maskChannelIdentifier` display path, and the repeated browser pass
  found no visible mainland mobile-number or 10+ continuous-digit pattern.

## Required Fixes

- None for the desktop admin V1 smoke.

## Residual Risk

- This pass used a desktop `1440x1000` viewport. Mobile/responsive sensory
  checks remain follow-up evidence if explicit responsive certification is
  required.

## Follow-up Domain Routing

- Sensory is green for the current desktop admin V1 browser smoke.
