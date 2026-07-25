# Requirements: camera-rental-full-platform-v1

## Summary

Build the complete V1 camera and photographic-equipment rental platform in the
independent `camera-rental` repository. The release includes the Spring Boot
rental domain, additive MySQL schema, Web admin, customer uni-app, staff
uni-app, Nuxt customer website, native rental orders, physical-device
scheduling, fulfillment, inspection, maintenance, deposits, payment/refund
linkage, procurement and warehouse visibility, reporting, and read-only
XianGuanJia synchronization.

The final acceptance target is a complete end-to-end platform rather than a
backend-only foundation. Implementation may be delivered in verified vertical
slices, but a slice is not the full V1 until every in-scope actor can complete
the relevant workflow through production UI and authoritative backend APIs.

## Users & Actors

- Customer/member: browses rentable products, checks dates, places and pays for
  orders, manages delivery or pickup, and views rental progress, refunds, and
  deposit status.
- Rental operations administrator: manages products, prices, orders, schedules,
  customer review, channel mappings, exceptions, and operational dashboards.
- Warehouse operator: receives assets, scans devices, prepares outbound orders,
  records returns, and resolves device/order mismatches.
- Inspector: records return condition, evidence, missing accessories, damage,
  and whether the occupied schedule can be released.
- Maintenance operator: records faults, repairs, costs, completion, and device
  availability.
- Purchasing and inventory operator: manages suppliers, procurement receipts,
  warehouse locations, asset onboarding, transfers, and retirement.
- Finance/review operator: reviews rent, deposits, refunds, compensation,
  reconciliation, and revenue reports without changing source records.
- Channel integration operator: manages authorized shops, synchronization,
  mapping, retry, and manual-review queues without accessing plaintext secrets.
- Scheduled job and verified callback: perform idempotent channel synchronization
  and asynchronous processing.
- System administrator: manages roles, permissions, dictionaries, tenants, and
  runtime configuration through existing platform capabilities.

## In Scope

### Platform & Backend

- Add and enable `yudao-module-rental-api` and `yudao-module-rental-biz`.
- Keep rental business rules, transactions, persistence, APIs, jobs, and
  XianGuanJia adapters in the rental module.
- Enable and reuse the existing system, infra, member, pay, mall product, and
  ERP modules. Rental code may call their published APIs or explicit adapters
  but must not depend on another module's Mapper or persistence DO.
- Expose actor-specific APIs under `/admin-api/rental/**` and
  `/app-api/rental/**`; no frontend calls XianGuanJia directly.
- Use stable state machines, optimistic versions or idempotency keys, auditable
  transitions, and safe error codes.

### Catalog, Pricing & Packages

- Use the existing mall product module as the owner of category, brand, SPU,
  SKU, media, and ordinary catalog identity. The rental module owns whether a
  SKU is rentable and its rental lifecycle; it does not reuse mall trade orders
  as rental orders.
- Configure which mall products/SKUs are rentable and attach rent pricing,
  deposit, delivery buffers, inspection buffers, quantity limits, and status.
- Support cameras, lenses, drones, lighting, audio, tripods, and accessories.
- Support rentable packages containing multiple SKU quantities.
- Provide customer-safe category, brand, product, SKU, package, media, policy,
  price, and availability read models.
- Calculate quotes on the backend using integer cents and return a breakdown,
  billable range, occupied-range explanation, deposit, delivery fees, discounts,
  and quote expiry/version.

### Physical Devices, Procurement & Warehouses

- Use the existing ERP module as the owner of suppliers, procurement documents,
  purchase receipt, warehouses, stock movement, transfer, and stocktake
  documents. Add an explicit cross-module API/adapter where ERP currently
  exposes only internal services; rental code must not call ERP Mappers or DOs.
- Manage each physical device independently with device number, serial number,
  SKU, warehouse/location, acquisition data, condition, status, and QR/barcode.
- Support supplier and procurement linkage, receipt, device onboarding, transfer,
  stocktake differences, loss, retirement, and history.
- Keep SKU quantity and physical-device state distinct; quantity alone cannot
  satisfy a reservation.
- Prevent an unavailable, repairing, lost, retired, or conflicting device from
  being assigned or scanned into an invalid transition.

### Native Rental Orders

- Create native rental orders from customer Web and uni-app after backend quote
  and availability revalidation.
- Store customer/member, source channel, items, billable dates, fulfillment,
  amount snapshots, deposit, payment linkage, review state, and order lifecycle.
- Support draft/created, awaiting payment, paid, reserved, preparing, outbound,
  renting, returning, inspecting, completed, cancelled, refunding, and closed
  outcomes through explicit valid transitions.
- Support cancellation, extension, early return, item/device replacement,
  partial/full refund, overdue, damage compensation, and manual review according
  to the final approved policies.
- Enforce idempotent order submission and callback processing.

### Scheduling & Device Assignment

- Distinguish customer billable dates from the device occupied interval.
- Treat user-facing business ranges as inclusive dates and normalize them in
  `Asia/Shanghai` to `[start, endExclusive)` for persistence and conflict
  checks. Overlap means `newStart < existingEndExclusive` and
  `newEndExclusive > existingStart`; adjacent boundaries do not conflict.
- Query availability by SKU/package, quantity, dates, warehouse, maintenance,
  and effective schedules.
- Create an expiring SKU quantity reservation during customer order creation,
  confirm it after payment, and release it on cancellation or payment timeout.
- Assign concrete physical devices before picking/outbound. In one transaction,
  recheck device state and overlap, consume the confirmed quantity reservation,
  and create the device assignment and effective schedule.
- Reject overlapping effective occupied intervals for one physical device.
- Adjust or release schedules when orders are cancelled, extended, returned
  early, replaced, refunded, inspected, maintained, lost, or retired.
- Present both billable and occupied ranges in admin and staff workflows.

### Fulfillment, Inspection & Maintenance

- Support both express delivery and self-pickup in V1. Their default outbound,
  return, and inspection buffers remain configurable and require final product
  values.
- Let staff scan and validate devices for picking, outbound verification,
  handover, return receipt, inspection, maintenance, transfer, and retirement.
- Record deliveries, carrier/tracking references, handoff timestamps, evidence,
  operators, idempotency keys, and redacted audit history.
- Require completed return inspection before restoring normal availability.
- Record condition, missing accessories, damage, cleaning, compensation, and
  maintenance actions without erasing prior states.
- Active maintenance creates an occupied/blocking record and prevents
  assignment until completion.

### Payment, Deposit, Refund & Reconciliation

- Link rent payment, deposit hold/receipt, refund, deposit release/forfeit,
  delivery fees, discounts, overdue fees, and compensation as separate
  accounting concepts.
- Reuse the existing payment module rather than storing gateway secrets or
  implementing payment callbacks in frontend code.
- Support the payment channels already implemented by the baseline, including
  WeChat Pay and Alipay where runtime channel configuration is valid. Default
  tests use mock channels and never require production payment credentials.
- Verify callbacks, enforce idempotency, and conserve integer-cent totals across
  payment, refund, daily allocation, and reporting.
- Provide finance views for receivable, received, refunded, held, released,
  forfeited, compensated, and unreconciled amounts.

### XianGuanJia / Xianyu

- Configure XianGuanJia with runtime-injected placeholders and keep the
  integration disabled by default.
- Implement one canonical JSON serializer, body-bound signer, redacted error
  decoder, and backend HTTP client for documented read-only endpoints.
- Query authorized shops, product metadata, products, SKUs, orders, after-sales,
  and express companies.
- Ingest order push plus fixed-window incremental list/detail synchronization
  with shop-scoped cursors, stable tie breaking, bounded pages, retry, and run
  counts.
- Preserve raw restricted payload, hash, normalized fields, parser/conversion
  versions, source timestamps, and redacted errors.
- Treat external identifiers as strings and order amounts as integer cents.
- Parse `seller_remark`; empty or invalid remarks preserve paid revenue and
  create a manual-review state.
- Keep after-sale refund raw values and unit-confirmation state while the
  external documentation remains ambiguous.
- Map a channel order to zero or one internal rental order idempotently; missing
  product/date/customer mappings remain review-required.
- Exclude XianGuanJia product, stock, listing, delivery, price, and refund-decision
  write operations from this change.

### Admin Web

- Provide production pages for dashboard, products/packages, devices, warehouse
  and procurement views, orders, schedule/calendar, assignment, fulfillment,
  inspection, maintenance, deposits/refunds, Xianyu shops/sync/review, reports,
  dictionaries, and authorized configuration.
- Reuse Element Plus and the existing admin routing, permission, table, form,
  dialog, drawer, upload, dictionary, and request patterns.
- Handle loading, empty, error, permission, stale version, schedule conflict,
  retry, and sensitive-field masking states.

### Customer uni-app

- Support WeChat Mini Program, H5, and App for catalog browsing, search/filter,
  product/package detail, date selection, quote, checkout, payment, order list
  and detail, cancellation/refund request, extension request, delivery/pickup
  information, and deposit visibility.
- Use backend results for pricing, availability, order state, and payment state.
- Handle login expiry, payment cancellation, duplicate submit, inventory
  conflict, weak networks, pagination, and platform differences.
- Reuse the member module and existing account/password, SMS, and WeChat/social
  login capabilities. The same member identity and address records are used by
  customer Web and uni-app.
- Do not treat the existing editable member `name` field as verified legal
  identity. If the approved order policy requires real-name verification,
  provide a dedicated verification flow and expose only a safe status/reference
  to rental clients.

### Staff uni-app

- Provide task lists and scan-driven flows for picking, outbound, handover,
  return receipt, inspection, missing/damaged item recording, maintenance,
  transfer, stocktake, and schedule lookup.
- Resolve scan codes to a physical device and require backend validation before
  any state change.
- Support safe retry/idempotency and clear next-action results for intermittent
  warehouse connectivity.

### Customer Nuxt Web

- Provide SSR-safe category, brand, search, product/package detail, date
  selection, quote, checkout, payment result, member orders, order detail, and
  policy/help pages.
- Provide indexable metadata and canonical content for public catalog pages.
- Share backend app APIs with the customer uni-app and keep secrets server-side.

### Reports & Operations

- Provide revenue, refund, deposit, compensation, utilization, idle time,
  device income, device cost/ROI, maintenance cost, order source, product/SKU,
  warehouse, overdue, and sync-health reports.
- Derive reports from normalized source records and preserve drill-down to
  auditable orders/devices where permission allows.
- Include paid Xianyu orders in rent revenue even when seller-remark parsing
  failed; do not silently net refunds against rent without an explicit metric.
- Support bounded export with permission checks and sensitive-field masking.

### Database, Security & Quality

- Add timestamped, additive MySQL migrations; never rewrite executed history.
- Store incremental MySQL migrations under `camera-rental-server/sql/mysql/migrations/`;
  the first rental migration is `20260723_001_rental_foundation.sql`.
- Create the rental product, package, device, order, schedule, assignment,
  delivery, inspection, maintenance, deposit, channel, sync, and audit entities
  named in `spec-map.json`, plus only those supporting entities approved during
  design.
- Follow existing audit, tenant, logical-delete, ID, and naming conventions.
- Add unique constraints and indexes for device/order/channel identities,
  status/date queries, schedules, warehouses, review queues, and sync cursors.
- Never commit or expose real credentials, signatures, full phone/address/ID
  data, payment credentials, or unrestricted raw payloads.
- Test amount conservation, date boundaries, overlap, concurrent assignment,
  state transitions, idempotency, permission isolation, redaction, and
  cross-client API contracts.
- Reserve `[1_024_000_000, 1_025_000_000)` for rental business error codes and
  register the range when implementation begins.

## Out of Scope

- XianGuanJia write operations against real shops.
- Real AppKey/AppSecret, production payment credentials, certificates, or
  production hostnames in Git, fixtures, logs, frontend bundles, or docs.
- A microservice split, new message broker, or duplicate order/payment/catalog
  platform when existing modules can own those generic capabilities.
- Frontend-authoritative pricing, schedule, inventory, payment, or status
  transitions.
- Automatic approval of ambiguous channel orders, refunds, compensation, or
  customer identity evidence.
- Hard deletion of historical orders, financial records, schedules, device
  events, maintenance, sync evidence, or audit records as a repair strategy.
- Production deployment topology and commercial operations not required to
  verify the application behavior.

## UI Design Impact

- Foundation spec: `openspec/specs/ui-design/design.md`.
- New production UI is required in admin, customer uni-app, staff uni-app, and
  Nuxt Web.
- All surfaces must distinguish billable and occupied dates, use text plus color
  for status, mask private data, and render backend conflicts explicitly.
- Each actor flow must cover loading, empty, validation, permission, network,
  server, conflict, disabled, and success states.
- Theme acceptance covers both light and dark modes, and locale acceptance
  covers Simplified Chinese and English.

## Theme & Locale Capability Impact

- Theme policy is approved as `light-dark` with `theme-toggle:user` across
  admin, customer uni-app, staff, and Nuxt Web.
- Every production client must expose an accessible user-controlled switch,
  persist the selected mode safely, apply it before primary content paint where
  the runtime permits, and avoid hydration or startup theme flashes.
- Both themes must cover rental pages, shared components, charts, calendars,
  forms, tables/lists, dialogs/drawers, scan states, checkout/payment, loading,
  empty, permission, validation, conflict, and error states.
- Locale policy is approved as `i18n:enabled`, `locales:zh-CN,en`, and
  `default-locale:zh-CN`.
- Every production client must provide an accessible persisted locale switch.
  Rental copy, statuses, validation, permission, conflict, payment, sync,
  report, export, and policy content must be complete in both locales.
- Nuxt public pages must provide locale-aware routes or an equivalent stable
  URL policy, localized metadata, canonical/hreflang behavior, and SSR output
  without a client-only language flash.
- All four production clients, prototypes, component tests, and visual
  acceptance matrices must cover light/dark multiplied by `zh-CN`/`en`.

## Architecture & Database Impact

- Foundation spec: `openspec/specs/system-architecture/design.md`.
- The modular monolith remains authoritative for every rental domain invariant.
- Full V1 adds all core rental tables plus supporting warehouse, procurement,
  pricing, device-event, payment-link, refund, compensation, and reporting
  projections only where existing modules do not already own the source record.
- Existing member, pay, mall, and ERP modules are enabled for V1. Rental tables
  link to their public identities and APIs without mutating mall trade-order
  semantics or depending on cross-module persistence internals.
- The current member baseline has no real-name verification service. Any
  approved real-name requirement adds a restricted member-owned verification
  aggregate/API; rental orders retain only the verification reference and
  order-time status snapshot, not raw identity numbers or document images.
- Financial and historical entities are append/audit oriented and cannot be
  destructively rolled back.

## Frontend-Backend Data Flow Impact

- Foundation spec: `openspec/specs/frontend-backend-data-flow/design.md`.
- Implements all named foundation flows and adds quote/payment, cancellation,
  extension, pickup/delivery, procurement receipt, warehouse transfer,
  maintenance, deposit/refund, reconciliation, and reporting flows.
- Every mutation uses server validation, permission, state/version checks, and
  an idempotency strategy.
- Customer clients call only app APIs; admin and staff call only admin APIs;
  external callbacks and clients terminate in the backend.
- External-channel consistency is eventual; local financial, assignment, and
  state-transition writes are transactional.

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`.
- Each client creates actor-specific pages while extracting local reusable
  date-range, money, status, conflict, schedule, order-timeline, scan, and
  operation-result components where repeated.
- Typed API services, domain read models, formatters, and request-state hooks
  are separated from page components.
- Cross-client visual source sharing is not introduced; authoritative business
  rules remain backend-only.
- Backend signing, synchronization, pricing, scheduling, state machines,
  fulfillment, inspection, finance linkage, and reporting remain independently
  testable services.

## Unresolved Gaps

- Confirm whether real-name verification is mandatory before payment, outbound,
  or only for selected high-risk products; login itself reuses member auth.
- Confirm whether the deposit is a separate payment order or a provider
  pre-authorization/hold, plus refund approval rules.
- Confirm default occupied-date buffers for express delivery and self-pickup.
- Confirm pricing, extension, cancellation, overdue, early-return, damage, and
  compensation policies.
- Confirm the minimum V1 report definitions and export boundaries.
