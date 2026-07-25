# Implementation Readiness: xian-guanjia-data-integration-v1

## First Slice

1. Scaffold `yudao-module-rental-api` and `yudao-module-rental-biz`.
2. Add `20260723_001_xianyu_rental_foundation.sql`.
3. Add disabled-by-default runtime configuration with environment placeholders.
4. Implement canonical JSON, signing, redaction, error classification, and
   MockWebServer contract tests.
5. Implement application/shop query and persistent authorization snapshots.
6. Add bounded product, order, and after-sale sync with raw payloads, cursors,
   run audit, idempotency, and retries.
7. Add authorization/after-sale alerts and safe replay.
8. Add product/SKU mapping, channel-order conversion, device, schedule,
   assignment, and review.
9. Add source-linked operational reports.
10. Add admin operational, alert, report, and sync-monitor pages.

## Excluded Modules

Do not enable member, pay, mall, ERP, customer uni-app, staff uni-app, or Nuxt
Web. Do not implement customer-created orders, payment, deposit, fulfillment,
inspection, maintenance, or any XianGuanJia write action.
