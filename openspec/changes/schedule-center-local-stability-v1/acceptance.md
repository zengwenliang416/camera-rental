# Acceptance Criteria: schedule-center-local-stability-v1

## User-Visible Criteria

- All existing schedule-center views and overlays use one coherent semantic
  design system for typography, color, spacing, radius, elevation, focus,
  status, motion, and data identifiers.
- The application has no page-level horizontal overflow at 360, 390, 768, or
  1440 CSS pixel review widths. Dense tables and schedule regions use an
  intentional internal scroller or responsive alternate layout.
- Desktop navigation remains compact and task-oriented. Tablet and mobile
  navigation does not render the desktop navigation row off-screen and keeps
  primary tasks reachable with keyboard and touch.
- The dashboard prioritizes sync health, operational metrics, urgent actions,
  and current queues. Empty sections explain the state without oversized blank
  regions.
- Gantt and assignment surfaces visibly distinguish billable ranges from
  occupied ranges and never imply that frontend availability is authoritative.
- Order, device, shipping, and exception views share consistent filters,
  loading, result, selection, detail, pagination, empty, and retry behavior.
- Shipping preserves `waybill -> device -> pending-order search -> confirm
  binding -> ship`. OCR output is visibly a review draft and cannot trigger
  shipment without explicit confirmation.
- The shipping workbench can search by receiver name, full receiver phone, or
  order number. Only an operator who already has the required shipment
  permission can see complete receiver/order fields returned by the backend.
- The management order page returns complete receiver name, mobile, address,
  and seller remark to operators with `rental:xianyu:query`; it does not return
  raw detail JSON, goods JSON, payment numbers, credentials, or signatures.
- Network, permission, sync, conflict, disabled-write, and empty states have
  distinct localized presentations. Raw errors such as `Failed to fetch` are
  not displayed.
- Theme and locale controls are keyboard accessible, persist the user's choice,
  and apply to every route, dialog, drawer, toast, empty state, and error state.
- `zh-CN` is the default/fallback locale and English layouts contain no raw
  translation keys or required Chinese-only fragments.
- Light and dark themes retain readable text, visible focus, status contrast,
  and non-color status cues.
- All compact-layout actionable controls meet a minimum 44 CSS pixel target,
  dialogs/drawers manage focus, and reduced-motion preference removes
  non-essential animation.
- Customer-private data may appear in authorized management order and shipment
  views, but does not appear in ordinary exports, screenshots, fixtures, error
  messages, URLs, browser persistence, analytics, or client logs.
- An authorized operator can manage the tenant's XianGuanJia enabled state,
  base URL, AppKey, replacement AppSecret, webhook base URL, supported sync-job
  settings, and real-shipment write switch without editing a server
  configuration file or restarting the backend.
- Enabling real writes requires an explicit warning confirmation. Disabling
  integration or writes is always available to authorized operators.
- The management integration page uses a password-only replacement input for
  AppSecret, never displays the stored value, clears the field after submission,
  and shows only credential-presence status after refresh.

## System Criteria

- Authentication, local storage keys, tenant propagation, permission checks,
  deployment base path, and `/admin-api` routing remain authoritative.
- Every maintained frontend is adapted to the current backend contracts. The
  management order-page and pending-shipment response/query contracts are
  updated, `PUT /rental/xianyu/config/update` is added, and no legacy
  configuration field, environment-variable path, or compatibility adapter is
  retained.
- Server responses remain authoritative for orders, devices, schedules,
  assignments, status, amounts, utilization, sync state, and write enablement.
- Reads support cancellation or stale-result suppression where applicable.
  Business mutations show pending state, prevent duplicate submission, wait
  for server acceptance, and refresh affected server state.
- Assignment, dispatch, return, shipping, review, money, and schedule states are
  not optimistically accepted.
- The effective integration, credential, endpoint, webhook, synchronization,
  and shipment-write state is read from the current tenant's persisted
  `xianyu_application` configuration. Missing rows and `NULL` operational flags
  are disabled. A tenant cannot read or change another tenant's configuration.
- Enabling integration fails when required persisted credentials are missing or
  the base URL is invalid. Enabling writes fails when integration is disabled.
  The shipment service checks the persisted write switch before assignment,
  remote API invocation, device dispatch, or shipment persistence.
- Legacy `XGJ_ENABLED`, `XGJ_BASE_URL`, `XGJ_APP_KEY`, `XGJ_APP_SECRET`,
  `XGJ_WEBHOOK_BASE_URL`, `XGJ_TENANT_ID`, `XGJ_WRITE_ENABLED`, and XianGuanJia
  job properties are removed from application YAML and local startup scripts,
  are not read at runtime, and have no compatibility fallback. Only the generic
  database encryption master key remains outside the admin page.
- Feature routes can fail independently: one feature/query failure does not
  erase unrelated cached views or user filter/draft state.
- Gantt, QR, OCR/image, and shipping-only modules are lazy-loaded or otherwise
  excluded from the initial dashboard path.
- No new production dependency is added without a recorded inability to
  implement the behavior with the current stack.
- `pnpm lint`, `pnpm test`, and `pnpm build` pass.

## Data Criteria

- API VOs are mapped through typed adapters/read models before complex
  presentation; views do not depend on unbounded raw response objects.
- External identifiers remain strings; amounts remain integer cents until
  display formatting; dates use typed contract values and explicit range
  semantics.
- URL state contains only safe shareable navigation/filter state. Form drafts,
  temporary selections, and private values are not placed in URLs.
- Browser storage contains only the existing session/tenant data and approved
  theme/locale preferences; it does not persist a second source of truth for
  operational records.
- Migration 029 extends `xianyu_application` with base URL, AppKey, encrypted
  AppSecret, webhook base URL, write flag, and supported synchronization
  settings. Migration 030 physically removes the legacy
  `credential_reference` column. Migration 031 disables integration, writes,
  and jobs for unconfigured application rows. No secret is copied from the
  process environment.
- Empty data, sync failure, permission denial, stale data, schedule conflict,
  and disabled third-party write state are represented separately.

## Component Criteria

- Reusable components, hooks, utilities, or services named in
  `component-impact-map.json` are extracted instead of duplicated.
- Source follows the dependency direction `view -> feature hook/controller ->
  typed service -> transport`; adapters convert API VOs to feature read models.
- Authentication/session, permissions, preferences, navigation/overlay state,
  server queries, and mutations are not combined in one global Context.
- Query hooks and command hooks/functions are separated. Shared presentational
  components do not import API clients or page-specific state.
- `QuickBindingView`, `QuickBindingModal`, `AppContext`, and
  `GanttScheduleView` are decomposed into single-responsibility modules.
- No production TypeScript, TSX, or CSS source file exceeds 600 physical lines.
  Files above 450 lines have a documented responsibility review; newly
  extracted modules normally remain below 300 lines.
- Duplicate order/device filtering, metrics, status mapping, permission action,
  error, and mutation-lifecycle logic is removed in favor of shared hooks,
  utilities, or components.
- Shared components have behavior-facing props and intent events and can be
  tested without booting unrelated feature routes or the entire application.
- Empty `._*` metadata files are absent from the active source tree and build
  inputs.

## Verification Surfaces

- Facticity: compare every visible metric, status, identifier, date, contact,
  write-enabled state, and operation result with mocked or controlled API
  responses; prohibit invented sample business data.
- Static: TypeScript no-emit check, production build, import-boundary scan,
  translation-key completeness, no raw error/private-data scan, and automated
  600-line ceiling check for production TS/TSX/CSS files.
- Unit: adapters, money/date/status utilities, async-state reducers, permission
  action state, query cancellation, command submit guards, preference
  persistence, and extracted shared components.
- Redteam: unauthorized actions, tenant mismatch, disabled XianGuanJia writes,
  stale assignment, duplicate shipment submission, raw error leakage, private
  data leakage, malformed QR/OCR input, and unsupported locale/theme values.
- E2E: login/session reuse, dashboard sync error/retry, route navigation,
  Gantt empty/conflict states, order filtering/detail, device QR/detail,
  shipping review/confirmation gating, exception resolution gating, theme and
  locale persistence, keyboard navigation, and responsive widths.
- Sensory: independent desktop/tablet/mobile review of every feature in light
  and dark modes, representative `zh-CN` and `en`, plus loading, empty, error,
  permission, conflict, disabled, dialog, drawer, table/list, Gantt, QR, and
  shipping states. Review must confirm no horizontal page overflow, clipped
  required actions, inconsistent radii/status colors, or oversized empty
  regions.
- Configuration verification covers query-only users, authorized config
  operators, cross-tenant isolation, missing application rows, encrypted-secret
  replacement/preservation, missing credentials, invalid URLs, enable/disable
  transitions, dynamic read/write client configuration, webhook lookup,
  scheduled tenant execution, shipment denial before side effects, and absence
  of stored AppSecret in responses, logs, fixtures, URLs, and browser
  persistence.

## Unresolved Gaps

- None.
