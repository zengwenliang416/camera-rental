# Task Brief: 005-rental-conversion

## Goal

Rental operator can convert one eligible persisted XianGuanJia channel order
into one internal rental order, or receive one actionable manual-review result
when mapping, paid amount, or rental dates are incomplete.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`

## Vertical Slice

The backend locks a normalized channel order, returns an already accepted
conversion on replay, requires an explicit `MAPPED` product/SKU mapping,
parses seller remarks with a versioned rule set, and either creates one rental
order plus item or upserts one manual-review record. This task has no HTTP
controller because a later authorized admin/job boundary will invoke it.

## In Scope

- Versioned seller-remark parser for explicit `#租期M.D-M.D#` dates and the
  documented receipt/return fallback.
- Explicit shop/product/SKU mapping lookup with no automatic equipment-model
  selection.
- Transactional conversion of one channel order into at most one rental order
  and item, preserving `pay_amount` as integer cents.
- Additive type widening for rental-order and rental-item rent amounts so
  documented `int64` channel cents cannot be truncated.
- Idempotent manual review for incomplete mapping, amount, or dates.
- Focused parser and conversion tests for success, replay, and review paths.

## Out Of Scope

- Controllers, permissions, admin UI, mapping correction UI, scheduler,
  real API calls, raw payload access, daily rent allocation, device
  assignment, occupied schedules, refunds, and all third-party writes.

## Files Allowed

- `camera-rental-server/yudao-module-rental/**`
- `camera-rental-server/sql/mysql/migrations/**`
- `docs/domain/rental-order.md`
- `docs/integrations/xianyu/{field-mapping,order-sync}.md`
- `openspec/changes/xian-guanjia-data-integration-v1/**`

## Interfaces / Seams

- `XianyuRentalConversionService` owns one local conversion transaction.
- `SellerRemarkRentalPeriodParser` is a pure versioned date-parsing component.
- Mapping, rental-order, rental-item, review, and channel-order mappers own
  persistence queries; no controller accesses a mapper.

## Components To Create

- Product-mapping, rental-order, rental-item, and manual-review DO/mapper
  boundaries.
- Conversion result, conversion service, and seller-remark parser.

## Components To Reuse

- Existing normalized channel order, tenant/audit base DOs, MyBatis Plus,
  Spring transactions, and the foundation migration tables.

## Components To Extract

- Keep seller-remark parsing independent of conversion persistence so a later
  correction workflow can reparse historical orders with a named version.

## API / Data Flow Contracts

- Conversion input is a local channel-order id; output is either
  `CONVERTED(rentalOrderId)` or `REVIEW_REQUIRED(reviewId, reasonCode)`.
- The channel order is the source of truth for paid rent revenue. Missing or
  invalid conversion prerequisites never delete, overwrite, or hide it.
- The rental source identity includes the shop id and external order id to
  preserve shop-scoped channel identity.

## State / Error / Empty / Loading Behavior

- Loading: a later job/controller owns request lifecycle; this service locks
  the source row before inspecting or writing state.
- Empty: no mapping or no parsable rental period yields `REVIEW_REQUIRED`.
- Error: an unknown local order id rejects without a fabricated review or
  rental order; persistence failures roll back the transaction.
- Disabled: this local conversion layer performs no network I/O.
- Permission: no external endpoint is introduced in this slice.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=SellerRemarkRentalPeriodParserTest,XianyuRentalConversionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `git diff --check -- camera-rental-server/yudao-module-rental docs openspec/changes/xian-guanjia-data-integration-v1`
- `rg -n '/api/open/(product/(create|edit|stock|online|offline|delete)|order/(consign|price)|trade/refund/(agree|reject))' camera-rental-server/yudao-module-rental`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- One normalized channel order currently models one source product/SKU and
  therefore converts to one rental-order item. Multi-item channel order
  conversion needs a later normalized line-item slice.
