# System Architecture & Database Spec

## Overview

The camera-rental platform is an independent source repository containing one
Spring Boot monolith and four separately built clients. The backend is the
authoritative owner of rental pricing, physical-device availability, occupied
ranges, allocation, order state, payment linkage, and third-party channel
normalization.

The accepted baseline is a modular monolith. The first rental implementation
must add `yudao-module-rental` rather than extending mall order tables with
rental-specific lifecycle and scheduling behavior.

## Application Topology

- Frontend runtime:
  - `camera-rental-admin`: Vue 3, TypeScript, Element Plus, Vite, pnpm.
  - `camera-rental-uniapp`: Vue 3 uni-app for WeChat Mini Program, H5, and App.
  - `camera-rental-staff`: Vue 3 TypeScript uni-app for warehouse operations,
    built with pnpm.
  - `camera-rental-web`: Nuxt 4 SSR customer website, built with bun.
- Backend runtime: Java 17, Spring Boot 3.5.15, Maven, MyBatis Plus, and the
  ruoyi-vue-pro monolith.
- API gateway or edge layer: no separate gateway is required in the initial
  modular-monolith stage; clients use the backend's existing admin/app API
  prefixes.
- Background workers: Spring-managed scheduled jobs perform safe incremental
  channel synchronization and retry. Push processing may enqueue internal work
  only through an infrastructure capability already enabled in the server.
- External services: MySQL, Redis, existing file infrastructure, optional
  member/pay/mall/ERP modules when explicitly enabled, and XianGuanJia.
- Local development entrypoints: backend port `48080` in current local
  configuration; each frontend uses the scripts and package manager declared by
  its own manifest.
- Production deployment shape: separately built clients call one deployed
  backend service; production topology, hostnames, and scaling policy remain
  deployment configuration rather than source constants.

## Module Boundaries

### `yudao-module-rental`

- Responsibility: rental product configuration, physical devices, rental
  orders, scheduling, assignments, delivery, inspection, maintenance, deposits,
  and channel mappings.
- Public contract: Java API module for cross-module contracts plus
  `/admin-api/rental/**` and `/app-api/rental/**` HTTP APIs from the Biz module.
- Owned data: every `rental_*` table and rental-specific state transition.
- Dependencies: framework starters, `yudao-module-system`, and
  `yudao-module-infra`; member, pay, mall, ERP, or WMS only after an explicit
  enablement decision.
- Forbidden dependencies: frontend code, direct writes into mall order core
  tables, third-party credentials in source, and Controller-to-Mapper access.
- Extension points: channel adapters, pricing policies, allocation policies,
  inspections, maintenance workflows, and domain events.

### `yudao-module-system`

- Responsibility: staff identity, roles, menus, permissions, tenant and data
  scope.
- Public contract: existing system APIs and security framework.
- Owned data: system users, roles, permissions, tenants, departments, and
  dictionaries.
- Dependencies: existing framework modules.
- Forbidden dependencies: rental domain implementation.
- Extension points: rental permissions, menu entries, and dictionaries.

### `yudao-module-infra`

- Responsibility: files, configuration, scheduled job infrastructure, logging,
  and other existing infrastructure services.
- Public contract: existing infra APIs and framework contracts.
- Owned data: existing infra tables only.
- Dependencies: existing framework modules.
- Forbidden dependencies: rental business decisions.
- Extension points: rental jobs, configuration records, and controlled raw-data
  storage.

### `yudao-module-member`

- Responsibility in the baseline: member account, login, profile, address,
  level, group, points, and related customer capabilities.
- Verified limitation: `member_user.name` is an editable profile value. The
  baseline has no real-name verification aggregate, verification status,
  identity-document model, or public verification API.
- Public contract used by rental: member existence/status and address DTOs.
- If real-name verification is approved, add a dedicated restricted member
  verification aggregate and public status API. Rental stores only the
  verification reference and order-time status snapshot, never a raw identity
  number or identity-document image.
- Forbidden dependency: rental code treating profile `name`, address recipient,
  social login, or payment account identity as verified legal identity.

### Client modules

- UI modules: admin pages, customer uni-app pages, staff operation pages, and
  Nuxt customer pages.
- Domain modules: client-side rental types, read models, formatting, and
  non-authoritative form validation.
- Application/service modules: typed API clients for admin or app API prefixes.
- Infrastructure modules: authentication transport, request interceptors,
  storage adapters, and platform-specific uni-app or Nuxt runtime adapters.
- Shared libraries: sharing is within each client repository boundary unless a
  separately versioned cross-client package is explicitly approved.

## Frontend Architecture

- Routing: preserve each baseline router or uni-app page manifest; rental pages
  are grouped under rental-specific route namespaces.
- Rendering mode: admin is SPA, customer and staff clients use uni-app targets,
  and customer web remains Nuxt SSR-compatible.
- State management: Pinia or the existing store pattern owns authentication and
  durable UI state; server records remain server state and are refreshed after
  mutations.
- Form handling: reuse existing form components and client validation for
  immediate feedback; server validation remains authoritative.
- Data fetching: typed client modules call only the declared backend prefix for
  that actor.
- Error handling: map the backend common result and business errors to loading,
  empty, permission, conflict, and retry states without exposing stack traces.
- Design system source: each client retains its current UI system; the project
  UI spec defines shared semantic behavior rather than forcing one component
  library across incompatible runtimes.

## Backend Architecture

- API style: REST controllers using the existing common response and pagination
  contracts; admin and app controllers are separated.
- Request validation: Bean Validation on request VOs plus service-entry checks
  for cross-entity rules.
- Auth/session model: existing Spring Security token model, role/permission
  annotations, tenant isolation, and data scope where enabled.
- Domain service boundaries: Controllers call Services; Services own
  transactions; Mappers own persistence; converters separate DO, DTO, and VO
  models.
- Background jobs: idempotent scheduled services with bounded pages, fixed
  upper time boundaries, durable cursors, retry records, and audit metrics.
- File/object storage: use existing infra file capability; no credentials or
  customer-private raw payloads enter public storage.
- Observability: structured, redacted logs and sync-run counters; never log
  secrets, signatures, full phone numbers, addresses, identity numbers, or
  payment credentials.

## API Surface

| Route or RPC | Owner | Input | Output | Auth | Side Effects |
| --- | --- | --- | --- | --- | --- |
| `/admin-api/rental/devices/**` | rental admin controller | device create/update/query VOs | device response/page VOs | rental device permission | create or update physical-device records |
| `/admin-api/rental/orders/**` | rental admin controller | order query/review VOs | order response/page VOs | rental order permission and data scope | review or transition rental orders |
| `/admin-api/rental/schedules/**` | rental admin controller | date range, SKU, device, assignment VOs | availability and schedule VOs | rental schedule permission | assignment writes occur only in a transaction |
| `/admin-api/rental/operations/**` | rental admin controller | scan, delivery, return, inspection, maintenance VOs | validated operation result VOs | operation-specific permission | transition device/order state and audit action |
| `/admin-api/rental/xianyu/**` | rental integration controller | shop, cursor, query, retry VOs | shop, sync-run, channel-order VOs | channel read/sync permission | read-only external calls and local upserts |
| `/app-api/rental/catalog/**` | rental app controller | catalog and availability queries | customer-safe product/availability VOs | public or member according to endpoint | no authoritative writes |
| `/app-api/rental/orders/**` | rental app controller | quote, create, and member order VOs | quote/order VOs | member authentication for writes | create order after server-side revalidation |
| internal rental API module | rental API module | stable cross-module DTOs | stable cross-module DTOs | in-process caller | declared cross-module effect only |

Exact endpoint suffixes and VO fields are change-level contracts and must be
specified before implementation.

## Database Model

All tables follow the project's base audit, tenant, and logical-delete
conventions where applicable. Monetary values use integer cents. Business dates
use date/time types interpreted in `Asia/Shanghai`.

Customer-facing business ranges include both displayed dates. Services convert
them to `Asia/Shanghai` half-open intervals `[start, endExclusive)` before
persistence and conflict checks. Two intervals overlap only when
`newStart < existingEndExclusive && newEndExclusive > existingStart`.

Customer order creation reserves SKU quantity for the occupied interval with an
explicit expiry. Payment confirms the quantity reservation. A concrete physical
device is assigned before picking/outbound, when the service transactionally
rechecks device state and interval overlap and then creates the assignment and
device schedule.

| Entity | Purpose | Owner | Fields | Relationships | Indexes | Constraints | Lifecycle | Migration | Retention/Deletion |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `rental_product_config` | Adds rental rules to a product/SKU | rental | product/SKU reference, rental status, pricing policy, buffers | one product/SKU to many order items and devices | product/SKU, status | one active config per referenced SKU scope | draft, active, inactive | additive SQL | logical delete; referenced history retained |
| `rental_device` | Tracks one physical asset | rental | device number, serial number, SKU, warehouse, status, acquisition data | many devices to one SKU; one device to many assignments | device number, serial number, SKU/status, warehouse/status | unique device number; serial uniqueness per approved policy | in-stock through retired/lost | additive SQL | logical delete; transactional history retained |
| `rental_package` | Defines a rentable bundle | rental | code, name, status | one package to many package items | code, status | unique package code | draft, active, inactive | additive SQL | logical delete |
| `rental_package_item` | Defines bundle quantities | rental | package, SKU/config, quantity | many items to one package | package id, SKU/config id | unique package and item reference | follows package | additive SQL | removed only when not referenced or logically deleted |
| `rental_order` | Owns customer, channel, dates, amount, and state | rental | order number, member/customer reference, source, rent dates, amounts, status, review state | one order to many items, deliveries, deposits, and channel mappings | order number, customer, status, rent dates, created time | unique internal order number | explicit order state machine | additive SQL | logical delete forbidden for ordinary repair; retain audit history |
| `rental_order_item` | Owns ordered SKU, quantity, dates, and pricing snapshot | rental | order, SKU/config, quantity, rent dates, amount snapshot | many items to one order; one item to many assignments | order id, SKU/config, rent dates | positive quantity and non-negative amounts | follows order with controlled adjustments | additive SQL | retained with order |
| `rental_schedule` | Records an effective occupied interval | rental | device, order item, occupied start, occupied end-exclusive, type, status, version | many schedules to one device and order item | device plus interval/status, order item, status | half-open interval; no overlapping effective interval for one device, enforced transactionally with lock/constraint strategy | reserved, effective, released, cancelled | additive SQL with concurrency design | never hard-delete to hide conflicts |
| `rental_device_assignment` | Links a physical device to an order item | rental | order item, device, schedule, assignment status | joins item, device, and schedule | order item, device, status | one effective assignment per device and schedule relationship | proposed, confirmed, released, replaced | additive SQL | retain replacement history |
| `rental_delivery` | Tracks outbound and return logistics or handoff | rental | order, direction, logistics mode, timestamps, carrier/tracking references | many deliveries to one order | order, direction/status, tracking reference | idempotent external logistics event key when present | prepared, sent, received, cancelled | additive SQL | retain audit history; redact displays |
| `rental_inspection` | Records return inspection | rental | order, device, inspector, result, completed time, damage summary | many inspections to order/device | device, order, result, completed time | completed inspection required before availability restoration | pending, completed, reopened | additive SQL | retain |
| `rental_maintenance` | Blocks and tracks repair | rental | device, issue, status, dates, cost | many records to one device | device/status, date range | active maintenance blocks assignment | reported, repairing, completed, cancelled | additive SQL | retain |
| `rental_deposit` | Separates deposits from rent | rental | order, amount, received/refunded amounts, status, payment references | many deposit operations to one order | order/status, payment reference | non-negative integer cents; idempotent payment references | pending, held, partially refunded, refunded, forfeited | additive SQL | retain financial history |
| `rental_channel_application` | Identifies an external application configuration without storing plaintext secret | rental integration | channel, app key identifier, enabled state, config reference | one application to many shops | channel/app key identifier, enabled | unique channel application identity | enabled, disabled | additive SQL | logical delete only after shops detached |
| `rental_channel_shop` | Tracks shop authorization lifecycle | rental integration | application, external shop id, display name, auth status, expiry, last sync | many shops to one application and many channel orders | application/shop, auth status, expiry | unique application plus external shop id | authorized, expired, revoked, disabled | additive SQL | retain historical linkage |
| `rental_channel_order` | Stores external order identity and normalized fields | rental integration | shop, external order no, status, pay amount, seller remark, raw payload reference, parse version/error, sync time | maps to zero or one rental order | shop/external order, external update time/id, sync time, review status | unique channel/application/shop/external order identity | received, normalized, linked, review-required, failed | additive SQL | no hard delete for sync repair |
| `rental_channel_sync_cursor` | Persists safe incremental progress | rental integration | shop, sync type, upper bound, update time, tie-breaker id, run id | one cursor stream per shop and sync type | shop/sync type | unique shop and sync type | advances only after durable page handling | additive SQL | retain current and audit runs as designed |
| `rental_channel_sync_record` | Audits each sync run or payload processing attempt | rental integration | run id, counts, status, error summary, start/end times | belongs to shop/application | run id, shop/status, started time | unique run id | running, succeeded, partial, failed | additive SQL | retain operational history |

## Permissions & Security

- User roles: customer/member, warehouse operator, inspector, maintenance
  operator, rental operations manager, finance/review staff, channel integration
  operator, and system administrator.
- Permission checks: every admin mutation and sensitive query uses explicit
  backend permission checks; menus are not authorization.
- Data isolation: preserve existing tenant and department/data-scope behavior;
  external application and shop records are scoped to the owning tenant.
- Secret handling: inject XianGuanJia credentials with environment variables,
  configuration center, or a secret manager. Store only a safe key identifier or
  encrypted reference where persistence is required.
- Identity handling: profile `name` is not proof of real-name verification.
  Identity evidence, if introduced, is restricted to the dedicated member
  verification boundary; ordinary rental APIs, logs, exports, and tables expose
  only safe status/reference data.
- Audit logging: record actor, action, target, result, correlation/run id, and
  redacted error. Keep raw channel payload access restricted.
- Abuse cases: prevent repeated assignment, replayed callbacks, duplicate
  channel orders, unauthorized shop selection, IDOR on orders/devices, mass
  export of PII, and accidental third-party write calls.

## Integration Boundaries

- Third-party APIs: XianGuanJia is accessed only from the backend integration
  client; the first lane is read-only.
- Webhooks: callbacks terminate at backend endpoints, verify the documented
  identity/signature contract, persist safely, deduplicate, and return promptly.
- Queues: optional internal async transport must reuse an enabled infrastructure
  capability and preserve tenant/correlation context.
- Email/SMS/push: use existing platform modules only after a change explicitly
  scopes notification behavior.
- Payments: reuse `yudao-module-pay` only after module enablement and callback
  contracts are approved; rent, deposit, refund, and compensation remain
  separate accounting concepts.
- Analytics: derive revenue and utilization from normalized rental records;
  parsing failure must not remove paid channel orders from total rent receipts.

## Operational Constraints

- Performance constraints: paginate all list and sync APIs; index device/status,
  order/status/date, shop/status, and channel update cursors; avoid N+1 detail
  pulls without bounded concurrency.
- Availability expectations: duplicate jobs, retries, and webhook replay must be
  safe; external outages must not corrupt local order or schedule state.
- Migration rules: add timestamped incremental SQL; do not edit an already
  executed historical migration.
- Backup/restore: database and restricted raw integration payloads require
  coordinated backup; cursor restoration must not skip records.
- Feature flag rules: XianGuanJia integration defaults disabled and real API
  tests require an explicit opt-in.
- Rollback constraints: schema rollback cannot discard orders, assignments,
  financial records, or raw channel evidence; code rollback must remain
  compatible with additive schema.

## Architecture Do's and Don'ts

- Do keep module ownership explicit.
- Do name persistence and side effects before implementation.
- Do update this spec when a feature changes architecture.
- Do use integer cents, typed business dates, and `Asia/Shanghai`.
- Don't create new APIs, tables, queues, or permissions without recording them here.
- Don't let frontend components bypass declared service or data-flow boundaries.
- Don't expose secrets or customer-private raw payloads to any frontend.
