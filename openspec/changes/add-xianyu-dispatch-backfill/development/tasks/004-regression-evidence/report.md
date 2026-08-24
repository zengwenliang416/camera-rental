# Task Report: 004-regression-evidence

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipServiceTest.java`
- `openspec/changes/add-xianyu-dispatch-backfill/tasks.md`
- `openspec/changes/add-xianyu-dispatch-backfill/development/validation-log.jsonl`
- `openspec/changes/add-xianyu-dispatch-backfill/development/drift-check.jsonl`
- `openspec/changes/add-xianyu-dispatch-backfill/development/tasks/*/context.json`

## What Changed

- Added the missing refunded/closed/cancelled, cross-tenant, same-waybill/different-device, unmapped conversion, and Delivery-failure test cases.
- Re-ran the full focused backend class against the final implementation.
- Added task assertion mappings and replayable validation commands for managed SpecNav receipts after the authorized production Git baseline is created.

## TDD Evidence

- The focused backend class now has 35 passing tests and directly verifies zero `XianyuWriteClient` and zero write-configuration reads on the backfill paths.
- The Delivery failure test confirms failure propagation, no later local update calls, and `rollbackFor = Exception.class`; database rollback remains a Verification 2.0 runtime oracle.

## Verification Commands

- Backend focused Maven test: passed, 35 tests, 0 failures, 0 errors, 0 skipped.
- Admin Vue TypeScript, ESLint, and Prettier had earlier interactive passes, but final-baseline managed reruns remain pending because the local admin dependency tree is currently unavailable.
- `git diff --check`: passed.

## Concerns

- The backend and diff results are current but not yet signed managed receipts because the authorized production Git baseline has not been created.
- The project Runtime is selected and installed; admin dependency recovery remains a separate local toolchain blocker.

## Scope Deviations

- The slice stayed within the approved focused test and SpecNav evidence paths and did not repair unrelated repository failures.

## Follow-up Needed

- Create the authorized local production commit, recover the admin toolchain without changing the lockfile, and replay the declared commands through the SpecNav evidence runner.
