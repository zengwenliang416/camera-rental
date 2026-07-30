# Quality Review: 006-auth-exceptions-overlays

## Verdict

approved

## Separation Of Concerns

- Authentication validation, safe-error classification, exception read
  models, QR state, device presentation, permissions, workspace overlay state,
  and shared shells are separated from raw transport.
- `useQuickBindingWorkspace` owns quick-binding route/modal/preselection
  lifecycle. Orders and dashboard emit one intent instead of coordinating
  route and modal state themselves.
- Shared dialog, drawer, permission-action, and operation-result components
  have behavior-facing inputs and do not import API clients, feature contexts,
  or page-specific state.

## Component Cohesion / Coupling

- `QuickBindingModal` is a focused compatibility wrapper around
  `ShippingWorkbench` and `ConfirmDialogShell`; close behavior delegates to one
  cleanup intent.
- `DeviceDetailDrawer` reuses `deviceCardPresentation`, so availability and
  warehouse semantics are shared with the device list rather than coupled to
  mapper-created Chinese display fragments.
- Order-specific quick binding preserves its current route, while generic
  shipping navigation is explicitly represented by `openQuickBinding(null)`.
  The distinction is localized and covered through the hook's public behavior.

## Test Quality

- Independently rerun the focused auth, exception, device, QR, safe-error, and
  overlay command: 14 tests passed, 0 failed.
- Independently rerun `pnpm test`: 64 tests passed, 0 failed.
- The mounted happy-dom tests execute real React hook, dialog, and drawer
  behavior. They verify retained route state, preselected-order state, initial
  focus, both Tab boundaries, Escape, explicit close, focus events on the
  connected trigger, modal removal, generic shipping navigation, and drawer
  focus lifecycle.
- The pure `overlayKeyAction` test separately covers Escape and both focus-wrap
  decisions without depending on component source text.

## Error Handling

- Login, review, QR, permission, network, timeout, and unknown failures map to
  stable localized categories rather than raw transport messages.
- Login, review, QR, and command pending states retain duplicate-submission
  protection. QR success requires `signed === true`, `payloadVersion ===
  "CRD1"`, and a `CRD1|` payload before rendering.
- Escape and explicit modal close use the same cleanup path, clearing modal
  state and preselection before focus restoration.

## Reuse / Duplication

- Shared overlay key behavior is reused by dialog and drawer shells, with
  mounted regression coverage for both.
- Device presentation, permission actions, operation results, status badges,
  identifiers, date ranges, empty states, and the shipping workbench are reused
  rather than duplicated.
- `happy-dom` is test-only and provides behavior coverage that the prior source
  regex assertions could not supply.

## Complexity Delta

- Independently rerun `pnpm lint` and `pnpm build`; TypeScript no-emit and the
  Vite production build both exited 0. Quick-binding modal, shipping workbench,
  exceptions, device detail/QR, schedule, and allocation remain separate lazy
  chunks.
- Repository `git diff --check` passed. No production TypeScript, TSX, or CSS
  file exceeds 600 physical lines; `messages.ts` is the largest at 599 lines.
- Shared UI forbidden-import, QR/private-persistence, and `._*` metadata scans
  passed. The workspace hook is 44 lines and reduces orchestration coupling
  without adding runtime state libraries or production dependencies.

## Required Fixes

- No blocking quality fixes remain for task 006 before development handoff.
