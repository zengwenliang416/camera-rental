# Monitor Plan

## Signals

- Backend errors from `/admin-api/rental/xianyu/order/ship/**`.
- Count of `rental_device_shipment` rows by source, response code, and tenant.
- Idempotency conflict count.
- Device status transitions to `RENTED` after shipment.
- Assignment status transitions to `DISPATCHED`.
- XianGuanJia client failures and timeout rates.

## Observation Window

- First local smoke window: immediately after deploy.
- First production window: first controlled shop write test and the following
  24 hours.

## Normal Values

- `XGJ_WRITE_ENABLED=false` in ordinary local and production-safe startup.
- No real XianGuanJia write calls unless a controlled operator test is active.
- Shipment rows should have masked waybill audit data and non-empty
  idempotency keys.

## Owner

- Operator: 老大.
- Technical triage: 小G.

## Escalation

- Disable writes by setting `XGJ_WRITE_ENABLED=false`.
- Stop the affected frontend route from dispatching new shipment submissions.
- Preserve shipment rows and backend logs for audit before rollback.
