# Requirements: xian-guanjia-data-integration-v1

## Summary

Build XianGuanJia data integration plus the internal rental-operations core. It
reads and preserves channel data, converts eligible channel orders into
internal rental orders, and provides physical-device scheduling and assignment
through the admin UI.

## Users & Actors

- Integration operator: configures a runtime application, views authorized
  shops, starts bounded read syncs, and reviews failures.
- Operations analyst: queries normalized product, order, after-sale, and sync
  data without accessing secrets or unrestricted private fields.
- Rental operator: reviews imported orders, fixes mapping/date issues, and
  manages rental order status.
- Equipment operator: manages physical devices, occupied schedules, conflicts,
  and device assignment.
- Scheduler/webhook receiver: performs idempotent incremental synchronization
  and durable push ingestion.

## In Scope

- Add a dedicated backend integration module with API/Biz separation.
- Runtime-only XianGuanJia credentials; integration disabled by default.
- One canonical UTF-8 JSON serializer, signer, HTTP client, error decoder, and
  redactor.
- Read authorized shops, categories, attributes, products, product details,
  SKUs, order lists/details, after-sale lists/details, and express companies.
- Receive documented product/order pushes, persist raw events, deduplicate, and
  schedule detail refresh.
- Monitor shop authorization validity, expiry, professional-plan state, and
  documented guarantee/deposit health fields; create deduplicated alerts.
- Use fixed upper-bound windows, six-month limits, 10,000-row splitting,
  stable timestamp/id tie breaking, durable cursors, bounded retry, and run
  counts.
- Preserve restricted raw payload, payload hash, normalized fields, schema and
  conversion versions, source timestamps, sync timestamps, and redacted errors.
- Store external identifiers as strings and documented money values as integer
  cents. Preserve ambiguous after-sale values with unit-confirmation state.
- Parse `seller_remark` using versioned rules. Invalid or missing remarks retain
  paid revenue and enter manual review.
- Maintain explicit channel product/SKU to internal equipment-model mappings,
  including unmapped and ambiguous review states.
- Convert a channel order idempotently to zero or one internal rental order.
- Manage physical devices independently from SKU/channel inventory.
- Store billable dates separately from occupied dates. Internally use
  `Asia/Shanghai` half-open intervals and reject overlapping effective
  schedules for one device.
- Assign devices and create schedules in one transaction with a conflict
  recheck and idempotency key.
- Track after-sale status and documented timeout fields independently from
  rental-order status; create actionable exception alerts without automatically
  accepting or rejecting refunds.
- Support replay of failed push/detail/page processing from durable raw data
  without advancing an unsafe cursor or duplicating records.
- Provide source-linked reports for rent revenue, refunds, order source,
  product/SKU, schedule utilization, idle time, and assigned-device income.
  `pay_amount` is rent revenue; refunds remain a separate metric.
- Provide admin pages for application health, shops, products/SKUs, orders,
  after-sales, rental orders, devices, schedules, assignments, manual review,
  alerts, sync runs, cursors, reports, restricted raw-record access, and safe
  retry/replay.
- Admin pages support light/dark mode and `zh-CN`/`en`, with persisted user
  preferences and complete loading, empty, permission, error, and retry states.

## Out of Scope

- Customer-created rental orders, checkout pricing, payment, deposit, real-name
  verification, delivery fulfillment, inspection, maintenance, procurement,
  warehouse, or customer-facing rental reports.
- Customer uni-app, staff uni-app, and Nuxt customer website changes.
- XianGuanJia product, inventory, listing, delivery, price, or refund-decision
  write operations.
- Real credentials in source, fixtures, logs, docs, APIs, or frontend bundles.

## Architecture & Data

- New module: `yudao-module-rental-api` and `yudao-module-rental-biz`, including
  `integration/xianyu`.
- Reuse system permissions/tenancy and infra configuration, files, jobs, and
  logging; do not enable member, pay, mall, or ERP modules for this change.
- Add additive MySQL migrations under
  `camera-rental-server/sql/mysql/migrations/`.
- Persist channel application/shop/product/order/after-sale data plus
  `rental_order`, `rental_order_item`, `rental_device`, `rental_schedule`, and
  `rental_device_assignment`, explicit product mappings, and alert records.
- Frontend calls `/admin-api/xianyu/**` and `/admin-api/rental/**`; third-party
  calls terminate in the backend.

## UI Design Impact

- Only `camera-rental-admin` is changed in the frontend.
- Reuse Element Plus, existing routing, permissions, tables, forms, drawers,
  dictionaries, and request patterns.
- Mask phone/address/private fields by default. Restricted raw payload access
  requires a separate permission and audit event.

## Unresolved Gaps

- The current implementation covers authorized-shop and order synchronization,
  order conversion/manual review, physical-device assignment, schedules, sync
  runs, authorization-loss alerts, and operational reports for revenue,
  source breakdown, product/SKU, utilization, idle time, and assigned-device
  income.
- Order-push ingestion now has signature verification, strict parsing,
  idempotent durable persistence, after-commit detail refresh, and bounded
  infra Job retry. Product-push ingestion now has signature verification,
  strict parsing, idempotent durable persistence, after-commit read-only
  product-detail refresh, and bounded retry/replay. Product list/SKU page
  orchestration now persists `PRODUCT_PAGE` evidence, refreshes stale
  product details, chunks multi-spec SKU reads, and advances a separate
  `PRODUCT` cursor only after fixed-window success. Express-company lookup
  now preserves `EXPRESS_COMPANIES` raw evidence. After-sale orchestration
  still needs complete admin operations.
- Guarantee health alerts now parse documented `is_deposit_enough` values into
  persisted shop health and one deduplicated backend alert when the service
  deposit is insufficient. Manual order-push event replay now uses a separate
  permission and requeues only durable local push payloads;
  order-detail raw-payload replay uses the same permission and reprocesses
  local detail evidence without advancing cursors; order-page raw-payload
  replay uses persisted `ORDER_PAGE` evidence to refresh missing/stale details
  without advancing cursors; product-push replay requeues durable local
  `PRODUCT_PUSH` payloads and triggers only the read-only product-detail
  refresh path.
  After-sale timeout alerts and
  order/after-sale page sync failures now refresh deduplicated operational
  alerts. Restricted raw-payload access now uses a separate permission, backend
  re-redaction, and API access-log auditing.
- Browser E2E and real MySQL migration/concurrency verification now exist for
  the current worktree; runtime red-team and independent sensory review remain
  pending.
