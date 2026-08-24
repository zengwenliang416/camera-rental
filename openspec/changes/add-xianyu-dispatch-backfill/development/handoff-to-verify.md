# Development Handoff To Verify: add-xianyu-dispatch-backfill

## Implemented Slices

- Local-only operator contract for already-shipped Xianyu orders without scanner hardware or another remote shipment request.
- Transactional backend correction covering conversion, assignment, occupied schedule, dispatch, `ADMIN_BACKFILL` Shipment, outbound Delivery, and channel logistics.
- Permission- and status-gated Web-admin dialog based on approved `admin-dialog-v1`.
- Focused backend and admin regression checks for eligibility, tenant isolation, idempotency, conflicts, rollback boundaries, typing, linting, formatting, and whitespace.

## Files Changed

- Backend: rental error codes, Xianyu admin Controller/VO, shipment Mapper, shipment Service, and focused Service test.
- Admin: typed Xianyu API, channel-order page, cohesive backfill dialog, and `zh-CN`/`en` locales.
- SpecNav: active change development packets, validation planning, drift checks, review artifacts, migration-not-required manifest, and this handoff.

## Requirements Covered

- Development code and tests cover A4-A12.
- Static UI implementation and the approved prototype cover the development inputs for A1-A3, A13, and A14.
- Runtime E2E and sensory proof for A1-A3, A13, and A14 remains mandatory in Verification 2.0.

## Prototype Decisions Implemented

- Implemented `ui-html / admin-dialog-v1`.
- Preserved the local-only warning, keyboard entry, existing logistics defaults, actual ship time, reason, validation, loading, error-preserving, success, locale, theme, and narrow-layout intent.
- Did not copy prototype fixtures, mock handlers, or review-only layout glue into production.

## Components Created / Reused / Extracted

- Created `XianyuDispatchBackfillDialog` as one cohesive single-use component.
- Reused the existing Xianyu typed API module, order page, conversion service, assignment service, device dispatch service, shipment persistence, Delivery service, permissions, and locale system.
- No shared hook, store, or parallel backend orchestration was extracted because there is one production use site.

## API / Data Flow Changes

- Added `POST /admin-api/rental/xianyu/order/dispatch-backfill` under `rental:xianyu:ship`.
- Eligible orders are uncancelled status `21` or `22`; pending, refunded, closed, cross-tenant, conflicting, and non-shippable inputs fail before a successful local commit.
- The local path does not read Xianyu write-enabled runtime configuration and does not call `XianyuWriteClient`.
- No database, configuration, dependency, menu, or permission migration is required.

## Tests Added

- Expanded `XianyuOrderShipServiceTest` to 35 passing tests.
- Added cases for success, pending/refunded/closed/cancelled orders, tenant isolation, device and waybill conflicts, conversion, idempotent replay/conflict, existing dispatched assignment reuse, Delivery failure propagation, and zero remote writes.

## Local Validation

- Backend focused Maven test passed: 35 tests, 0 failures, 0 errors, 0 skipped.
- Admin Vue TypeScript, targeted ESLint, and targeted Prettier had earlier interactive passes; final managed reruns remain pending safe dependency recovery.
- `git diff --check` and strict OpenSpec validation passed.
- Real `/rental/order` page rendering was inspected with the built-in browser; the unavailable local backend limited that check to static rendering and network-error handling, not full E2E.

## Known Risks

- Database-backed transaction rollback still needs a deterministic Verification 2.0 oracle; the unit test currently proves propagation, call ordering, and `rollbackFor = Exception.class`.
- User-to-shop authorization is not added; this approved change uses tenant-level shop isolation plus `rental:xianyu:ship`.
- One waybill maps to one device and an unconverted multi-item order uses the first rental item, as explicitly accepted.
- The user authorized a local production commit without push; managed task acceptance receipts will bind to that implementation `HEAD`.
- Project Verification Runtime `2.0.0-alpha.2` is selected, installed, and repaired.

## Items Requiring Six-Domain Verification

- Facticity: bind requirements, acceptance, approved prototype, actual API/VO/Service/UI code, and the no-remote-write claim.
- Static: rerun backend/admin checks through managed signed receipts and verify all changed files are mapped.
- Unit: preserve the 28-test result and add a deterministic database-backed rollback oracle if the approved case plan requires it.
- Redteam: probe pending/refunded/closed/cancelled, cross-tenant, non-shippable device, idempotency tampering, and waybill/device conflict paths with no partial writes or remote calls.
- E2E: use an authorized status `21`/`22` fixture to open the Web dialog, submit keyboard-entered data, observe success, and confirm list refresh; verify hidden/denied states.
- Sensory: verify default, validation, loading, conflict, error, and success states across light/dark, `zh-CN`/`en`, desktop, and narrow layouts.
