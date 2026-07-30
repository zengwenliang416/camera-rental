# Rollback Plan

## Triggers

- Backend startup reports encryption, schema, or migration errors.
- Admin or schedule-center public smoke checks fail after activation.
- Authorized order data crosses a tenant or permission boundary.
- Shipment writes produce an incorrect state transition or duplicate mutation.

## Rollback Command

```bash
systemctl stop camera-rental-server.service
ln -sfn /opt/camera-rental/releases/5e86ddc231ff26bafe7ff93d2c0bfa7bd1c1cc57 /opt/camera-rental/current
# Restore the compressed database dump from:
# /opt/camera-rental/backups/pre-admin-xianyu-20260730T110043
systemctl start camera-rental-server.service
nginx -t && systemctl reload nginx
```

## Data Recovery

- Migration 030 has no application-only compatibility rollback because it
  removes `credential_reference`.
- Restore the full pre-migration database backup together with the previous
  application release; do not roll application code back alone.
- Preserve current logs and export any post-migration business writes before
  restoring the database.

## Verification

- Confirm `camera-rental-server.service` is active.
- Confirm both public admin routes return HTTP 200.
- Confirm the release-info endpoint reports the previous release.
- Confirm restored table counts and XianGuanJia application fields match the
  backup baseline before reopening operations.
