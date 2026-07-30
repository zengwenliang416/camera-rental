# Prototype Handoff: schedule-center-local-stability-v1

## Approved Branch Variant

- Branch: `ui-html`
- Variant: `integrated-rental-operations-v6-outbound-command-center`
- Approval status: **APPROVED** on `2026-07-29`. The user reviewed the
  production-calibrated shipping prototype, requested the final device and
  order search behavior, then explicitly asked to start frontend
  transformation.

## Screens Or Flows

- Unified management login and same-origin session reuse.
- Today workbench with synchronization health, registered-asset boundary,
  operational metrics, work queues, and urgent actions.
- SN-level Gantt scheduling with separate billable and occupied ranges.
- Registered-device availability matrix and shortage explanation.
- Rental-order list and detail.
- Device ledger, import-field guidance, QR, maintenance, and detail drawer.
- Outbound flow: `waybill -> device -> pending-order search -> confirm binding
  -> ship`.
- Exception and manual-review workbench.
- Responsive desktop, tablet, mobile, light/dark, `zh-CN`/`en`, and explicit
  loading, empty, error, permission, conflict, disabled, and populated states.

## Components To Create

- `ScheduleCenterAppShell`, `ResponsiveWorkspaceNavigation`,
  `AccountAndPreferenceMenu`, `ThemeToggle`, and `LocaleToggle`.
- `SyncHealthBanner`, `OperationalMetricGrid`, `OperationalMetricCard`,
  `FeaturePageHeader`, and `FilterToolbar`.
- `DataStateBoundary`, `LoadingState`, `EmptyState`, `PermissionState`,
  `ErrorRetryState`, and `ConflictState`.
- `StatusBadge`, `IdentifierText`, `DateRangeDisplay`,
  `BillableOccupiedRangeLegend`, and `ResponsiveDataList`.
- `DetailDrawerShell`, `ConfirmDialogShell`, `PermissionAwareAction`, and
  `OperationResultPanel`.
- Shipping components: `ShippingWorkflowStepper`, `WaybillReviewPanel`,
  `DeviceSelectionPanel`, `DeviceIdentitySearch`,
  `PendingOrderSelectionPanel`, `AuthorizedOrderIdentitySearch`,
  `CompleteOrderResultSchema`, and `ShipmentConfirmationPanel`.
- Feature hooks and adapters listed by `component-impact-map.json`, including
  focused query hooks, command hooks, stale-request suppression, safe error
  mapping, and typed read models.

## Components To Reuse

- Existing React 19, Vite, Tailwind 4, `lucide-react`, `motion`,
  `qrcode.react`, and `jsqr` dependencies.
- Existing token refresh, tenant header, session storage, permission bootstrap,
  and common result handling in `src/api`.
- Existing rental API contracts and mapper tests.
- Existing assignment, dispatch, return, review, QR, OCR, pending-shipment, and
  shipment-confirmation backend endpoints.

## Extraction Targets

- Split `AppContext` into session, permissions, preferences, navigation/
  overlays, feature queries, and commands.
- Split `QuickBindingView` and `QuickBindingModal` into shipment workflow
  panels, hooks, typed adapters, and intent-only presentational components.
- Split `GanttScheduleView` into query/controller, filters, date grid, lane,
  range, legend, and interaction modules.
- Extract shared async-state, status, identifier, date/range, money, private
  contact, filter, list, drawer, dialog, and permission-action behavior.
- Remove active `._*` macOS metadata files from the source tree.

## API Contracts

- Preserve all routes listed in `spec-map.json`, including authentication,
  permission info, XianGuanJia config and express companies, devices,
  schedules, channel orders, pending-shipment orders, manual reviews, QR,
  OCR, shipment, assignment, dispatch, return, and review commands.
- No route, request, response, permission code, tenant behavior, or backend
  state machine changes are approved by this frontend handoff.
- Pending-shipment search accepts receiver name, full receiver phone, or order
  number through the existing endpoint. Full receiver/order values are shown
  only when the backend returns them to an authorized shipment operator.

## Data Flows

- `FLOW-SCHEDULE-CENTER-SESSION-BOOTSTRAP`
- `FLOW-SCHEDULE-CENTER-PERMISSION-BOOTSTRAP`
- `FLOW-SCHEDULE-CENTER-DASHBOARD-QUERY`
- `FLOW-SCHEDULE-CENTER-GANTT-QUERY`
- `FLOW-SCHEDULE-CENTER-ORDER-QUERY`
- `FLOW-SCHEDULE-CENTER-DEVICE-QUERY`
- `FLOW-SCHEDULE-CENTER-SHIPMENT-REVIEW`
- `FLOW-DEVICE-ASSIGNMENT`
- `FLOW-DEVICE-HANDOVER`
- `FLOW-SCHEDULE-CENTER-MANUAL-REVIEW`
- `FLOW-SCHEDULE-CENTER-SYNC-RETRY`
- `FLOW-SCHEDULE-CENTER-PREFERENCE-PERSISTENCE`

Queries map API VOs into typed feature read models. Commands are explicit,
non-optimistic, guarded against duplicate submission, and followed by server
state refresh. Shipping search text and private results remain ephemeral and
never enter URL state, browser persistence, analytics, or client logs.

## State Behavior

- Loading: retain route/filter/draft context and show a scoped loading state.
- Empty: distinguish genuine empty data from permission, sync, and filter
  results without oversized blank panels.
- Error: classify safe localized categories; never render raw exceptions.
- Disabled: keep read-only operation available while clearly disabling writes.
- Permission: hide or disable intent controls, with backend authorization
  remaining authoritative.
- Conflict: show server-provided scheduling or stale-state conflicts and
  require refresh/review before retry.
- Success: wait for server acceptance, display the operation result, and
  invalidate affected server queries.

## Theme And Locale Policy

- Theme support: `light-dark`; system preference is initial-only.
- Theme modes shown in prototype: light and dark.
- Theme toggle: present, keyboard accessible, and persisted.
- Internationalization: enabled.
- Locales shown in prototype: `zh-CN` and `en`.
- Default/fallback locale: `zh-CN`.
- Locale switcher: present, keyboard accessible, and persisted.

## Out Of Scope Items

- Backend, database, API contract, permission, tenant, authentication, or
  business state-machine changes.
- New scheduling, pricing, automatic assignment, automatic dispatch, or OCR
  auto-acceptance behavior.
- Changes to admin, staff, uni-app, or customer web repositories.
- A second client-side source of truth for server records.
- Unmasked private data outside the explicit, permission-gated shipment lookup.
- Production deployment, database writes, or real shipment submission during
  frontend development and automated testing.

## Required Tests

- `bun run lint`, `bun run test`, and `bun run build`.
- Automated source-file line ceiling and forbidden-import checks.
- Adapter, formatting, error classification, permission-action, query
  cancellation, command submit-guard, and preference persistence tests.
- Shipping device search, authorized order search, OCR-draft preservation,
  workflow step gating, and duplicate-submit tests.
- Browser review at 1440x900, 1087x900, 768x1024, 390x844, and 360x800 with no
  page-level horizontal overflow.
- Light/dark, `zh-CN`/`en`, loading, empty, error, permission, conflict,
  disabled, dialog, drawer, Gantt, QR, and shipping review.
- Private-data, raw-error, invented-data, URL, storage, and client-log leakage
  checks.

## Open Risks

- Existing components combine transport, derived metrics, workflow state, and
  presentation; implementation must proceed in vertical slices and preserve
  behavior while extracting boundaries.
- The current backend may not return every receiver/order field required by the
  approved shipping result schema. Frontend must display only returned fields
  and surface a safe incomplete-data state rather than inventing or deriving
  values.
- Production counts can change independently of this masked prototype.
  Implementation and tests must use controlled API responses and never hardcode
  prototype business totals into production source.
- The pending-shipment endpoint and backend permission remain authoritative for
  unmasked receiver visibility. Frontend presentation is not a security
  boundary.
