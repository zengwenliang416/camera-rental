# Deploy Plan

## Environment

- Target: project deployment for the camera rental platform.
- Components: `camera-rental-server`, `camera-rental-admin`,
  `camera-rental-staff`.
- Runtime safety: keep `XGJ_WRITE_ENABLED=false` for normal deployment unless a
  controlled shop write test is explicitly approved.

## Command

```bash
shasum -a 256 camera-rental-server/sql/mysql/migrations/20260726_023_xianyu_ship_workflow.sql
pnpm --dir camera-rental-admin type-check
pnpm --dir camera-rental-staff build:h5
cd camera-rental-server && mvn -pl yudao-module-rental -am test
```

## Config / Secrets

- XianGuanJia credentials remain environment-only.
- Do not commit `XGJ_APP_SECRET`, shipment OCR keys, SSH keys, or production
  passwords.
- Required safe default: `XGJ_WRITE_ENABLED=false`.

## Migrations

- Manifest: `development/migrations/manifest.json`
- Deployment evidence: `operations/migration-deployment.json`
- Apply `camera-rental-server/sql/mysql/migrations/20260726_023_xianyu_ship_workflow.sql`
  before starting the updated backend in an environment that does not already
  contain `rental_device_shipment` and menu ids `7070`, `7071`.

## Smoke Checks

- Admin `/rental/order` renders the shipment workbench and shows write-disabled
  state when `XGJ_WRITE_ENABLED=false`.
- Staff H5 shipment page can search a pending candidate and blocks submit until
  required waybill/device/order fields are present.
- Backend config endpoint reports write operations disabled in safe mode.
- Mock-server shipment path can be repeated in a non-production environment
  before any real channel write.

## Owner

- Operator: 老大.
- Implementer/verification recorder: 小G.

## Deploy Window

- Use a low-traffic maintenance window because the migration adds a shipment
  audit table and permission menu records used by the updated UI.
