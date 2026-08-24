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

- Managed backend focused Maven test: passed, 35 tests, 0 failures, 0 errors, 0 skipped.
- Managed Admin Vue TypeScript, targeted ESLint, and targeted Prettier checks: passed.
- Managed `git diff --check`: passed.
- All 13 development commands were system-executed against Git HEAD
  `1ac3c96ecedaaff8671694d7ff8681c7c6e9911e` with signed receipts and
  `fallback_used=false`.

## Concerns

- Mockito and transaction metadata do not prove persisted database rollback or
  concurrent allocation behavior; those remain deterministic Verification 2.0
  runtime oracles.

## Scope Deviations

- The slice stayed within the approved focused test and SpecNav evidence paths and did not repair unrelated repository failures.

## Follow-up Needed

- Preserve the signed development evidence as input to the immutable
  Verification 2.0 case snapshot.
