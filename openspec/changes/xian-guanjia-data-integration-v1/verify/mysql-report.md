# MySQL 8.4 Migration Verification

Recorded on 2026-07-24 against a disposable `mysql:8.4` Docker container.
The container used an isolated database and was removed after verification.

## Passed

- Imported the upstream `ruoyi-vue-pro.sql` and `quartz.sql` baseline.
- Applied rental migrations `001` through `017` in filename order.
- Verified a second migration-run pass by matching every recorded filename and
  SHA-256 checksum, then skipping all 17 already-applied files.
- Verified `xianyu_push_event.processing_token` exists.
- Verified `idx_xianyu_push_event_retry`,
  `idx_xianyu_order_admin_shop_page`, and
  `idx_rental_schedule_admin_default` exist.
- Verified migration 017 report indexes and UTF-8 report-menu repair.
- Executed the webhook event `ON DUPLICATE KEY UPDATE id=id` pattern twice for
  the same `(tenant_id, dedupe_key)` and observed exactly one stored row.

## Key Output

```text
migration_count=17
processing_token_column=1
push_retry_index=4
order_shop_page_index=4
schedule_default_index=3
report_menu_name=经营报表
rows_after_duplicate=1
mysql-verification-complete
```

The index values are the number of indexed columns returned by
`information_schema.statistics`, not duplicate index counts.

## Not Covered

- Authenticated backend startup and API smoke tests.
- Browser E2E and sensory review.
- Approved-environment rollout and production data distribution checks.
- Browser transition-flow and permission-denied click-through checks.

Real MySQL concurrent allocation and representative schedule query-plan evidence
are covered separately in `verify/mysql-concurrency-report.md`.

## Runtime Schema Patch 020

Recorded on 2026-07-25 against the active local `ruoyi-vue-pro` database used
by backend `48082`.

- Applied
  `camera-rental-server/sql/mysql/migrations/20260725_020_xianyu_raw_payload_version_width.sql`.
- Confirmed `xianyu_raw_payload.schema_version` is `varchar(64)`.
- Confirmed `xianyu_raw_payload.redaction_version` is `varchar(64)`.
- Confirmed `xianyu_push_event.processing_token` exists as `varchar(64)`.
- Confirmed the production SQL and `openspec` audit copy have identical
  SHA-256:
  `b5cf0cc4139161c50dda0a6dbd43fde07147bcd6fc921c128e3debb4f43990b1`.

## Runtime Job Seed Patch 021

Recorded on 2026-07-25 against the active local `ruoyi-vue-pro` database used
by backend `48082`.

- Applied
  `camera-rental-server/sql/mysql/migrations/20260725_021_xianyu_product_after_sale_jobs.sql`
  twice.
- Confirmed exactly two active rows exist for `xianyuProductSyncJob` and
  `xianyuAfterSaleSyncJob`.
- Confirmed both rows have status `1` and cron `0 0/10 * * * ?`.
- Confirmed the production SQL and `openspec` audit copy have identical
  SHA-256:
  `eb7689e1b3589f134dd9b81bc853e3acc7b1a5f884c2b6a6b5c426d632d86c9f`.
