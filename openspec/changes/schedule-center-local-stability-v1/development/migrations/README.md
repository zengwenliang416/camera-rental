# Persistent Data Changes: schedule-center-local-stability-v1

## Execution Order

1. `20260729_029_xianyu_admin_managed_config.sql`
2. `20260730_030_xianyu_remove_legacy_credential_reference.sql`
3. `20260730_031_xianyu_disable_unconfigured_applications.sql`

## Validation

- Confirm `xianyu_application.credential_reference` no longer exists.
- Confirm every enabled application has both a persisted AppKey and encrypted
  AppSecret.
- Confirm unconfigured applications have integration, write, and job switches
  disabled.
- Confirm application, shop, order, raw payload, review, device, assignment,
  shipment, and schedule row counts do not decrease during migration.

## Rollback

- There is intentionally no application-only compatibility rollback after
  migration 030.
- Restore the pre-migration database backup together with the matching old
  application version if a full rollback is required.
