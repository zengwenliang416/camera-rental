# Development Migrations: redesign-admin-device-schedule-v2

## Execution Order

- Apply `20260808_046_rental_device_schedule_v2.sql` after the previously
  recorded migrations and before activating the V2 backend.

## Validation

- The production SQL and this audit copy must remain byte-identical.
- The deployment migration runner must record filename and checksum before the
  application is considered active.

## Rollback

- Prefer application rollback while retaining the additive lock table.
- Destructive rollback requires an approved export and data-retention review.
