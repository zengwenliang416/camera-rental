# Development Migrations: add-customer-return-registration

## Execution Order

1. `20260801_034_rental_delivery_channel_orders.sql`
2. `20260801_036_customer_return_registration.sql`

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
