# Task Brief: 003-channel-persistence

## Goal

An integration operator can durably preserve a documented XianGuanJia order
detail response and advance a stable order cursor only after the local
transaction succeeds.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`

## Vertical Slice

The backend parses a successful order-detail payload, stores it in restricted
raw evidence storage, upserts a normalized channel order by shop and external
order number, and then allows a later page runner to persist a stable
`(update_time, order_no)` cursor. No sync controller or scheduler is introduced.

## In Scope

- XianGuanJia order-detail payload parser for documented order, goods, amount,
  seller-remark, and epoch-second fields.
- SHA-256 payload identity, restricted raw-payload persistence, and normalized
  `xianyu_order` idempotent upsert.
- Cursor comparison and persistence using `(source_updated_at, external_order_id)`
  as a stable tie breaker.
- Focused unit tests for parsing, idempotent persistence wiring, payload
  hashing, and cursor ordering.

## Out Of Scope

- Real API calls, controllers, scheduled jobs, paging orchestration, replay,
  alerting, shop/product/after-sale persistence, order conversion, admin UI,
  and all third-party write operations.

## Files Allowed

- `camera-rental-server/yudao-module-rental/**`
- `docs/integrations/xianyu/**`
- `openspec/changes/xian-guanjia-data-integration-v1/**`

## Interfaces / Seams

- `XianyuOrderPayloadParser` accepts a raw successful detail payload and
  produces documented normalized fields.
- `XianyuOrderPersistenceService` owns the local raw-record and order upsert
  transaction without transport or controller dependencies.
- `XianyuSyncCursorService` advances only strictly newer stable cursor points;
  a future page runner owns when it invokes this service.

## Components To Create

- Channel order, raw-payload, and sync-cursor DOs and mappers.
- Order payload parser, SHA-256 hasher, persistence service, and cursor service.

## Components To Reuse

- Existing `XianyuReadClient` raw response boundary.
- `TenantBaseDO`, `BaseMapperX`, MyBatis Plus, Jackson, and Spring
  transactions.

## Components To Extract

- Keep JSON parsing, hashing, raw persistence, order upsert, and cursor
  comparison separate so later shop/product/after-sale and page orchestration
  do not duplicate safety behavior.

## API / Data Flow Contracts

- Only successful order-detail JSON reaches the persistence service.
- Raw payload is stored only in `xianyu_raw_payload`; it is never logged or
  returned by this slice.
- `xianyu_order` uses `(tenant_id, shop_id, external_order_id)` as its
  idempotency key.
- A cursor point is newer only when its source timestamp is newer, or when the
  timestamp is equal and its external order ID sorts after the saved ID.

## State / Error / Empty / Loading Behavior

- Loading: later orchestration owns sync-run progress.
- Empty: a missing optional source field remains null or `UNKNOWN`; the required
  external order number rejects the payload.
- Error: malformed JSON or missing required identifiers aborts the local
  transaction before cursor advancement.
- Disabled: transport-level integration checks remain in `XianyuReadClient`;
  this persistence layer performs no network I/O.
- Permission: no operator endpoint is introduced in this slice.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `git diff --check -- camera-rental-server openspec/changes/xian-guanjia-data-integration-v1`
- `rg -n "product/create|product/edit|product/delete|order/consign|order/change-price|refund/agree|refund/refuse" camera-rental-server/yudao-module-rental`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- The payload passed to this service is the successful `data` object from the
  documented order-detail endpoint. A future transport orchestrator must not
  pass remote error responses into this boundary.
