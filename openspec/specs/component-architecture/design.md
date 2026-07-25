# Component Architecture & Reuse Spec

## Overview

Component reuse is scoped first to each application because admin, customer
uni-app, staff uni-app, and Nuxt use different runtimes and component systems.
Shared business truth belongs in backend services and API contracts, not copied
frontend utilities. Cross-client source sharing requires a separately approved
package boundary.

## Component Taxonomy

- Page/screen components: route or uni-app page entrypoints that coordinate
  permissions, route parameters, server queries, and domain components.
- Layout components: existing admin shell, customer navigation, staff operation
  shell, and Nuxt SSR layout.
- Domain components: rental date range, money breakdown, availability result,
  device identity/status, schedule range, order timeline, sync run status, and
  manual-review reason.
- Form components: rental order form, physical-device form, assignment form,
  handover/inspection form, and channel sync filters.
- Data display components: page tables/lists, detail descriptions, status tags,
  amount/date formatters, schedule lanes, and audit summaries.
- Feedback components: loading skeleton, empty result, permission denial,
  validation summary, conflict result, external outage, and retry panel.
- Headless hooks: typed query state, stale-request cancellation, idempotent
  submit guard, scan resolution, pagination, and permission-aware action state.
- Domain utilities/services: integer-cent formatting, `Asia/Shanghai` date
  display, stable status mapping, typed API clients, and non-authoritative input
  normalization.

## Cohesion Rules

- A component has one user-visible reason to change.
- Page components coordinate but do not implement price, scheduling, allocation,
  or channel signing algorithms.
- Presentational components receive response/read-model types and emit user
  intent.
- API serialization, domain conversion, UI rendering, and platform side effects
  live in separate modules.
- Staff scan components resolve and display a scan; the backend accepts or
  rejects the device operation.

## Coupling Rules

- Page components may compose shared components from the same application.
- Shared components must not import page-specific routes or stores.
- Domain components may import domain types and formatters but not raw request
  clients.
- API clients may import transport and generated/manual contract types but not
  view components.
- Admin and staff clients call only `/admin-api/rental/**`; customer clients call
  only `/app-api/rental/**`.
- No frontend imports backend DO/Mapper classes or exposes third-party
  credentials.
- Nuxt presentational code remains SSR-safe and cannot require browser globals at
  module evaluation time.

## Shared Component Extraction Rules

Extract a component, hook, utility, or service when any of these are true:

- The same UI behavior appears in two or more screens of one client.
- The same state machine is repeated.
- The same validation or formatting rule is repeated.
- A page-local component exceeds a single user-facing responsibility.
- A proposed implementation would duplicate an existing design-system control.
- Rental date range or occupied range is formatted differently in multiple
  places.
- Multiple pages map the same backend error/status codes independently.

Cross-client duplication of a visual primitive is acceptable when platform
component systems differ. Cross-client duplication of money, scheduling, or
state-transition business rules is forbidden and must move to the backend.

## Component Public API Rules

- Props are stable, minimal, and behavior-facing.
- Read-only domain components accept explicit read models rather than database
  rows or unbounded response objects.
- Events use intents such as `confirm-assignment`, `retry-sync`, or
  `submit-inspection`, not DOM names.
- Money props use integer cents and format only for display.
- Date props use typed/ISO contract values plus explicit range semantics; do not
  compare localized display strings.
- Slots/children are allowed only when they reduce coupling and preserve
  accessibility.

## State Ownership Rules

- Local state: dialog visibility, field drafts, selected rows, scan buffer, and
  transient expansion.
- Shared UI state: authentication, stable preferences, dictionaries, and
  permission-aware navigation in the existing application store.
- Server/cache state: products, devices, orders, schedules, assignments, shops,
  channel orders, and sync runs.
- Form state: owned by the form/page boundary until accepted; clear only after a
  successful response.
- URL state: pagination, filters, selected safe tab/entity, and other shareable
  state.
- Derived state: formatting and view-only labels; never a second source of truth
  for amount, availability, or lifecycle.

## Composition Patterns

- Preferred composition patterns: route page -> query/form hook -> typed API
  service -> domain/presentational components.
- Preferred composition patterns: staff operation page -> scan resolver ->
  confirmation component -> backend operation service -> result component.
- Preferred composition patterns: list page -> filter form + paginated table/list
  + detail drawer/dialog.
- Forbidden composition patterns: presentational component importing API client,
  page-specific store imported by a shared component, frontend scheduling
  engine, or component directly mutating cached server entities.
- Approved provider/context boundaries: existing Element Plus, Wot UI, uni-app,
  Pinia, router, i18n, and Nuxt providers only where supported by that client.
- Approved headless hook patterns: hooks own cancellation/loading/error state and
  return explicit commands; they do not hide destructive side effects on mount.

## File & Naming Conventions

- Component file naming: follow each existing client convention; new rental
  domains use clear PascalCase component names or the local uni-app convention.
- Hook naming: `useRental...` for reusable client behavior.
- Test naming: colocated or existing test-tree convention with behavior names,
  not implementation method names.
- Story/prototype naming: include actor and flow, such as
  `AdminDeviceAssignment` or `StaffReturnInspection`.
- Barrel/export rules: use existing application conventions; avoid broad barrels
  that create cycles or hide infrastructure dependencies.

## Testing Expectations

- Shared component tests: formatting, status rendering, disabled states, and
  emitted user intents.
- Hook tests: loading, stale response, retry, idempotent submit guard, and
  conflict refresh.
- Integration tests: page-to-API request mapping and response/error rendering
  with mocked transport.
- Accessibility checks: keyboard operation for admin/web, labels and focus,
  non-color status cues, readable contrast, and reduced-motion behavior.
- Visual/prototype review: loading, empty, error, permission, conflict, and
  narrow/mobile layouts, under the approved theme and locale policy.

## Refactor Triggers

- Duplicate logic detected: extract within the current client or move business
  truth to the backend.
- Cross-boundary import detected: introduce a typed service/read-model boundary.
- Props become data-source-specific: define a domain-facing read model.
- Component grows multiple responsibilities: split coordination, form, and
  presentation.
- Test setup requires unrelated modules: reduce hidden store/router/global
  dependencies.
- A customer and staff page implement the same authoritative calculation: remove
  both and consume a backend result.

## Component Do's and Don'ts

- Do extract reusable UI, hooks, and domain utilities when the extraction rules trigger.
- Do keep shared components independent of page-specific state and routes.
- Do update this spec before adding a new shared component family.
- Do reuse the current client component library rather than adding another.
- Don't copy/paste component logic across pages when a local shared abstraction fits.
- Don't make low-level components know about API clients, database rows, or auth globals.
- Don't create a cross-client package solely to share visually similar controls.
