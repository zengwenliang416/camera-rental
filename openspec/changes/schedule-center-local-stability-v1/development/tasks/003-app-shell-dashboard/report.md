# Task Report: 003-app-shell-dashboard

## Status

DONE

## Files Changed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/src/App.tsx`
- `camera-rental-schedule-center/src/index.css`
- `camera-rental-schedule-center/src/components/Header.tsx`
- `camera-rental-schedule-center/src/components/DashboardView.tsx`
- `camera-rental-schedule-center/src/context/AppContext.tsx`
- `camera-rental-schedule-center/src/api/rental.ts`
- `camera-rental-schedule-center/src/api/snapshotLoader.ts`
- `camera-rental-schedule-center/src/api/snapshot.test.ts`
- `camera-rental-schedule-center/src/app/**`
- `camera-rental-schedule-center/src/features/dashboard/**`
- `camera-rental-schedule-center/src/features/preferences/**`
- `camera-rental-schedule-center/src/shared/**`
- Empty AppleDouble `._*` files removed from the active `src` tree.

## What Changed

- Replaced the desktop-row-on-mobile header with a responsive application shell
  and compact two-column/three-column navigation menu.
- Added persisted light/dark and `zh-CN`/`en` preferences without storing any
  server record or private query result.
- Added safe synchronization error classification, retry/login actions,
  integration write-readiness, and permission presentation.
- Centralized route permissions across navigation and dashboard intents, and
  return an invalid active route to the dashboard after permission refresh.
- Split snapshot reads by permission and failure boundary so an expected 403
  or feature query failure preserves successful authorized collections.
- Added explicit loading, ready, read-only, disabled, and unavailable
  XianGuanJia states instead of treating configuration failure as read-only.
- Added compact-menu Escape dismissal, initial focus, and trigger focus return.
- Removed the shared sync banner's API import and extracted theme, locale, and
  metric-card components.
- Rebuilt the dashboard around server-derived registered assets, operational
  metrics, work queues, urgent state, and the explicit asset-registration
  boundary.
- Removed direct return mutation from the dashboard; queue actions now enter
  existing review/assignment/shipping/detail flows.
- Lazy-loaded Gantt, orders, devices, shipping, exceptions, assignment dialog,
  and QR/detail drawer outside the initial dashboard chunk.

## TDD Evidence

- Added dashboard read-model tests before browser acceptance, including zero
  inventory and registered-device utilization behavior.
- Added preference validation/persistence tests for invalid stored values and
  approved keys only.
- Added safe error-category tests for network, authentication, permission,
  timeout, and unknown failures.

## Verification Commands

- `pnpm test`: 31 tests passed, 0 failed.
- `pnpm lint`: `tsc --noEmit` exited 0.
- `pnpm build`: Vite production build exited 0 and emitted separate feature
  chunks.
- `git diff --check`: exited 0.
- Production line-ceiling and AppleDouble scans: no source file exceeded 600
  lines and no active `._*` file remained.
- Browser checks at 1440x900, 768x1024, 390x844, and 360x800: no page-level
  horizontal overflow; XianGuanJia state remained visible and accessible at
  every width.
- Compact navigation and account menu checks: opening focuses the first item,
  Escape dismisses, and focus returns to the trigger.
- Browser theme/locale check: light/dark and `zh-CN`/`en` changed immediately
  and persisted after reload.

## Concerns

- The local backend was not running during browser review, so the implemented
  safe synchronization-failure and zero-snapshot states were exercised instead
  of a populated live snapshot.
- Legacy Gantt, order, device, exception, authentication, and overlay internals
  still require their own approved vertical slices for complete theme, locale,
  responsive, and architecture coverage.

## Scope Deviations

- Independent review required permission-scoped partial snapshot loading.
  Task scope was explicitly expanded to the existing frontend
  `AppContext`/snapshot orchestration files; no backend API or business
  contract changed.

## Follow-up Needed

- Continue with Gantt/allocation, order/device, exception/authentication, and
  final context decomposition/verification slices.
