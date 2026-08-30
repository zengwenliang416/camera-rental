# Task Report: 008-tenant-device-catalog

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-server/sql/mysql/migrations/20260826_050_rental_device_catalog_management.sql`
- `camera-rental-admin/src/api/rental/device.ts`
- `camera-rental-admin/src/views/rental/device`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/locales/en.ts`
- `camera-rental-admin/tests/deviceCatalogModel.test.ts`
- `ops/github-deploy/migrations.txt`
- `openspec/changes/add-customer-return-registration`

## What Changed

- Added tenant-aware device-category and device-model persistence, normalized
  uniqueness rules, current-catalog seed data and model sequence state.
- Added permission-protected catalog query and category/model creation APIs.
- Kept the backend authoritative for category/model membership and canonical
  manual device-number composition from the configured model prefix.
- Added category/model quick-create dialogs to the existing device-create flow,
  followed by catalog refresh and automatic selection of the saved entry.
- Preserved ERP inbound behavior: known models use the tenant catalog and
  unknown models retain the existing uncategorized path.

## TDD Evidence

- `RentalDeviceCatalogServiceTest`, `RentalDeviceAdminServiceTest`,
  `RentalDeviceInboundServiceTest`, `RentalDeviceInboundCategoryTest`,
  `RentalDeviceCodeStandTest`, `RentalDeviceCodeTest` and
  `ReturnSerialNormalizerTest` ran 33 tests with zero failures or errors.
- `deviceCatalogModel.test.ts` ran three tests covering backend-provided model
  options, category/model membership and prefix-plus-suffix preview behavior.
- The migration runner covered first application, idempotent replay and changed
  checksum rejection.

## Verification Commands

- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceCatalogServiceTest,RentalDeviceAdminServiceTest,RentalDeviceInboundServiceTest,RentalDeviceInboundCategoryTest,RentalDeviceCodeStandTest,RentalDeviceCodeTest,ReturnSerialNormalizerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `node --test --experimental-strip-types tests/deviceCatalogModel.test.ts`
- `pnpm ts:check`
- `VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`
- `bash ops/github-deploy/tests/migration-runner-test.sh`
- `bash -n ops/github-deploy/apply-migrations.sh`
- `cmp` and `shasum -a 256` for both migration 050 copies
- Scoped `git diff --check`

## Concerns

- Migration 050 has not been applied to a database by this task.
- Browser interaction against the real rental backend remains a formal
  Verification responsibility.
- The admin build retains unrelated legacy CSS and large-chunk warnings.

## Scope Deviations

- Implementation stayed within the declared allowed files and preserved every
  listed non-goal.

## Follow-up Needed

- Run the approved Verification V2 case snapshot against an identified runtime.
- Record migration application separately in Operations before project
  deployment or archive readiness is claimed.
