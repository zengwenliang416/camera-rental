## Context

The repository contains a ruoyi-vue-pro backend and Vue admin baseline. Current
local documentation records the XianGuanJia signing and read endpoint contracts
verified on July 23, 2026. Real credentials must not enter Git or tests.

## Goals / Non-Goals

**Goals**

- Exact-body signing and safe read-only HTTP access.
- Durable raw plus normalized data with idempotent synchronization.
- Channel-order conversion into internal rental orders.
- Physical-device scheduling, conflict detection, and assignment.
- Authorization/after-sale alerts, replayable synchronization, and
  source-linked operational reporting.
- Admin query, operational handling, sync execution, retry, and health visibility.

**Non-Goals**

- Customer-created orders, pricing checkout, payments, deposits, or customer
  applications.
- Any XianGuanJia write operation.

## Decisions

- Create `yudao-module-rental-api` and `yudao-module-rental-biz`; keep the
  XianGuanJia adapter under `integration/xianyu`.
- Use environment-backed configuration and disable integration by default.
- Serialize once; sign and transmit the exact same UTF-8 bytes.
- Store large/restricted raw payloads behind a dedicated table/reference and
  permission boundary.
- Advance cursors only after durable page processing.
- Normalize external identifiers to strings and preserve unknown source values.
- Maintain explicit external product/SKU to internal equipment-model mappings;
  never infer a physical device from channel inventory.
- Map one channel order to at most one internal rental order. Missing product,
  date, or customer mappings remain review-required.
- Use inclusive business dates for display and `Asia/Shanghai`
  `[start, endExclusive)` intervals internally.
- Schedule and assign concrete device instances transactionally; SKU quantity
  alone never proves availability.
- Use MockWebServer for all default transport tests.
- Derive rent revenue from channel `pay_amount`, keep refunds separate, and
  preserve drill-down to source orders and assigned devices.

## Risks / Trade-offs

- Official schemas contain ambiguous fields; preserve raw values rather than
  silently coercing them.
- Read-only scope avoids operational writes but requires manual action in
  XianGuanJia for fulfillment or refunds.
- Raw payload retention improves auditability and increases privacy controls and
  storage requirements.

## Migration Plan

1. Add module and additive schema.
2. Deploy disabled.
3. Configure credentials through runtime secret injection.
4. Verify shop query in a controlled environment.
5. Enable bounded synchronization per authorized shop.
6. Enable order conversion, manual review, and device assignment after local
   data validation.

Rollback disables jobs and endpoints while retaining imported evidence and
cursors. Schema rollback must not delete synchronized records.
