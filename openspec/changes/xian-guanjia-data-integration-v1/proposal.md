## Why

The business needs XianGuanJia orders to drive internal camera-rental
operations. A raw data mirror alone cannot allocate physical devices, detect
schedule conflicts, or track the operational rental order.

## What Changes

- Add a standalone, read-only XianGuanJia backend module.
- Persist raw and normalized shop, product, SKU, order, after-sale, push, cursor,
  and sync-run data.
- Add internal rental orders converted idempotently from channel orders.
- Add physical-device inventory, billable/occupied schedules, and assignment.
- Add shop authorization/after-sale health alerts and safe replay workflows.
- Add revenue, refund, order-source, utilization, and device-income reporting.
- Add bounded incremental synchronization, retries, redaction, and audit.
- Add admin pages for channel data, rental orders, devices, schedules,
  assignments, review, and sync monitoring.
- Explicitly exclude customer checkout/payment and all third-party writes.

## Capabilities

### New Capabilities

- `xian-guanjia-data-ingestion`: Secure read-only ingestion, persistence,
  synchronization, monitoring, and admin query of XianGuanJia data.
- `rental-operations-core`: Internal channel-derived rental orders, physical
  devices, occupied schedules, conflict detection, and assignment.

### Modified Capabilities

None.

## Impact

- Backend: new `yudao-module-rental` API/Biz module containing the XianGuanJia
  integration and rental operations domain.
- Database: additive `xianyu_*` and `rental_*` tables and indexes.
- Admin: channel, alert, rental order, device, schedule, assignment, review, and
  reporting pages.
- Runtime: `XGJ_APP_KEY` and `XGJ_APP_SECRET` environment injection.
- No customer checkout, payment, deposit, member-real-name, or public client
  impact.
