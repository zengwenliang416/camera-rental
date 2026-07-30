# Release Notes: schedule-center-local-stability-v1

## Summary

- Production now serves the redesigned schedule center and updated management UI.
- Authorized operators can review complete receiver details and search pending shipments by order number, receiver name, or full phone.
- XianGuanJia credentials, switches, endpoints, and synchronization settings are managed per tenant in the admin application.
- Production release: `b5968cf257b05810033b53f360f636d0535fc5dd`.

## Verification

- Backend reactor and 257 rental tests passed.
- Schedule center passed 77 tests, TypeScript, and production build.
- Admin TypeScript check passed.
- Public admin and schedule-center routes returned HTTP 200 and matching release metadata.

## Known Limitations

- No real XianGuanJia shipment mutation was executed during verification.
- The production persisted write switch is enabled and must be used only by an authorized operator.
- Full application rollback after migration 030 requires restoring the pre-migration database backup with the previous application release.
- CodeGraph remains advisory and unindexed for this change.
