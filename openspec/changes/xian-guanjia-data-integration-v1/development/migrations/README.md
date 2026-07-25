# Development Migrations: xian-guanjia-data-integration-v1

## Execution Order

1. Back up the target MySQL schema and confirm the deployment tenant policy.
2. Execute production migrations `001` through `019` in filename order.
3. For local development, `camera-rental-server/scripts/setup-local.sh`
   creates `camera_rental_schema_migration`, records each filename/checksum, and
   skips only an exact previously recorded migration.
4. Confirm every production migration exactly matches its audit copy under
   `development/migrations/`.
5. Deploy the backend containing `yudao-module-rental`, then deploy the admin
   frontend after the menu/i18n migrations are applied.

The production-path SQL is the sole deployment source. The file in this
directory is an exact audit copy required by the development handoff contract;
do not execute both files.

## Validation

- Run the commands recorded in `manifest.json`.
- Confirm all production/audit migration pairs `001` through `019` are equal.
- Confirm `xianyu_order.pay_amount` is `BIGINT NOT NULL DEFAULT 0` after
  migration 002.
- Confirm `rental_order.rent_amount`, `rental_order.refund_amount`, and
  `rental_order_item.rent_amount` are `BIGINT NOT NULL DEFAULT 0` after
  migration 003.
- Confirm migration 007 leaves only the active
  `uk_xianyu_shop_tenant_application_authorize` unique key.
- Confirm migration 010 creates `uk_infra_job_active_handler`.
- Confirm migration 013 creates the seller/status lookup and push retry
  composite indexes.
- Confirm migration 014 creates the default order and schedule admin-page
  composite indexes.
- Confirm migration 015 creates the shop-filtered order admin-page composite
  index.
- Confirm migration 016 adds the webhook processing-attempt token used for
  stale worker recovery.
- Confirm migration 017 adds business-report permissions, indexes, and UTF-8
  menu-name repair for the rental report page.
- Confirm migration 018 grants schedule, sync-run, and standalone report menu
  visibility to roles that already have the rental root menu or report query
  permission.
- Confirm migration 019 creates separate raw-payload and safe-replay
  permissions without granting either permission to roles automatically.
- Confirm migrations 005, 008, 009, 011, and 012 create/update the expected
  permissions, dictionaries, schedule/sync-run menus, and i18n menu keys.
- Live MySQL execution and post-migration queries are required before
  deployment; checksum/file checks alone are insufficient.

## Rollback

- Roll back the application deployment first.
- Do not drop the new tables to resolve an application issue because they
  retain channel and rental audit history.
- Do not narrow `xianyu_order.pay_amount` after values outside 32-bit range
  have been stored. Any schema rollback requires a range audit and a separate
  approved migration.
- Do not narrow converted rental amount columns after values outside 32-bit
  range have been stored. Any schema rollback requires a range audit and a
  separate approved migration.
- Do not restore the old shop uniqueness key without first proving that
  seller/external-shop identity is unique across every authorized brand.
- Menu and dictionary rollback must preserve permissions still referenced by
  deployed backend/frontend code.
- Any destructive rollback must be a separately approved migration with a
  backup, retention review, and documented recovery procedure.
