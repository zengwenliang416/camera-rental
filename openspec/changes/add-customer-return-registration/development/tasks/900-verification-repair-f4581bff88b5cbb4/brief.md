# Verification Repair: 900-verification-repair-f4581bff88b5cbb4

## Goal

Repair test_defect for return-success-idempotent.

## Frozen Failure

- Failure: `failure-4b6942602a78ba59d4654977c66e931acb68a958042619b6b822df747e78f92d`
- Attempt: `attempt-20260830081458-316899b22249fd11`
- Classification: `test_defect`

## Scope

- `camera-rental-admin/src/api/rental/device.ts`
- `camera-rental-admin/src/api/rental/returnRegistration.ts`
- `camera-rental-admin/src/locales/en.ts`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/views/rental/device/**`
- `camera-rental-admin/src/views/rental/return-registration/**`
- `camera-rental-admin/tests/deviceCatalogModel.test.ts`
- `camera-rental-server/sql/mysql/migrations/**`
- `camera-rental-server/yudao-module-infra/src/main/java/**`
- `camera-rental-server/yudao-module-infra/yudao-module-infra-api/**`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/**`
- `camera-rental-web/**`
- `openspec/changes/add-customer-return-registration/**`
- `ops/github-deploy/**`
- `ops/rustfs/**`
- `tests/specnav/customer-return-registration.js`
