# Task Report: 002-transactional-dispatch-backfill

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/XianyuOrderController.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/vo/XianyuOrderDispatchBackfillReqVO.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/rental/RentalDeviceShipmentMapper.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipService.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipServiceTest.java`

## What Changed

- Added `POST /admin-api/rental/xianyu/order/dispatch-backfill` under `rental:xianyu:ship`.
- Added typed request validation, shipped-order eligibility, tenant-shop validation, device resolution, conversion, assignment, occupied schedule, dispatch, `ADMIN_BACKFILL` Shipment, outbound Delivery, and channel logistics updates.
- Added normalized idempotent replay, request-hash validation, business-key conflict handling, typed failures, and `rollbackFor = Exception.class`.
- Kept the local correction path independent from Xianyu write-enabled configuration and `XianyuWriteClient`.

## TDD Evidence

- `XianyuOrderShipServiceTest` covers local success, pending/refunded/closed/cancelled rejection, cross-tenant rejection, same-waybill/different-device conflict, conversion, idempotent replay/conflict, existing dispatched assignment reuse, Delivery failure propagation, and zero remote writes.

## Verification Commands

- `/Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -o -Dmaven.repo.local=/Volumes/zwl/maven-repository -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderShipServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` passed with 35 tests, 0 failures, 0 errors, and 0 skipped.

## Concerns

- The unit test proves Delivery failure propagation and the transaction annotation; a database-backed rollback oracle remains assigned to Verification 2.0.
- The accepted first-item and one-device-per-waybill limitations remain in force.

## Scope Deviations

- The slice stayed within the approved rental module files; no Xianyu write endpoint, migration, configuration, dependency, or permission was added.

## Follow-up Needed

- Run six-domain red-team and runtime verification after immutable case approval.
