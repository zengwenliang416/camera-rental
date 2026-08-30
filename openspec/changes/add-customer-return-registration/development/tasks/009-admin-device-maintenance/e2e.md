# E2E Evidence: 009-admin-device-maintenance

## Date And Scope

- Executed on 2026-08-30 in an isolated local environment.
- Backend: `http://127.0.0.1:48080`.
- Admin: `http://127.0.0.1:5173`.
- Temporary MySQL and Redis used non-default local ports and disposable
  containers. The host MySQL on port `3306` and production services were not
  accessed.
- Quartz remained in standby. No synchronization job, deployment, commit or
  push was performed.

## Database And API Evidence

- Applied the base schema, Quartz schema and migrations `001` through `051` to
  the temporary MySQL 8.4 database. The database contained 106 tables,
  including 21 `rental_*` tables.
- Migration 051 created independent update and delete permissions.
- Created eight isolated devices through the real create API and added local
  test references for status, assignment, purchase source, schedule, delivery
  and return-registration cases.
- Mutable-field update persisted serial number, warehouse, purchase amount and
  enabled state while request-supplied identity and lifecycle fields remained
  unchanged.
- Empty serial and warehouse values normalized to database `NULL`.
- Duplicate serial returned business error `1040002023`.
- Unsafe disable returned business error `1040002024`.
- A clean device was logically deleted through the real delete API. The active
  device count changed from eight to seven and the deleted row had
  `deleted = 1`.
- Non-available, purchase-sourced, assigned, scheduled, delivery-related and
  return-registration-related devices returned business error `1040002025`
  with the expected reason. All protected rows remained active.
- Focused regression coverage additionally verifies that active device locks
  and standalone shipment history block deletion. Released or elapsed locks do
  not block deletion. These two added guard paths were not replayed through the
  disposable browser/API environment.

## Permission Evidence

- A temporary query-only user received only `rental:device:query`.
- `GET /admin-api/rental/device/page` and the catalog endpoint returned code
  `0` with seven devices.
- Direct update and delete requests returned `403`.
- Database comparison before and after the denied requests confirmed that the
  target device was unchanged.
- The test-only role relation was inserted directly in SQL, so its stale
  `menu_role_ids` Redis entry was deleted before the final permission run.
  Normal permission-management APIs already perform this cache eviction.

## Browser Evidence

- The administrator device page rendered seven active devices.
- The edit dialog disabled device number, category, model and lifecycle
  status. Serial number, warehouse and purchase amount remained editable.
- Editing device `P4P-902` updated the visible warehouse to `E2E-WH-UI` and
  purchase amount to `123.45`; the database stored `12345` cents.
- Cancelling the delete confirmation preserved the row and the seven-device
  total.
- Confirming deletion of `P4P-908` displayed
  `设备不能删除：设备存在客户退回登记`; the row and total remained unchanged.
- The query-only user saw seven rows and seven QR actions, but zero create,
  edit or delete actions.
- Initial browser replay exposed unhandled promise warnings for confirmation
  cancellation and rejected deletes. The device page now catches those
  already-surfaced failures. A fresh-tab replay produced zero console errors
  and zero unhandled component-event warnings.

## Automated Regression

- Focused backend suite: 21 tests passed, 0 failures/errors/skips.
- Frontend device-maintenance model: 4 tests passed.
- `pnpm ts:check`: passed.
- `VITE_BASE_URL=http://127.0.0.1:5173 pnpm build:local`: passed with existing
  unrelated Lightning CSS, dynamic-import and large-chunk warnings.

## Video Evidence

- Recorded on 2026-08-30 against the isolated real backend and admin runtime:
  `evidence/video/2026-08-30-task-009-admin-device-maintenance-e2e.webm`.
- The recording is 27.08 seconds, VP8, 1440 x 900 at 25 fps. SHA-256:
  `5893d60e44a34b144a3d88e5449840ebb31d8acbf89fa831789624c90a5697ef`.
- The machine-readable assertion record is
  `evidence/video/2026-08-30-task-009-admin-device-maintenance-e2e.json`.
- All six recorded assertions passed: seven-device initial rendering, immutable
  identity/lifecycle fields, persisted edit refresh, delete cancellation,
  successful clean-device deletion and blocked history-device deletion.
- One non-core remote demo avatar image returned HTTP 503 during replay. Login,
  device catalog/page, update and delete requests completed successfully, and
  no page exception occurred.

## Result

Acceptance assertion `A7` is supported by authenticated local API and browser
E2E evidence, including the recorded video above. Production migration
application, deployment and user acceptance remain separate operations evidence.
