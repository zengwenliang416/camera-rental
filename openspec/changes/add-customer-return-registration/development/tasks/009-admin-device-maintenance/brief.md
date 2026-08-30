# Task Brief: 009-admin-device-maintenance

## Goal

Allow an authorized administrator to correct mutable physical-device metadata
and logically delete an unused device created by mistake without bypassing
device lifecycle or historical-data protections.

## Parent Artifacts

- `openspec/changes/add-customer-return-registration/requirements.md`
- `openspec/changes/add-customer-return-registration/acceptance.md`
- `openspec/changes/add-customer-return-registration/prototype/handoff.md`

## Vertical Slice

The existing rental-device list exposes permission-controlled edit and delete
actions. Edit persists only server-approved mutable fields. Delete confirms the
operator intent, locks the device, proves it has no source or business-history
references, performs a logical delete and refreshes the list.

## In Scope

- Backend update/delete request models, endpoints, service methods and business
  validation.
- Transactional row locking, tenant-scoped serial uniqueness and a reusable
  deletion-reference guard.
- Incremental update/delete permissions and role grants.
- Admin edit dialog, delete confirmation, API methods, localization and
  permission-controlled actions.
- Focused backend/frontend tests, migration checks, type-check and build.

## Out Of Scope

- Editing device number, category, model or lifecycle status.
- Editing or deleting device category/model catalog rows.
- Force deletion, cascade deletion or deletion of business history.
- Applying migrations, commit, push, deployment or production mutation.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-server/sql/mysql/migrations/20260830_051_rental_device_maintenance_permissions.sql`
- `camera-rental-admin/src/api/rental/device.ts`
- `camera-rental-admin/src/views/rental/device`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/locales/en.ts`
- `camera-rental-admin/tests/deviceMaintenanceModel.test.ts`
- `ops/github-deploy/migrations.txt`
- `openspec/changes/add-customer-return-registration`

## Interfaces / Seams

- `PUT /admin-api/rental/device/update`
- `DELETE /admin-api/rental/device/delete?id={id}`
- `rental:device:update`
- `rental:device:delete`
- `RentalDeviceAdminService`
- `RentalDeviceDeletionGuard`

## Components To Create

- `RentalDeviceUpdateReqVO`
- `RentalDeviceDeletionGuard`
- `DeviceEditDialog.vue`
- `deviceMaintenanceModel.ts`

## Components To Reuse

- Existing rental device controller, mapper, tenant filtering and row-lock
  patterns.
- Existing assignment, schedule, delivery-device and return-registration
  mappers as reference authorities.
- Existing Element Plus dialog, confirmation, permission and request-error
  patterns.

## Components To Extract

- Delete reference checks are isolated in `RentalDeviceDeletionGuard` rather
  than duplicated in the controller or frontend.
- Edit form conversion is isolated in `deviceMaintenanceModel.ts` so the dialog
  does not duplicate payload normalization.

## API / Data Flow Contracts

- Edit submits only `id`, `serialNumber`, `warehouseCode`, `purchaseAmount` and
  `enabled`; the server never accepts identity or lifecycle fields from this
  endpoint.
- Empty serial and warehouse values normalize to `NULL`; purchase amount is a
  non-negative integer number of cents.
- Disabling requires an `AVAILABLE` device with no active assignment.
- Delete locks the device row before checking status, purchase source,
  assignments, schedules, delivery relations and return-registration device
  relations, then calls the existing logical-delete mapper operation.
- The frontend displays backend errors and never predicts deletion safety.

## State / Error / Empty / Loading Behavior

- Loading: edit save and delete actions expose per-action loading states.
- Empty: optional serial and warehouse fields submit as empty values that the
  backend normalizes to `NULL`.
- Error: duplicate serial, unsafe disable and blocked delete errors remain in
  the dialog/list through the existing request error surface.
- Disabled: device number, category, model and lifecycle status are read-only.
- Permission: edit/delete actions and backend endpoints require independent
  update/delete permissions.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceAdminServiceTest,RentalDeviceDeletionGuardTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests compile`
- `cd camera-rental-admin && node --test --experimental-strip-types tests/deviceMaintenanceModel.test.ts`
- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-admin && VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`
- `bash ops/github-deploy/tests/migration-runner-test.sh`
- `git diff --check -- camera-rental-admin camera-rental-server/yudao-module-rental camera-rental-server/sql/mysql/migrations/20260830_051_rental_device_maintenance_permissions.sql openspec/changes/add-customer-return-registration`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- Stop if deletion would require cascading, deleting history or bypassing a
  business-reference check.
- Stop before applying migration `051`, committing, pushing or deploying.

## Unsafe Assumptions

- A hidden frontend button is not an authorization boundary.
- `AVAILABLE` alone does not prove a device has no historical references.
- Logical deletion does not make a serial number safe to reuse.
- Row locking must serialize deletion with assignment and lifecycle writes.
