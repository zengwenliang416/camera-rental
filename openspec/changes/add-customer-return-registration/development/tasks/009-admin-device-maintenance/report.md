# Task Report: 009-admin-device-maintenance

## Status

DONE

## Files Changed

- Rental backend device controller, update request, service, deletion guard,
  reference-count mappers, error codes and focused tests.
- Admin typed API, device list actions, edit dialog, extracted amount/payload
  model, localized copy and focused model tests.
- Permission migration 051, deployment migration list and SpecNav migration
  audit metadata.

## What Changed

- Added `PUT /admin-api/rental/device/update` for only serial number, warehouse,
  purchase amount and enabled state. Device number, category, model and
  lifecycle status are not accepted by the update request.
- Empty serial/warehouse values normalize to `NULL`; purchase amount remains a
  non-negative integer number of cents. Serial uniqueness checks include all
  same-tenant device rows, including logically deleted rows.
- Disabling requires an `AVAILABLE` device without an active assignment.
- Added `DELETE /admin-api/rental/device/delete` with a locked, tenant-scoped
  guard. It rejects non-available or purchase-sourced devices and any
  assignment, schedule, standalone shipment, delivery or return-registration
  history, including logically deleted history. Current active device locks
  also block deletion, while released or elapsed locks do not. The service
  uses the existing logical-delete mapper only after all guards pass.
- Added permission-controlled edit/delete actions, an edit dialog, deletion
  confirmation, list refresh and warehouse/purchase-amount display.
- Added menu permissions `rental:device:update` and `rental:device:delete`;
  migration 051 grants them only to roles already holding device-create
  permission.

## TDD Evidence

- `RentalDeviceAdminServiceTest` covers mutable-field-only updates, empty-value
  normalization, same-tenant serial conflicts, concurrent unique-key
  classification, disable restrictions and guarded logical deletion.
- `RentalDeviceDeletionGuardTest` covers available/unreferenced success and
  every status, purchase-source, active-lock and history-reference rejection
  path.
- `deviceMaintenanceModel.test.ts` covers cents/yuan conversion, mutable-only
  payload construction, empty optional values and invalid amounts.

## Verification Commands

- Focused offline Maven suite: 21 tests passed, 0 failures/errors/skips.
- Offline rental-module Maven compile: passed across the 24-module reactor.
- `node --test --experimental-strip-types tests/deviceMaintenanceModel.test.ts`:
  4 tests passed.
- `pnpm ts:check`: passed.
- `VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`: passed.
- `bash ops/github-deploy/tests/migration-runner-test.sh`: passed.
- `bash -n ops/github-deploy/*.sh ops/github-deploy/tests/*.sh`: passed.
- Migration production/audit copies match SHA-256
  `a5b41f65114631f5d453ba1af4250bac59de5d33d08417e6579c14fa228acbf7`.
- Scoped `git diff --check`: passed before report finalization.

## Concerns

- Migration 051 was applied only to the disposable local E2E database, not to
  production or any shared environment.
- The admin build retains existing unrelated Lightning CSS `*zoom`, dynamic
  import and large-chunk warnings.
- The global SpecNav handoff remains blocked by incomplete review artifacts for
  the earlier task `008-tenant-device-catalog`; no task 009 review-format or
  evidence blocker was reported.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Production release verification must apply migration 051 through the normal
  deployment process and repeat the operational smoke checks against the
  authorized target.

## Adjudication

Local implementation, static/unit validation and authenticated local API/browser
E2E are complete. Acceptance A6 is supported by unit evidence and A7 by
`e2e.md`. Production migration application, commit, push and deployment remain
outside this task.
