# Requirements: schedule-center-local-stability-v1

## Summary

Comprehensively redesign and refactor the standalone
`camera-rental-schedule-center` React application so it becomes a coherent,
responsive, accessible, maintainable operations console. The change covers the
dashboard, Gantt schedule, rental orders, device ledger and maintenance,
shipping, exceptions, authentication, dialogs, drawers, and QR-code surfaces.

The current permission model, tenant behavior, authentication storage, and
rental business workflows remain authoritative. All maintained clients are
adapted to the current contracts; legacy XianGuanJia configuration fields,
environment-variable paths, and compatibility adapters are removed. The
management order APIs provide authorized operators with complete customer
contact snapshots and searchable pending-shipment data.

## Users & Actors

- Rental operations manager: monitors workload, schedule health, utilization,
  shipping readiness, fulfillment, and exceptions.
- Scheduler: reviews billable and occupied ranges and assigns a physical device
  without relying on client-computed availability.
- Warehouse operator: resolves a device, reviews the selected order and
  waybill, and confirms an authorized outbound operation.
- Device and maintenance operator: reviews device identity, lifecycle, current
  assignment, schedules, maintenance, and QR code.
- Integration operator: understands management-data sync health and retries
  permitted reads without seeing raw third-party failures or secrets.
- Administrator: uses the existing unified account, tenant, and permission
  model and can switch theme and locale.

## In Scope

- Redesign every existing route/view and shared overlay in
  `camera-rental-schedule-center`, including loading, empty, partial-error,
  permission, conflict, disabled, and successful states.
- Introduce one semantic design-token layer for color, typography, spacing,
  radius, elevation, motion, focus, and responsive breakpoints. Remove ad hoc
  page-specific visual systems and map status colors to the foundation spec.
- Provide a compact desktop application header and a mobile/tablet navigation
  pattern that never relies on an overflowing desktop navigation row.
- At widths from 360 CSS pixels upward, prevent page-level horizontal overflow.
  Tables, schedule lanes, and dense data regions may use an explicitly labeled
  internal scroller, responsive card/list alternative, or detail drawer.
- Preserve one dominant action per task region and move secondary actions,
  sync health, account controls, theme, and locale into clearly grouped
  controls.
- Make the dashboard information hierarchy task-oriented: synchronization
  health, key operational metrics, urgent actions, current work queues, and
  concise empty states. Empty panels must not create excessive unused height.
- Make Gantt scheduling distinguish billable and occupied ranges with text,
  legend, accessible status cues, and server-provided conflict information.
- Make order, device, shipping, and exception lists use consistent filter,
  result, selection, detail, pagination, loading, and retry patterns.
- Preserve the outbound sequence `waybill -> device -> pending-order search ->
  confirm binding -> ship`; OCR/image recognition remains a review draft and
  cannot automatically dispatch.
- In the shipping workbench only, allow an authorized operator to explicitly
  search pending-shipment orders by receiver name, full receiver phone, or
  order number. A matched result may show the complete receiver and order
  details returned by the backend for shipment verification. Private query
  values and results must not enter URL state, browser persistence, ordinary
  screenshots, analytics, or client logs.
- Return complete receiver name, receiver mobile, receiver address, and seller
  remark from the existing management order-page API to users who already have
  `rental:xianyu:query`. Preserve the existing `rental:xianyu:ship` requirement
  for pending-shipment search and shipment verification.
- Keep raw third-party payloads, goods blobs, payment numbers, credentials,
  signatures, and unrelated sensitive integration fields out of management
  order responses. Complete customer fields must not be copied into logs,
  ordinary exports, analytics, URLs, browser persistence, or test fixtures.
- Replace raw transport failures such as `Failed to fetch` with safe localized
  error categories, retained user state, and a permitted retry action.
- Add accessible light/dark and `zh-CN`/`en` switches with persisted user
  preferences. The default and fallback locale is `zh-CN`.
- Add keyboard-visible focus, semantic labels, non-color status cues, minimum
  44 CSS pixel touch targets on compact layouts, reduced-motion behavior, and
  focus management for dialogs and drawers.
- Split the application into `app`, feature, shared UI, domain/read-model,
  hook, and typed service boundaries. Feature areas include dashboard,
  schedule, orders, devices/maintenance, shipping, exceptions, authentication,
  and preferences.
- Apply container/presentational composition, query-command separation,
  adapter/read-model mapping, and explicit async-state modeling. Components
  emit user intent and do not import raw transport clients.
- Decompose `AppContext` so authentication/session, permissions, preferences,
  navigation/overlay state, server queries, and mutations do not share one
  global provider.
- Keep server records as invalidatable server state. Context/store state is
  limited to session, stable permissions/dictionaries, preferences, and truly
  shared UI state.
- Extract repeated status, money, date/range, device identity, customer-safe
  contact, sync health, filters, table/list, empty/error, dialog, drawer, and
  operation-result behavior.
- No production TypeScript, TSX, or CSS source file may exceed 600 physical
  lines. New and substantially refactored files should normally remain under
  300 lines; files approaching 450 lines trigger a responsibility review.
- Route-level or feature-level lazy loading must keep Gantt, QR generation,
  image/OCR, and shipping-only code out of the initial dashboard path when the
  runtime supports it.
- Expand automated coverage for read-model adapters, hooks, shared components,
  permission/action states, themes/locales, responsive behavior, and the
  existing high-risk assignment and shipping flows.

## Out of Scope

- New database tables, authentication flows, or changes to rental-order/device
  state machines beyond the existing management order response, pending-query
  contract extension, and the tenant-scoped XianGuanJia shipment-write switch
  stored on the existing `xianyu_application` record.
- New scheduling algorithms, pricing rules, automatic assignment, automatic
  dispatch, OCR acceptance without operator review, or new third-party write
  operations.
- Changes to customer uni-app, staff uni-app, or Nuxt customer web. The
  `camera-rental-admin` XianGuanJia integration page is in scope only for
  managing the tenant shipment-write switch.
- Replacing React, Vite, Tailwind, the current icon library, QR library, or
  motion library without a separately justified dependency decision.
- Persisting a second copy of orders, devices, schedules, permissions, or
  backend-derived metrics in browser storage.
- Exposing complete customer fields without the existing management-order or
  shipment permissions; exposing them through logs, ordinary exports, URLs,
  browser persistence, analytics, screenshots, or fixtures; exposing secrets,
  raw third-party payloads, payment numbers, or stack traces.

## UI Design Impact

- Foundation spec: `openspec/specs/ui-design/design.md`
- Adopt the foundation neutral/black/blue semantic palette with green, amber,
  and red reserved for stable status meanings. Status is always paired with
  text or iconography.
- Use one typography scale and a monospace/data style for SN, order, waybill,
  sync-run, and device identifiers. Chinese and English layouts must tolerate
  copy expansion without truncating required actions.
- Prefer borders and surface hierarchy to repeated shadows. Use the approved
  radius scale consistently across cards, controls, menus, and overlays.
- Keep motion within 160-240 ms, limit it to meaningful entry/transition
  feedback, and disable non-essential motion under reduced-motion preference.
- Desktop review baseline: 1440 x 900. Tablet review baseline: 768 x 1024.
  Mobile review baselines: 390 x 844 and 360 x 800.
- The mobile header contains product identity plus compact task navigation and
  grouped secondary controls; it must not render the desktop navigation row
  off-canvas.
- Dense lists expose essential fields first and move secondary/private detail
  into a drawer or expandable region. Management order and shipping views may
  render complete customer fields returned under their existing permissions;
  public, screenshot, fixture, analytics, export, and unauthenticated surfaces
  remain masked or omit those fields.
- Prototypes and sensory review cover all feature views plus loading, empty,
  sync failure, permission denial, conflict, disabled write, confirmation,
  successful operation, dialog, drawer, table/list, and Gantt states.

## Theme & Locale Capability Impact

- Theme support: `light-dark`, with system preference used only as the initial
  value when no explicit user preference exists.
- Theme toggle policy: create an accessible user-controlled toggle and persist
  the explicit selection in the existing safe preference storage.
- Internationalization: `enabled`.
- Supported locales: `zh-CN,en`.
- Default locale: `zh-CN`; fallback locale: `zh-CN`.
- Locale toggle policy: user controlled and persisted.
- Prototype coverage: every route and required state is reviewed in light and
  dark mode; representative desktop and mobile flows are reviewed in both
  `zh-CN` and `en`, with no raw keys or mixed-language fallback fragments.

## Architecture & Database Impact

- Foundation spec: `openspec/specs/system-architecture/design.md`
- Cross-layer change in `camera-rental-schedule-center`,
  `camera-rental-admin`, and the rental module's existing management order,
  application configuration, integration-client, scheduler, webhook, and
  shipment services. Extend the existing `xianyu_application` table with
  tenant-managed integration fields; do not add a second configuration table.
- Preserve the standalone React/Vite deployment path
  `/admin/schedule-center/`, existing same-origin token/tenant storage, and
  `/admin-api` transport prefix.
- Organize source into an application shell, feature modules, shared UI,
  reusable hooks, domain/read models, typed API services, adapters, and
  utilities. Feature modules cannot import another feature's page internals.
- Use dependency direction `view -> feature hook/controller -> typed service ->
  transport`, with adapters mapping API VOs to view read models.
- Separate query behavior from commands/mutations. Destructive or
  business-state-changing commands are explicit functions triggered by user
  intent; they are never hidden in mount effects.
- Use discriminated async states or an equivalent explicit state machine for
  idle/loading/success/empty/partial-error/error/permission/conflict states.
- Do not add another global state library merely to replace an oversized
  Context. Prefer focused React providers and feature hooks unless current
  evidence proves a shared store is necessary.
- Preserve existing package manager and dependencies. A new dependency requires
  evidence that the current stack cannot provide the capability.

## Frontend-Backend Data Flow Impact

- Foundation spec: `openspec/specs/frontend-backend-data-flow/design.md`
- Use the current routes for permission info, XianGuanJia configuration
  and express companies, devices, schedules, channel orders, pending-shipment
  orders, manual reviews, QR resolve/read, OCR draft, shipment confirmation,
  device assignment, dispatch, return, and manual-review resolution/closure.
- Maintained clients use these current API routes:
  `/system/auth/get-permission-info`, `/rental/xianyu/config/get`,
  `/rental/xianyu/express-company/list`, `/rental/device/page`,
  `/rental/schedule/page`, `/rental/xianyu/order/page`,
  `/rental/xianyu/order/pending-ship/page`, `/rental/manual-review/page`,
  `/rental/device/resolve-qr`, `/rental/device/get-qr`,
  `/rental/xianyu/order/ship/ocr`, `/rental/xianyu/order/ship`,
  `/rental/device/assign`, `/rental/device/dispatch`,
  `/rental/device/return`, `/rental/manual-review/resolve`, and
  `/rental/manual-review/close`.
- Extend the existing configuration contract with
  `PUT /rental/xianyu/config/update`. The request may contain the tenant's
  integration enabled state, base URL, AppKey, an optional replacement
  AppSecret, webhook base URL, write state, and supported synchronization-job
  settings. Tenant ID, application ID, legacy credential references, and
  encrypted values are never accepted from the browser.
- `GET /rental/xianyu/config/get` reports the persisted tenant configuration
  while returning only a masked AppKey and `appSecretConfigured`; AppSecret is
  never returned. An empty replacement AppSecret preserves the existing
  encrypted secret. Missing configuration is represented as a safe disabled
  draft with documented defaults.
- Saving an enabled integration requires a valid HTTPS base URL, non-empty
  AppKey, and either an existing encrypted AppSecret or a replacement secret.
  Enabling write operations additionally requires the integration itself to be
  enabled. Disabling integration or writes remains available as an emergency
  operation.
- Read clients, write clients, webhook verification, synchronization commands,
  and shipment commands resolve configuration from the persisted tenant
  application instead of `rental.xianyu.*` process properties. Webhooks resolve
  the tenant application by the documented AppKey identity before signature
  verification. Infrastructure jobs use the existing `@TenantJob` execution
  model, enter each tenant context, and use a database-backed guard to skip
  tenants whose integration or job switch is disabled. Cron expressions and
  job lifecycle remain managed by the infrastructure job administration page.
- `POST /rental/xianyu/order/ship` reads the persisted tenant write state inside
  the server-side command path before any assignment, remote call, or local
  shipment mutation. Browser flags, legacy `XGJ_*` environment variables, and
  `rental.xianyu.*` process properties are not read and have no compatibility
  fallback.
- Remove `xianyu_application.credential_reference` from the current schema and
  disable every historical application row that lacks a persisted AppKey or
  encrypted AppSecret. No maintained client or server path may depend on the
  removed field.
- Extend `GET /rental/xianyu/order/page` so its typed response carries the
  persisted complete receiver snapshot and seller remark while continuing to
  exclude raw payloads, goods blobs, and payment numbers.
- Extend `GET /rental/xianyu/order/pending-ship/page` so `keyword` matches
  external order number, receiver name, or receiver mobile and its response
  carries complete receiver fields and seller remark.
- Filters, safe route selection, dialog drafts, and temporary selections are
  client state. Devices, schedules, orders, assignments, sync status, write
  enablement, and metrics are server state and are refreshed after mutations.
- Receiver-name, full-phone, and order-number shipment queries remain ephemeral
  form state. They are sent only to the existing pending-shipment endpoint,
  never copied into URL parameters or browser storage, and cleared when the
  shipping feature unmounts or the session changes.
- Reads may use cancellation and bounded retry. Writes are never optimistically
  accepted and can only retry under their existing idempotency contract.
- API/mapper failures are converted to safe error categories at the service or
  adapter boundary; views never display raw exceptions.
- The UI must distinguish no data, no permission, sync failure, disabled write,
  stale data, schedule conflict, and genuine empty results.
- Amount, date, schedule, assignment, order-status, and device-status business
  truth is not recalculated or persisted by frontend components.
- AppKey and AppSecret are tenant-managed business configuration. The
  management page accepts AppKey and a one-way replacement AppSecret input,
  clears the secret field immediately after saving, and never receives the
  stored secret. The backend encrypts AppSecret at rest with the existing
  MyBatis encryption handler. Only the generic database encryption master key
  remains infrastructure configuration outside the management page.
- The full persisted configuration is tenant isolated, defaults to disabled,
  is guarded by `rental:xianyu:config:update`, and is reflected by both admin
  frontends after refresh. Secrets are excluded from logs, operation-log
  request bodies, error messages, fixtures, URLs, and browser persistence.

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`
- Cohesion/coupling impact: route/page components coordinate a feature and stay
  thin; presentational components render read models and emit intents; hooks
  own query/command state; services own API calls; adapters own VO-to-read-model
  conversion; shared UI has no page/store/transport dependency.
- Shared extraction requirement: repeated layout, navigation, metric, filter,
  data-state, status, identifier, date/range, list/table, dialog/drawer,
  permission-action, and operation-result behavior is extracted according to
  `component-impact-map.json`.
- `QuickBindingView`, `QuickBindingModal`, `AppContext`, and
  `GanttScheduleView` must be split because each exceeds or approaches the
  600-line ceiling and currently contains multiple responsibilities.
- Pages must not duplicate order/device filtering, metric derivation, status
  mapping, permission checks, or mutation lifecycle handling.
- Shared components cannot import API clients, feature pages, page-specific
  routes, or the all-application context.
- The refactor must remove empty macOS metadata source files from the active
  TypeScript source tree and prevent them from being treated as modules.

## Unresolved Gaps

- None. The product boundary is approved: comprehensively redesign and refactor
  the frontend while preserving current backend APIs, permissions, tenant
  behavior, and business workflows.
