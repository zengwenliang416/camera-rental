# Task Brief: 007-admin-device-category-catalog

## Goal

Allow administrators to classify rental devices by major category and create a
device using only a backend-approved category, model and device-number prefix.

## Parent Artifacts

- `openspec/changes/add-customer-return-registration/requirements.md`
- `openspec/changes/add-customer-return-registration/acceptance.md`
- `openspec/changes/add-customer-return-registration/prototype/handoff.md`

## Vertical Slice

The management device page loads the authoritative catalog from the rental
backend, filters and displays category data, and submits a linked category,
model and device number that the backend validates before persistence.

## In Scope

- Backend category/model catalog, admin catalog endpoint, create validation,
  page filtering and ERP inbound classification for known models.
- Additive `rental_device.category_code` migration with known-model backfill.
- Admin category filter, category column and category/model linked selects.
- Focused backend, frontend model, type-check, build and browser validation.

## Out Of Scope

- Applying migration 049 to any database.
- Creating or editing catalog values from the management frontend.
- Changing customer return matching, order lifecycle, assignment, schedule,
  warehouse return or inspection behavior.
- Git commit, push, deployment or production verification.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-server/sql/mysql/migrations/20260825_049_rental_device_category.sql`
- `camera-rental-admin/src/api/rental/device.ts`
- `camera-rental-admin/src/views/rental/device`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/locales/en.ts`
- `camera-rental-admin/tests/deviceCatalogModel.test.ts`
- `ops/github-deploy/migrations.txt`
- `openspec/changes/add-customer-return-registration`

## Interfaces / Seams

- `GET /admin-api/rental/device/catalog`
- `GET /admin-api/rental/device/page`
- `POST /admin-api/rental/device/create`
- `RentalDeviceModelCatalog`
- `rental_device.category_code`

## Components To Create

- `RentalDeviceModelCatalog`
- `RentalDeviceCategoryRespVO`
- `deviceCatalogModel.ts`

## Components To Reuse

- Existing rental device controller, service, MyBatis Plus page query and DO.
- Existing Element Plus form, select, table and loading patterns.
- Existing device-number short-code generation and ERP inbound flow.

## Components To Extract

- Frontend catalog lookup, membership validation and default device-number
  generation are isolated in `deviceCatalogModel.ts`.

## API / Data Flow Contracts

- The frontend fetches category and model choices from the backend catalog and
  never maintains an independent business matrix.
- Device creation submits `categoryCode`, `equipmentModelCode` and `deviceNo`;
  the backend rejects an unknown category/model pair or mismatched prefix.
- Known ERP model codes receive their catalog category; unknown models remain
  supported with a null category.

## State / Error / Empty / Loading Behavior

- Loading: catalog-backed controls remain unavailable until catalog loading
  completes.
- Empty: no selected category produces an empty model option list.
- Error: catalog load and create failures use the existing management request
  error surface.
- Disabled: model selection is disabled until a category is selected.
- Permission: the existing rental-device query/create backend permissions remain
  authoritative.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `cd camera-rental-admin && node --test --experimental-strip-types tests/deviceCatalogModel.test.ts`
- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-admin && VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceModelCatalogTest,RentalDeviceCodeStandTest,RentalDeviceAdminServiceTest,RentalDeviceInboundCategoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `git diff --check -- camera-rental-admin camera-rental-server/yudao-module-rental camera-rental-server/sql/mysql/migrations/20260825_049_rental_device_category.sql openspec/changes/add-customer-return-registration`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- Stop before applying migration 049 without an explicitly identified database
  target and separate authorization.
- Stop if the frontend needs a second hard-coded catalog or if ERP unknown-model
  creation would be removed.

## Unsafe Assumptions

- Do not assume frontend validation protects direct API callers.
- Do not assume every historical `equipment_model_code` belongs to the current
  catalog.
- Do not treat same-origin mock browser evidence as real backend E2E.
