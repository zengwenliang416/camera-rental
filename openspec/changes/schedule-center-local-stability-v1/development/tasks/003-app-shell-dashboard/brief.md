# Task Brief: 003-app-shell-dashboard

## Goal

An operator can enter the schedule center, understand synchronization and
write-readiness state, navigate every permitted module without horizontal
overflow, and act on today's server-derived work queues from a coherent
responsive dashboard.

## Parent Artifacts

- `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`

## Vertical Slice

Bootstrap the existing session and server snapshot, render the responsive
application shell, allow the user to persist light/dark and `zh-CN`/`en`
preferences, show safe synchronization state, and present task-oriented
dashboard metrics and queues that route to the existing server-authoritative
flows.

## In Scope

- Introduce the approved semantic token layer for the application shell and
  dashboard.
- Replace the tablet/mobile overflowing navigation row with an accessible
  compact menu.
- Add focused preference state and safe persistence for theme and locale only.
- Add a grouped account/preference menu, synchronization control, and
  integration write-readiness indicator.
- Extract safe error classification, feature page header, sync health, metric,
  status, and empty-state presentation.
- Derive registered-device, order, maintenance, and review metrics from the
  current mapped server snapshot.
- Redesign the dashboard around synchronization health, operational metrics,
  urgent queues, and the registered-asset data boundary.
- Lazy-load Gantt, orders, devices, shipping, exceptions, and global overlays
  outside the initial dashboard path.
- Add pure tests for dashboard read-model and preference behavior.

## Out Of Scope

- Gantt internals, order list/detail, device ledger/detail, exception internals,
  login-page redesign, and shipping-workbench internals.
- New APIs, server-derived metrics, persistence tables, permissions, business
  commands, scheduling algorithms, or invented production totals.
- Full translation of legacy feature pages; this slice localizes the new shell,
  shared states, and dashboard foundation used by later slices.

## Files Allowed

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

## Interfaces / Seams

- Existing `useApp()` remains the server snapshot, permission, navigation, and
  command seam during this slice.
- `PreferenceProvider` owns only approved stable preferences and has no server
  record dependency.
- Dashboard adapters consume `RentalOrder`, `DeviceInstance`, and
  `ExceptionItem` read models and emit view-only metrics and queue slices.
- Shared presentational components receive values and intent callbacks only.

## Components To Create

- `ScheduleCenterAppShell`
- `ResponsiveWorkspaceNavigation`
- `AccountAndPreferenceMenu`
- `ThemeToggle`
- `LocaleToggle`
- `SyncHealthBanner`
- `FeaturePageHeader`
- `OperationalMetricGrid`
- `OperationalMetricCard`
- `StatusBadge`
- `EmptyState`
- `DashboardPage`

## Components To Reuse

- Existing React, Tailwind, `lucide-react`, session bootstrap, permission
  checks, mapped rental read models, and server refresh command.

## Components To Extract

- Safe visible synchronization error classification.
- Dashboard read-model derivation.
- Preference validation and storage adapter.
- Shared semantic data-state and operational metric presentation.

## API / Data Flow Contracts

- Preserve the existing permission bootstrap and
  `fetchScheduleCenterSnapshot()` flow.
- Split permission-scoped snapshot queries so one expected feature denial or
  partial query failure does not erase unrelated authorized data.
- No new request is introduced and no server record is persisted in browser
  storage.
- Theme and locale are the only new storage keys.
- Metrics and queues remain view-only derivations of mapped backend records.
- Navigation triggers existing feature routes and commands; no business write
  runs from a mount effect or optimistic update.

## State / Error / Empty / Loading Behavior

- Loading: keep the shell navigable and show scoped synchronization progress.
- Empty: explain that only registered server assets are counted and avoid
  oversized empty panels.
- Error: classify network, authentication, permission, and timeout failures
  into safe localized messages with permitted retry/login actions.
- Disabled: show read-only XianGuanJia state without implying writes are
  available.
- Permission: remove unavailable navigation and preserve the existing global
  access-denied state.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `pnpm test`
- `pnpm lint`
- `pnpm build`
- `git diff --check`
- Production TS/TSX/CSS line-ceiling scan.
- Browser checks at 1440, 768, 390, and 360 CSS pixels.

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- Do not treat registered-device count as the operator's total physical stock.
- Do not infer sync health merely from non-empty local arrays.
- Do not hardcode prototype totals, dates, order identities, or customer data.
- Do not claim legacy feature pages are translated or dark-mode complete in
  this slice.
