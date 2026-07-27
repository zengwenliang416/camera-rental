# Task 001 Report

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipService.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipServiceTest.java`
- `camera-rental-server/sql/mysql/migrations/20260726_023_xianyu_ship_workflow.sql`
- `openspec/changes/xian-guanjia-order-dispatch-ship-v1/development/migrations/20260726_023_xianyu_ship_workflow.sql`

## What Changed

- Backend ship idempotent replay now loads the shipment device before building the response, so repeated submissions return `deviceNo`.
- Added focused unit coverage for write-disabled no-mutation behavior, remote-success commit ordering, and idempotent replay response shape.
- Recorded the shipment workflow migration and checksum evidence under the active SpecNav change.

## TDD Evidence

- Added `XianyuOrderShipServiceTest` after identifying the idempotent replay response gap.
- The test suite validates the write switch before local mutation, the remote-call-before-local-dispatch/insert/update ordering, and replay without a second remote call.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderShipServiceTest,ShipmentOcrServiceTest,OpenAiCompatibleShipmentOcrClientTest -Dsurefire.failIfNoSpecifiedTests=false test` passed with 7 tests.
- `git diff --check` passed.

## Concerns

- Full cross-tenant red-team coverage still belongs to the verification stage.

## Scope Deviations

- None. No new XianGuanJia write endpoint was added.

## Follow-up Needed

- Run standard verification stage mock E2E and red-team probes before archive.
