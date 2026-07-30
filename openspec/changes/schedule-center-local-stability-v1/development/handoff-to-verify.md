# Development Handoff To Verify: schedule-center-local-stability-v1

## Implemented Slices

- `001-shipping-workbench` through `007-state-decomposition-final` are complete
  for development handoff.
- XianGuanJia configuration now has one supported source: the current tenant's
  persisted `xianyu_application` row.

## Files Changed

- Schedule-center application shell, dashboard, scheduling, orders, devices,
  shipping, exceptions, authentication, overlays, typed state boundaries, and
  focused frontend tests.
- Rental backend management order controller, VOs, services, mapper, and
- focused backend tests.
- Admin XianGuanJia configuration API, panel, persisted runtime resolver,
  tenant-aware jobs, shipment guard, and migrations 029 through 031.
- Xianyu order-sync and field-mapping documentation.

## Requirements Covered

- Responsive shipping workbench and waybill-first outbound sequence.
- Searchable registered available devices.
- Authorized pending-order search by receiver name, exact full phone, or order
  number.
- Complete authorized customer/order verification fields.
- Complete management order customer snapshots under the existing query
  permission.
- Existing tenant, permission, write-enable, idempotency, and server-state
  authority preserved.
- Legacy `XGJ_*`, `rental.xianyu.*`, and `credential_reference` paths removed
  without a compatibility fallback.

## Prototype Decisions Implemented

- Integrated outbound command-center hierarchy.
- Explicit waybill, device, order, rental-period, and permission gates.
- OCR remains an editable review draft.
- No prototype business fixture enters production source.

## Components Created / Reused / Extracted

- Created the shipping workbench panels, controller, read model, protected query
  adapter, and safe error mapping.
- Reused existing AppContext commands, typed API client, permission checks,
  express-company vocabulary, and device drawer.
- Extended existing backend query services rather than adding a parallel route.

## API / Data Flow Changes

- Existing routes are unchanged.
- Management order responses now carry complete receiver snapshots and seller
  remarks while excluding raw payloads, goods blobs, and payment numbers.
- Pending-shipment `keyword` now covers order number, receiver name, and exact
  receiver mobile and returns complete verification fields.
- Private query values and results remain current-page memory state.

## Tests Added

- Backend complete customer response and persisted snapshot tests.
- Pending response field and keyword predicate tests.
- Frontend complete short-lived candidate mapping test.
- Existing permission, write-disable, idempotency, status, and safe-error tests
  remain green.

## Local Validation

- Full rental backend reactor: 244 tests passed.
- Backend package: `mvn -pl yudao-server -am package -DskipTests` passed.
- Admin TypeScript no-emit and production build passed.
- Schedule-center: 74 tests, TypeScript no-emit, and Vite build passed.
- Local migrations 029 through 031 were applied after backup; the legacy
  column is absent and unconfigured application rows are disabled.

## Known Risks

- Live authorized production lookup was not executed.
- Real shipment, production deployment, and third-party writes were not
  executed.
- Historical orders without persisted receiver snapshots require normal detail
  backfill before name/mobile lookup can match them.
- Production migration, deployment, and third-party writes were not executed.

## Items Requiring Six-Domain Verification

- Facticity against a controlled backend record with complete receiver fields.
- Unauthorized and cross-tenant private-query denial.
- Live name/full-phone/order-number query behavior.
- Duplicate shipment submission and write-disabled behavior against a
  controlled backend.
- Responsive sensory review of populated complete-order results.
