# Acceptance Criteria: xian-guanjia-data-integration-v1

## Integration

- Integration is disabled by default and fails safely when runtime credentials
  are missing.
- Mock HTTP tests prove the exact canonical UTF-8 JSON bytes are both signed
  and transmitted for every in-scope read endpoint.
- Shop, product, SKU, order, after-sale, express-company, and push data are
  persisted idempotently as raw plus normalized records.
- Fixed windows, six-month boundaries, the 10,000-row cap, stable tie breakers,
  durable cursor advancement, bounded retry, and run counts prevent omissions
  and duplicates.
- Unknown fields and ambiguous after-sale amount units are preserved without
  silent coercion.
- Authorization expiry/invalid state, documented guarantee health failures,
  after-sale timeouts, and repeated sync failures create deduplicated alerts.
- Failed raw events/pages can be safely replayed without duplicate records or
  unsafe cursor advancement.
- No third-party write client or admin endpoint that performs outbound
  third-party writes exists.

## Rental Operations

- One channel order maps idempotently to at most one internal rental order.
- Channel product/SKU mappings are explicit, auditable, and can remain unmapped
  or review-required without silently selecting an equipment model.
- Missing product/date mappings or invalid remarks preserve the channel order
  and paid amount and create an actionable manual-review record.
- Operators can register uniquely identified physical devices independently of
  channel/SKU quantity.
- Billable and occupied ranges are stored separately; overlapping effective
  half-open schedules for one device are rejected under concurrent assignment.
- Device assignment and schedule creation succeed or roll back together and a
  replay does not duplicate either record.
- Reports show rent revenue from `pay_amount`, refunds separately, order source,
  product/SKU, utilization, idle time, and assigned-device income with
  drill-down to source records.

## Admin

- Authorized users can query channel data plus internal rental orders, devices,
  schedules, assignments, review queues, alerts, sync runs, reports, and
  redacted failures.
- Restricted raw payload access is separately permissioned, audited, and masks
  private fields in ordinary views and exports.
- Admin pages support persisted light/dark mode and `zh-CN`/`en`, including
  loading, empty, permission, validation, network, error, and retry states.

## Security & Quality

- No real AppKey, AppSecret, signature, full request headers, phone, address,
  identity data, or payment credential appears in source, fixtures, logs,
  responses, screenshots, or frontend bundles.
- Additive migrations provide tenant/audit fields, unique channel/order/device
  identities, cursor uniqueness, schedule indexes, and operational query
  indexes.
- Backend module tests, mock transport tests, admin type checks, and contract
  checks pass.

## Unresolved Gaps

- Order-push ingestion is implemented through signature verification, strict
  field parsing, durable idempotent events, after-commit detail refresh, and
  bounded infra Job retry. Product-push ingestion is implemented through the
  same durable push-event path and triggers only read-only product-detail
  refresh. Product/SKU list-page orchestration is implemented through
  read-only `PRODUCTS`, `PRODUCT_DETAIL`, and `PRODUCT_SKUS` calls with
  raw evidence and a separate cursor. Express-company lookup preserves raw
  evidence. After-sale orchestration remains incomplete.
- Authorization-loss, guarantee health, after-sale timeout, and
  order/after-sale page sync-failure alerts now have a deduplicated backend
  path and are visible through the existing admin alert list.
- Failed order-push events have bounded automatic replay. Separately
  permissioned manual order-push event replay requeues durable local push
  payloads, and order-detail raw-payload replay reprocesses durable local
  details without advancing page cursors or creating third-party writes.
  Order-page raw-payload replay reprocesses durable local `ORDER_PAGE` evidence
  and refreshes missing or stale order details without advancing page cursors.
  Product-push replay requeues durable local `PRODUCT_PUSH` payloads and
  triggers only read-only `/api/open/product/detail` refresh. Product/SKU
  list-page orchestration persists `PRODUCT_PAGE` evidence, refreshes stale
  details, chunks multi-spec SKU reads, and advances a separate `PRODUCT`
  cursor only after fixed-window success.
  Restricted raw-payload access has a separately permissioned, masked
  backend/admin path with API access-log auditing.
- Utilization, idle-time, product/SKU, source breakdown, and assigned-device
  income reports are implemented with source drill-down links; independent
  visual/sensory review remains incomplete.
- Browser E2E, live MySQL migration/concurrency evidence, and authenticated
  runtime smoke now exist. Runtime red-team and independent sensory review
  remain incomplete.
