# Development Handoff To Verify: redesign-admin-device-schedule-v2

## Implemented Slices

- Server-authoritative 14/30/90 day schedule workbench with device-lane pagination.
- Pending allocation, order detail, device candidate and device detail read models.
- Classified device locks and final-assignment lock recheck.
- Element Plus V2 timeline, metrics, filters, exception queue and right-side drawers.
- Candidate confirmation through the existing transactional assignment endpoint.

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `camera-rental-admin/src/api/rental`
- `camera-rental-admin/src/views/rental/schedule`
- `camera-rental-admin/src/locales`
- `camera-rental-admin/tests/scheduleModel.test.ts`
- `camera-rental-server/sql/mysql/migrations/20260808_046_rental_device_schedule_v2.sql`

## Requirements Covered

- All requirements in `specs/admin-device-schedule-workbench/spec.md` have an implementation path.
- Production-data browser verification remains pending until the Woodpecker deployment completes.

## Prototype Decisions Implemented

- One warehouse per tenant; no warehouse selector.
- Pending allocation does not create effective occupancy.
- Occupancy is a half-open interval and remains effective through return inspection.
- 14/30/90 day horizontal windows support cross-month rentals and continuation markers.
- Hundreds of devices use server-side search and 25/50/100 pagination.
- Order, device, logistics and candidate decisions use right-side drawers.

## Components Created / Reused / Extracted

- Created workbench, timeline, filters, metrics, pending allocation, exception and drawer components.
- Reused existing permissions, Element Plus, tracking detail/refresh and transactional assignment APIs.

## API / Data Flow Changes

- Added `/rental/schedule/workbench`.
- Added pending allocation page, order schedule detail, item candidates and device schedule detail.
- Workbench lists use local database snapshots and do not call external logistics providers.
- Candidate confirmation reuses `/rental/device/assign`, which rechecks effective schedules and active locks transactionally.

## Tests Added

- Added `RentalScheduleWorkbenchServiceTest`.
- Extended existing device-lock, device-operations and assignment tests in the preceding classified-lock slice.
- Replaced V1 schedule model tests with 14/30/90 window, clipping, cross-month, query and pagination tests.

## Local Validation

- Backend compilation passed.
- Focused backend suite passed: 17 tests, 0 failures.
- Full rental-biz run reached 453 tests with one new workbench fixture failure; the service added a defensive half-open overlap filter and the failing focused test then passed.
- `pnpm test:schedule`: 7 passed.
- `pnpm ts:check`: passed.
- `pnpm build:prod`: passed with existing project warnings.
- Migration runner test, migration copy comparison, strict OpenSpec validation and `git diff --check`: passed.

## Known Risks

- Migration 046 must be applied and recorded before production activation.
- Production data may expose model/status values not represented in the current page-derived model filter options.
- Browser verification with the authenticated production tenant is still required after deployment.

## Items Requiring Six-Domain Verification

- Real-data search, pagination and 14/30/90 window behavior.
- Candidate refresh after a stale/conflicting assignment.
- Device and logistics drawer content and redaction.
- Light/dark theme and narrow viewport sensory review.
- Production migration ledger and deployed commit identity.
