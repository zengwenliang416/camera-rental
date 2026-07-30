# Task Report: 006-auth-exceptions-overlays

## Status

DONE

## Files Changed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/src/components/LoginPage.tsx`
- `camera-rental-schedule-center/src/components/ExceptionsView.tsx`
- `camera-rental-schedule-center/src/components/DeviceDetailDrawer.tsx`
- `camera-rental-schedule-center/src/components/QuickBindingModal.tsx`
- `camera-rental-schedule-center/src/features/auth/**`
- `camera-rental-schedule-center/src/features/exceptions/**`
- `camera-rental-schedule-center/src/features/devices/deviceQrModel.ts`
- `camera-rental-schedule-center/src/features/devices/deviceQrModel.test.ts`
- `camera-rental-schedule-center/src/features/devices/useDeviceQr.ts`
- `camera-rental-schedule-center/src/features/devices/components/DeviceDetailDrawer.tsx`
- `camera-rental-schedule-center/src/features/devices/components/DeviceQrPanel.tsx`
- `camera-rental-schedule-center/src/shared/ui/DetailDrawerShell.tsx`
- `camera-rental-schedule-center/src/shared/ui/PermissionAwareAction.tsx`
- `camera-rental-schedule-center/src/shared/ui/OperationResultPanel.tsx`
- `camera-rental-schedule-center/src/features/preferences/messages.ts`

## What Changed

- Replaced the legacy login implementation with a responsive unified admin
  password-login surface. SMS and QR methods remain visibly disabled and no new
  authentication endpoint, storage key, or login method was introduced.
- Login validation and authentication failures now expose only localized safe
  categories. The modal traps focus, closes on Escape, and returns focus to the
  persistent account trigger when the originating menu item has unmounted.
- Replaced the legacy exceptions page with focused filters, severity/status
  presentation, permission-aware intents, and duplicate-resolution protection.
  The UI never displays success because the current command seam does not
  reliably expose a typed success/failure result.
- Replaced the legacy device drawer with a focus-managed, read-only server
  history surface. Removed direct frontend device-status mutation controls and
  the unsupported force-idle action.
- Added a QR hook and read model that render only signed `CRD1` responses.
  Unsigned, malformed, forbidden, loading, and failed states use safe localized
  presentation, and the payload is not copied to URLs, storage, analytics, or
  logs.
- Wrapped the existing shipping workbench in the shared focus-managed
  confirmation dialog without changing its workflow or private search state.
  The authenticated app now mounts this compatibility overlay, and an existing
  permission-gated order-binding intent opens it over the originating route
  without rendering a duplicate shipping workbench behind the dialog. Generic
  shipping navigation still opens the full shipping route.
- Centralized quick-binding open, close, route, and preselection behavior in a
  focused workspace hook. Order-specific modal entry preserves its orders or
  dashboard route, while close clears transient state and restores the
  connected trigger.
- Reused the locale-neutral device presentation model in the detail drawer, so
  mapper-created Chinese availability and warehouse prefixes do not leak into
  the English surface.
- Raised the password-visibility action to a 44 by 44 CSS pixel target.
- Added complete `zh-CN` and `en` copy while keeping the preference dictionary
  below the 600-line production ceiling.

## TDD Evidence

- Added login validation and safe error-presentation tests.
- Added signed/unsigned QR response-state tests.
- Retained focused exception filtering, severity, and permission-action tests.
- Added deterministic shared-overlay tests for Escape, Tab/Shift+Tab wrapping,
  actual DOM focus restoration, originating-route retention, quick-binding
  reachability, explicit close, and transient-state cleanup.
- The full schedule-center suite contains 64 passing tests.

## Verification Commands

- `pnpm test`: 64 tests passed, 0 failed.
- `pnpm lint`: `tsc --noEmit` exited 0.
- `pnpm build`: Vite production build exited 0 and retained separate lazy
  chunks for exceptions, device detail/QR, schedule, allocation, and shipping.
- `git diff --check`: exited 0.
- Production line-ceiling scan: no TypeScript, TSX, or CSS file exceeded 600
  physical lines; `messages.ts` is 599 lines.
- Browser login checks at 1440, 768, 390, and 360 CSS pixels: no page-level
  horizontal overflow, username received initial focus, SMS/QR methods were
  disabled, and no raw translation keys rendered.
- Browser modal check: Escape dismissed the login dialog and returned focus to
  the account trigger.
- Browser exception checks: `zh-CN` and `en` empty states rendered complete
  copy with no raw keys or page-level overflow.

## Concerns

- The current local management snapshot contains zero devices and zero manual
  reviews. Device drawer/QR success, populated exception cards, and real review
  resolution could not be exercised without fabricating or writing business
  data.
- The compatibility surface intentionally avoids a review success toast when
  the backend command result is not independently observable.

## Scope Deviations

- Added `happy-dom` as a development-only dependency for mounted overlay
  interaction tests. No runtime dependency, API, permission, QR format,
  authentication contract, device transition, review mutation, or persistence
  behavior changed.

## Follow-up Needed

- Run populated drawer, signed QR, and exception-resolution scenarios only
  when controlled tenant data or a dedicated mocked browser environment is
  available.

## Adjudication

No unresolved implementation blocker. Independent spec and quality review are
required before marking task 006 complete.
