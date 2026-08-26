# Development Migrations: add-customer-return-registration

## Execution Order

1. `20260801_034_rental_delivery_channel_orders.sql`
2. `20260801_036_customer_return_registration.sql`
3. `20260803_041_rental_device_short_codes.sql`
4. `20260825_049_rental_device_category.sql`
5. `20260826_050_rental_device_catalog_management.sql`

The files in this directory are exact audit copies of the production migrations
listed by `production_path` and pinned by SHA-256 in `manifest.json`.

## Validation

- Run `bash ops/github-deploy/tests/migration-runner-test.sh`.
- Run `sha256sum` against each audit copy and its production path and compare
  both values with `manifest.json`.
- The production release runner applies the files listed in
  `ops/github-deploy/migrations.txt` before switching the active release.

## Rollback

- The migration is additive. Rollback retains schema and data, disables the
  menu and revokes active links. Destructive table/column removal requires a
  separate reviewed migration after confirming no Return Delivery references.
- Migration 041 preserves every previous `device_no` in `legacy_device_no`.
  Rolling back application code can continue resolving those legacy values,
  but restoring them as primary device numbers requires a separately reviewed
  data migration.
- Migration 049 is additive. Application rollback may leave
  `rental_device.category_code` and its index in place; destructive removal or
  reversal of known-model backfill requires a separately reviewed migration.
- Migration 050 is additive. Application rollback may leave the tenant catalog
  tables and seed rows in place. Removing either table or reusing an allocated
  sequence requires a separately reviewed destructive/data migration.
