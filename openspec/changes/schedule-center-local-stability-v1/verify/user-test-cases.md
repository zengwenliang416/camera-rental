# User-Aligned Test Cases: schedule-center-local-stability-v1

## User Test Case Scope

- Source requirements: `openspec/changes/schedule-center-local-stability-v1/requirements.md`
- Acceptance criteria: `openspec/changes/schedule-center-local-stability-v1/acceptance.md`
- Prototype handoff: `openspec/changes/schedule-center-local-stability-v1/prototype/handoff.md`
- Development handoff: `openspec/changes/schedule-center-local-stability-v1/development/handoff-to-verify.md`

## Aligned Test Cases

### `utc-admin-config-zero-compat`

- Actor: tenant administrator.
- User goal: manage every XianGuanJia business setting from the admin page
  without any environment-variable or legacy credential-reference fallback.
- Preconditions: migrations 029 through 031 are applied and the generic
  database encryption key is available.
- Steps: open the tenant configuration page; inspect enabled, endpoint,
  masked AppKey, blank AppSecret replacement input, write switch, job switch,
  and synchronization parameters; restart the backend without `XGJ_*`.
- Expected result: configuration remains `READY`; AppSecret is never returned;
  write remains disabled unless explicitly enabled; runtime uses only the
  current tenant's persisted application row.
- Boundary / error / permission states: missing credentials cannot stay
  enabled; invalid URL cannot be enabled; another tenant cannot read or update
  the row.
- Acceptance refs: A3.

### `utc-scheduled-read-sync-integrity`

- Actor: integration operator.
- User goal: keep authorized shop, order, product, SKU, and after-sale data
  synchronized without losing existing records.
- Preconditions: integration and job switches are enabled for tenant 1; write
  switch is disabled.
- Steps: observe scheduled read synchronization; inspect recent sync runs;
  compare table counts; run schema and orphan-relation checks.
- Expected result: successful shops continue syncing idempotently; business
  row counts do not decrease; no cross-tenant or missing-parent relations are
  introduced.
- Boundary / error / permission states: expired authorization is skipped;
  third-party partial failures are recorded and do not delete successful or
  historical data.
- Acceptance refs: A3.

### `utc-responsive-live-dashboard`

- Actor: rental operations manager.
- User goal: use the dashboard against the current server snapshot on desktop
  and mobile.
- Preconditions: the local backend, admin frontend, and schedule center are
  running with an authenticated tenant-1 session.
- Steps: load the dashboard at 1440 and 390 CSS pixels; wait for the snapshot;
  inspect registered devices, pending shipment, in-progress, and review
  states.
- Expected result: the page shows server-derived counts, no prototype fixture
  count, and no page-level horizontal overflow.
- Boundary / error / permission states: loading, partial failure, permission
  denial, and empty states remain distinguishable.
- Acceptance refs: A1, A2, A3.

### `utc-shipping-write-guard`

- Actor: authorized warehouse operator.
- User goal: review a waybill, search an available device, search a pending
  order, and submit only when every server gate is satisfied.
- Preconditions: 60 registered available devices, synchronized pending orders,
  shipment query permission, and persisted write switch disabled.
- Steps: open the shipping workbench; inspect device search and order search;
  verify order search supports receiver name, full phone, and order number;
  inspect the final confirmation state without submitting.
- Expected result: OCR remains a draft; device and order are independently
  selected; complete customer verification fields are available only after a
  search; the real shipment button stays disabled while the write switch is
  off; no optimistic state mutation occurs.
- Boundary / error / permission states: missing waybill, device, order, rental
  period, permission, or write switch blocks submission.
- Acceptance refs: A1, A3, A5.

### `utc-permission-tenant-secret-isolation`

- Actor: administrator and restricted operator.
- User goal: preserve tenant and permission boundaries for configuration,
  customer data, raw payloads, and shipment commands.
- Preconditions: multiple tenant application rows exist and only tenant 1 is
  fully configured.
- Steps: inspect tenant-scoped database relations and permission-gated UI/API
  behavior; verify AppSecret and raw payload contents are absent from ordinary
  responses and logs.
- Expected result: no cross-tenant application/shop/order relation exists;
  private customer fields require the existing management query permission;
  secrets are never returned or logged.
- Boundary / error / permission states: missing permission returns a safe
  denial and cannot be bypassed by frontend state.
- Acceptance refs: A3, A5.

### `utc-theme-locale-keyboard`

- Actor: keyboard or touch user.
- User goal: operate every schedule-center route in light/dark mode and
  zh-CN/en without losing focus or context.
- Preconditions: authenticated schedule-center session.
- Steps: switch theme and locale; open mobile navigation, dialogs, drawers,
  account menu, and login overlay; exercise Escape and Tab/Shift+Tab.
- Expected result: preferences persist; labels update; overlays trap and
  restore focus; touch targets and reduced-motion behavior remain usable.
- Boundary / error / permission states: inaccessible actions remain absent or
  disabled with a clear reason.
- Acceptance refs: A1, A2, A4.

## User Signoff

Status: `pending explicit user approval`

The user must approve, edit, add, or remove these test cases before six-domain
verification can claim coverage.

## Domain Mapping

Each approved user test case must be mapped to all six domains:
facticity, static, unit, redteam, e2e, and sensory.
