# Task Brief: 008-tenant-device-catalog

## Goal

Allow a store administrator with `rental:device:create` permission to add
tenant-scoped device categories and models with numbering prefixes from the
existing device-create dialog.

## Vertical Slice

The existing device-create dialog loads the tenant catalog, allows an
authorized administrator to add a category or model, refreshes the catalog and
uses the selected model prefix to create one canonical physical-device number.

## In Scope

- Additive tenant-aware category/model tables and current-catalog seed data.
- Backend catalog query and category/model create endpoints.
- Manual device-number composition from a model prefix and administrator input.
- Existing-page quick-create dialogs with refresh and auto-selection.
- Focused backend/frontend tests, type-check and build.

## Out Of Scope

- A separate catalog page or menu.
- Applying migrations to any database.
- Editing or deleting existing catalog rows.
- Order, assignment, schedule, dispatch, return or inspection changes.
- Commit, push, deployment or production verification.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-server/sql/mysql/migrations/20260826_050_rental_device_catalog_management.sql`
- `camera-rental-admin/src/api/rental/device.ts`
- `camera-rental-admin/src/views/rental/device`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/locales/en.ts`
- `camera-rental-admin/tests/deviceCatalogModel.test.ts`
- `ops/github-deploy/migrations.txt`
- `openspec/changes/add-customer-return-registration`

## Reuse

- Existing `Dialog`, Element Plus `el-select` footer slots and
  `open('create')`/`success` form pattern.
- Existing `TenantBaseDO`, MyBatis Plus mapper and tenant unique constraints.
- Existing `RentalDeviceCode` normalization and canonical `01-999` formatting.
- Existing `rental:device:create` permission.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceCatalogServiceTest,RentalDeviceAdminServiceTest,RentalDeviceInboundServiceTest,RentalDeviceInboundCategoryTest,RentalDeviceCodeStandTest,RentalDeviceCodeTest,ReturnSerialNormalizerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-admin && node --test --experimental-strip-types tests/deviceCatalogModel.test.ts`
- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-admin && VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`
- `git diff --check -- camera-rental-admin camera-rental-server/yudao-module-rental camera-rental-server/sql/mysql/migrations/20260826_050_rental_device_catalog_management.sql openspec/changes/add-customer-return-registration`

## Stop Conditions

- Stop if catalog authority moves into the frontend.
- Stop if tenant isolation or normalized uniqueness cannot be proven.
- Stop before applying migration `050` to an unidentified database.
- Stop before commit, push, deployment or production mutation.
