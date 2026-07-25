# Task Brief: 006-device-allocation

## Goal

Equipment operator can assign one physical device to an eligible rental-order
item, creating exactly one non-overlapping occupied schedule or receiving a
typed local conflict result.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/data-flow-map.md`

## Vertical Slice

The backend accepts a local assignment command, returns a prior accepted
assignment on idempotent replay, validates the concrete device and rental item,
locks the device and item inside one transaction, rechecks effective occupied
periods with half-open overlap semantics, and writes the schedule plus
assignment together. No HTTP endpoint is introduced yet.

## In Scope

- Rental device, schedule, and assignment DO/mapper boundaries for the
  foundation tables already present in migration 001.
- Transactional local assignment service and typed domain failures.
- Device enabled/lifecycle, equipment-model, order eligibility, item capacity,
  date-range, idempotency, and effective-schedule conflict validation.
- Focused behavior tests for accepted, replayed, overlapping, adjacent,
  mismatched, full-item, and persistence-failure paths.
- Device scheduling documentation and task evidence.

## Out Of Scope

- Controllers, permissions, admin UI, device registration CRUD, device status
  transitions, swaps, cancellation, renewal, early return, inspection,
  maintenance, reporting, customer flows, and every XianGuanJia write.
- New DDL: the needed tables and indexes are already in the additive
  foundation migration and are not changed by this slice.

## Files Allowed

- `camera-rental-server/yudao-module-rental/**`
- `docs/domain/device-scheduling.md`
- `openspec/changes/xian-guanjia-data-integration-v1/**`

## Interfaces / Seams

- `RentalDeviceAssignmentService` owns one local transaction.
- `RentalDeviceMapper`, `RentalOrderItemMapper`, and `RentalOrderMapper`
  provide row locks; `RentalScheduleMapper` provides the effective overlap
  lock query; `RentalDeviceAssignmentMapper` owns replay and item-capacity
  lookups.
- A later authorized controller maps the typed domain result/errors to API
  responses and includes permission checks.

## Components To Create

- Device, schedule, and assignment DO/mapper boundaries.
- Assignment command/result, typed domain exception, and service.

## Components To Reuse

- Existing tenant/audit base DOs, MyBatis Plus lock helpers, Spring
  transactions, rental order/item persistence, and foundation migration schema.

## Components To Extract

- No extraction is needed: assignment coordination is the sole domain
  transaction and all persistence concerns remain in dedicated mappers.

## API / Data Flow Contracts

- Input: `{ orderItemId, deviceId, occupyStartDate, occupyEndDateExclusive,
  idempotencyKey }`.
- Success: `{ assignmentId, scheduleId, deviceId, occupyStartDate,
  occupyEndDateExclusive }`.
- Adjacent half-open dates are accepted. Any effective overlap uses
  `newStart < existingEndExclusive && newEndExclusive > existingStart`.
- Replayed idempotency returns the accepted assignment and creates no records.

## State / Error / Empty / Loading Behavior

- Loading: a future UI disables repeated submission while the backend remains
  authoritative.
- Empty: missing device, item, or order yields a typed local domain error.
- Error: disabled/non-available or model-mismatched device, ineligible order,
  full item, invalid period, and schedule conflict reject before any assignment
  write.
- Disabled: no local assignment endpoint exists in this slice.
- Permission: a future controller owns backend permission checks.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceAssignmentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `git diff --check -- camera-rental-server/yudao-module-rental docs/domain/device-scheduling.md openspec/changes/xian-guanjia-data-integration-v1`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- Need for new API/UI behavior, lifecycle transition, schedule mutation flow,
  DDL change, or a third-party write.

## Unsafe Assumptions

- This first assignment slice uses the existing order status
  `PENDING_ALLOCATION`. A later fulfillment-status slice decides when an order
  becomes fully allocated across all items.
- All future occupied-schedule writers must acquire the same `rental_device`
  row lock before querying or inserting schedules.
