# Task Brief: 006-auth-exceptions-overlays

## Goal

A user can sign in with the existing management account, review and resolve
permitted exceptions, inspect device identity and signed QR state, and operate
dialogs and drawers without losing keyboard focus or route context.

## Parent Artifacts

- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`

## Vertical Slice

Enter through the management login page or an expired-session overlay, retain
the existing session contract, open the manual-review route, inspect related
order/device context, execute the existing permission-gated review command,
and inspect device QR/detail or embedded shipping surfaces through accessible
overlay shells.

## In Scope

- Redesign the login page for responsive light/dark and `zh-CN`/`en` use while
  preserving the existing management password-login contract and storage keys.
- Present unsupported SMS and QR login methods as disabled information, not
  active authentication flows.
- Split exception filtering, severity/status presentation, permitted intents,
  and result states into a focused exceptions feature.
- Replace raw review and QR errors with safe localized categories.
- Split device detail, signed QR loading, lifecycle ranges, and permitted
  device actions into focused components and hooks.
- Add reusable focus-managed drawer, permission-action, and operation-result
  presentation.
- Wrap the embedded shipping workbench in the same accessible modal behavior
  without changing its approved workflow or private-data boundary.
- Add focused tests for login validation, exception read models, permission
  action state, QR async state, and overlay focus/dismissal helpers.

## Out Of Scope

- New login methods, authentication endpoints, permissions, QR payload format,
  review commands, device status transitions, shipping behavior, APIs,
  database changes, or global state decomposition.
- Persisting credentials, private customer data, QR payloads, review drafts, or
  overlay state in URLs or browser storage.

## Files Allowed

- `camera-rental-schedule-center/package.json`
- `camera-rental-schedule-center/src/App.tsx`
- `camera-rental-schedule-center/src/components/LoginPage.tsx`
- `camera-rental-schedule-center/src/components/ExceptionsView.tsx`
- `camera-rental-schedule-center/src/components/DeviceDetailDrawer.tsx`
- `camera-rental-schedule-center/src/components/QuickBindingModal.tsx`
- `camera-rental-schedule-center/src/features/auth/**`
- `camera-rental-schedule-center/src/features/exceptions/**`
- `camera-rental-schedule-center/src/features/devices/components/DeviceDetailDrawer.tsx`
- `camera-rental-schedule-center/src/features/devices/components/DeviceQrPanel.tsx`
- `camera-rental-schedule-center/src/features/devices/deviceQrModel.ts`
- `camera-rental-schedule-center/src/features/devices/deviceQrModel.test.ts`
- `camera-rental-schedule-center/src/features/devices/useDeviceQr.ts`
- `camera-rental-schedule-center/src/shared/ui/DetailDrawerShell.tsx`
- `camera-rental-schedule-center/src/shared/ui/PermissionAwareAction.tsx`
- `camera-rental-schedule-center/src/shared/ui/OperationResultPanel.tsx`
- `camera-rental-schedule-center/src/features/preferences/messages.ts`
- `camera-rental-schedule-center/src/shared/lib/safeError.ts`
- `camera-rental-schedule-center/src/index.css`

## Interfaces / Seams

- `useApp()` remains the session, permission, query, command, navigation, and
  overlay seam until task 007.
- Authentication and QR hooks may import existing typed services; reusable
  overlay and presentational components receive read models and intents only.
- Exception and device-detail pages emit existing allocation, shipping,
  review, close, and navigation intents without calling raw transport.

## Components To Create

- `UnifiedAdminLogin`
- `LoginCredentialForm`
- `ExceptionsPage`
- `ExceptionCard`
- `DeviceDetailDrawer`
- `DeviceQrPanel`
- `DetailDrawerShell`
- `PermissionAwareAction`
- `OperationResultPanel`

## Components To Reuse

- Existing management login, permission, review, QR, device, shipping, and
  assignment contracts.
- Existing semantic tokens, preferences, feature header, status badge, empty
  state, date-range display, responsive list, and confirmation dialog.

## Components To Extract

- Login field validation and safe authentication presentation.
- Exception filtering, severity/status, and intent readiness.
- QR loading/success/error state.
- Overlay focus trap, Escape dismissal, initial focus, and focus return.

## API / Data Flow Contracts

- Preserve `/system/tenant/get-id-by-name`, `/system/auth/login`,
  `/system/auth/logout`, `/rental/manual-review/resolve`,
  `/rental/device/get-qr`, and all existing session/tenant storage keys.
- QR and review success appear only after the existing backend response.
- Signed QR payload is render-only and is never copied into storage, logs,
  analytics, or URL state.
- The embedded shipping workbench continues to own its approved ephemeral
  private search state and server-authoritative confirmation gates.

## State / Error / Empty / Loading Behavior

- Loading: disable duplicate login, review, and QR commands while retaining the
  current overlay and route.
- Empty: distinguish no open review from no resolved history and no device
  schedule history.
- Error: show safe authentication, permission, network, timeout, QR, and
  command categories without raw transport text.
- Disabled: explain unsupported login methods, missing QR permission, and
  unavailable review/device actions.
- Permission: hide or disable actions according to existing permissions while
  preserving backend authorization.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `git diff --check`
- Production source line-ceiling and forbidden-import scans.
- Browser login, exceptions, drawer, QR, and modal checks at 1440, 768, 390,
  and 360 CSS pixels with keyboard and focus verification.

## Stop Conditions

- Scope lock mismatch.
- Required behavior needs a new login method, API, permission, QR format,
  review mutation, or device state transition.
- A shared overlay or presentational component would import transport.

## Unsafe Assumptions

- A visible login tab means that authentication method exists.
- Frontend permission state replaces backend authorization.
- QR retrieval success proves device availability or shipment readiness.
- Closing an overlay without focus return is acceptable.
- Raw backend review or authentication messages are safe visible copy.
