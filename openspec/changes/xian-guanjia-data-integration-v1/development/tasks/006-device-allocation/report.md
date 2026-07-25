# Task Report: 006-device-allocation

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/.../dal/{dataobject,mysql}/rental/`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/.../service/RentalDeviceAssignment*.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/.../service/RentalDeviceAssignmentServiceImplTest.java`
- `docs/domain/device-scheduling.md`
- `openspec/changes/xian-guanjia-data-integration-v1/development/tasks/006-device-allocation/`

## What Changed

- Added physical-device, occupied-schedule, and assignment persistence boundaries for the existing foundation tables.
- Added a transaction-scoped assignment service that locks the device, order item, and order before locking and rechecking overlapping effective schedules.
- Enforced `AVAILABLE` and enabled device state, exact equipment-model match, `PENDING_ALLOCATION` order state, unfilled item quantity, valid half-open dates, and request idempotency.
- Added typed domain errors for invalid input, eligibility failures, conflicts, and mismatched idempotency-key reuse.
- An accepted assignment writes one `EFFECTIVE` schedule and one `ASSIGNED` link in the same transaction. Replays return the stored schedule dates and never create duplicates.

## TDD Evidence

- Eight focused tests cover lock ordering, accepted assignment, exact replay, key-reuse rejection, overlap rejection, adjacent ranges, model mismatch, item capacity, and schedule-write failure before assignment write.

## Verification Commands

- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=RentalDeviceAssignmentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `git diff --check -- camera-rental-server/yudao-module-rental docs/domain/device-scheduling.md openspec/changes/xian-guanjia-data-integration-v1`
- Credential and XianGuanJia write-path scans passed with no real test credential or write endpoint match.

## Concerns

- The locking SQL and transaction boundary compile and the lock order is unit-tested, but this task did not run a concurrent transaction test against a real MySQL instance. Deployment verification must exercise two simultaneous overlapping assignments.

## Scope Deviations

No scope deviations were recorded for this slice.
## Follow-up Needed

- `007-admin-operations` must add the authorized controller, permission checks, device CRUD, schedule/assignment queries, bilingual light/dark UI, and user-facing conflict handling.
- A later lifecycle slice must handle cancellation, replacement, renewal, early return, inspection, maintenance, and the transition to fully allocated order status.

## Adjudication

No task-level blocker remains. The MySQL concurrency check is a documented six-domain verification requirement, not a reason to weaken the device-row-lock invariant.
