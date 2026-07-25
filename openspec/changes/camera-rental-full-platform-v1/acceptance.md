# Acceptance Criteria: camera-rental-full-platform-v1

## Customer Criteria

- A customer can browse rentable products and packages on both Nuxt Web and
  uni-app, select a valid date range, and receive the same authoritative quote
  and availability result from the backend.
- A signed-in customer can submit one idempotent order, complete the configured
  payment/deposit flow, and view the resulting order, amount, billable dates,
  fulfillment, deposit, and status on both customer clients.
- A concurrent capacity change causes an explicit conflict and does not create
  a partially paid, duplicated, or unscheduled order.
- A customer can request the approved cancellation, extension, early-return,
  and refund actions and sees the server-accepted result or a specific safe
  rejection reason.
- Public Nuxt catalog pages render SSR-safe metadata and content without
  exposing server or third-party credentials.

## Operations Criteria

- An authorized admin can configure rentable SKUs/packages, integer-cent
  pricing/deposit rules, buffers, and active status without changing mall order
  semantics.
- An authorized operator can create, import, locate, transfer, inspect,
  maintain, lose, retire, and audit a physical device with a unique scannable
  identity.
- The schedule UI clearly displays billable and occupied ranges and returns the
  backend conflict when an effective occupied interval overlaps.
- Inclusive business dates map to `Asia/Shanghai` half-open intervals; adjacent
  boundaries do not conflict, while every true overlap is rejected.
- Customer order creation reserves SKU quantity without binding a device;
  payment confirms the reservation, timeout/cancellation releases it, and a
  concrete device is transactionally assigned before picking/outbound.
- An order can progress through reservation, assignment, picking, outbound,
  rental, return, inspection, and completion without an invalid state jump.
- Cancelling, extending, returning early, replacing a device, or opening
  maintenance updates the effective schedule and preserves history.
- Admin list/detail pages handle loading, empty, permission, validation, stale,
  conflict, network, and safe server-error states.

## Staff Criteria

- Staff can scan a device for picking, outbound, return, inspection,
  maintenance, transfer, and stocktake, and the backend rejects a mismatched or
  invalid operation.
- Repeating a previously accepted scan with the same idempotency key returns the
  accepted result and does not duplicate delivery, inspection, maintenance, or
  device-event records.
- Return inspection must complete before a normal device becomes assignable;
  damage or maintenance keeps it blocked with a visible next action.
- Staff can perform the accepted flows on the supported uni-app targets with
  explicit weak-network retry behavior.

## Finance & Reporting Criteria

- Rent, deposit, delivery fee, discount, refund, deposit release/forfeit,
  overdue fee, and compensation remain separately traceable in integer cents.
- Payment and refund callbacks are verified and idempotent, and a replay cannot
  change totals twice.
- Daily allocations and all financial summaries conserve source totals,
  including remainder cents, leap day, month, and year boundaries.
- Authorized users can view and drill into revenue, refunds, deposits,
  utilization, idle time, device income/ROI, maintenance cost, overdue,
  channel-source, and sync-health reports.
- Paid Xianyu orders with missing or invalid rental remarks remain included in
  rent revenue and appear in a review queue.
- Exports are bounded, permission-controlled, and mask private fields.

## XianGuanJia Criteria

- Integration is disabled by default; enabling it without runtime credentials
  fails safely without exposing supplied values.
- Mock HTTP tests cover every documented in-scope read endpoint and prove the
  exact canonical UTF-8 JSON body is both signed and sent.
- An authorized operator can view local shop authorization health, start a
  bounded sync, inspect run counts, retry safe failures, and review normalized
  orders/after-sales without seeing secrets or unrestricted PII.
- Six-month query boundaries, the 10,000-row cap, fixed upper windows,
  same-timestamp tie breakers, and cursor advancement after durable processing
  are enforced.
- Replayed lists, details, pushes, callbacks, or manual sync requests do not
  create duplicate channel or rental orders.
- No XianGuanJia write client or admin endpoint is available in this change.

## System & Data Criteria

- The Maven reactor compiles and tests with rental API/Biz modules enabled.
- Timestamped additive migrations create approved rental entities, constraints,
  indexes, and audit columns without modifying historical migrations.
- Device number, internal order number, idempotency keys, payment references,
  channel identities, and sync cursor streams have the required uniqueness.
- The final concurrency design rejects overlapping effective schedules for one
  device under competing transactions.
- External identifiers retain exact digits as strings; money never uses floating
  point for settlement; business dates use `Asia/Shanghai`.
- Raw channel payload, normalized values, versions, hashes, source times, and
  redacted failures remain traceable under restricted access.
- Every admin mutation and sensitive query has an explicit backend permission
  and respects tenant/data scope.
- Secret and PII scans find no real AppSecret, signature, full phone/address/ID,
  payment credential, or production secret in source, fixtures, logs, APIs, or
  frontend bundles.

## Component & Cross-Client Criteria

- Components, hooks, utilities, and services named in
  `component-impact-map.json` are reused or extracted rather than duplicated.
- Page components do not implement pricing, availability, assignment,
  lifecycle, payment, or XianGuanJia signing rules.
- Typed admin/app APIs remain aligned with backend VOs across all affected
  clients.
- Admin runs its type checks and target UI tests; customer uni-app builds at
  least H5 and WeChat Mini Program; staff runs supported target builds; Nuxt
  passes type/build and SSR checks.
- Every production client exposes an accessible light/dark switch, persists the
  selected mode, restores it without a material startup flash, and renders all
  rental flows correctly in both modes.
- Every production client exposes an accessible `zh-CN`/`en` switch, persists
  the selected locale, and renders complete rental copy without raw keys or
  mixed-language fallback fragments.
- Nuxt public pages render localized SSR metadata and stable canonical/hreflang
  output under the approved locale URL policy.
- Visual acceptance covers all four light/dark and `zh-CN`/`en` combinations
  plus desktop, mobile, loading, empty, permission, validation, conflict, and
  error states.

## Verification Surfaces

- Facticity: online XianGuanJia contract snapshot, module manifests, migrations,
  endpoint/permission registry, and generated API contracts.
- Static: Maven compile, Java checks, frontend type checks, lint/build scripts,
  forbidden dependencies, SSR safety, secret scans, and contract diff.
- Unit: pricing, dates, schedule overlap, allocation, state machines, finance
  conservation, parsing, signing, redaction, and permissions.
- Redteam: concurrent assignment, replay, forged callback, tenant/shop mismatch,
  IDOR, malformed payload, oversized export, PII leakage, and attempted
  third-party write operation.
- E2E: MySQL-backed backend plus mock payment/XianGuanJia services, admin
  operations, customer purchase, staff fulfillment, return inspection, finance,
  and report reconciliation.
- Sensory: approved theme/locale prototypes and production pages on desktop,
  mobile, narrow view, and uni-app targets.

## Unresolved Gaps

- Real-name verification trigger policy.
- Deposit collection/hold and refund approval policy.
- Express and self-pickup occupied-range buffers.
- Pricing and exception lifecycle policies.
- Report definitions and export boundaries.
