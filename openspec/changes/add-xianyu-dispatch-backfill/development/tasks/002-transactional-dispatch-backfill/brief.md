# Task Brief: 002-transactional-dispatch-backfill

## Goal

Make an eligible shipped channel order locally consistent by binding one
physical device, occupied schedule, Shipment, Delivery, and channel logistics
inside one tenant-safe transaction.

## Parent Artifacts

- `openspec/changes/add-xianyu-dispatch-backfill/requirements.md`
- `openspec/changes/add-xianyu-dispatch-backfill/acceptance.md`
- `openspec/changes/add-xianyu-dispatch-backfill/prototype/handoff.md`

## Vertical Slice

`POST /admin-api/rental/xianyu/order/dispatch-backfill` accepts keyboard-entered
device and logistics facts, rejects invalid or conflicting requests before
mutation, and returns the committed local shipment result without remote writes.

## In Scope

- Checklist items `2.1` through `2.5`.
- Request validation, permission, tenant/order/device eligibility, optional
  conversion, assignment, schedule, dispatch, Shipment, Delivery, logistics
  update, idempotency, typed errors, and rollback.

## Out Of Scope

- XianGuanJia runtime write configuration and `XianyuWriteClient`.
- Database migrations, new tables, new permissions, multiple devices per
  waybill, or explicit multi-item selection.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/XianyuOrderController.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/vo/XianyuOrderDispatchBackfillReqVO.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/rental/RentalDeviceShipmentMapper.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipService.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipServiceTest.java`

## Components To Create

- `XianyuOrderDispatchBackfillReqVO`.
- The local-only controller endpoint and `XianyuOrderShipService.backfillDispatch`
  aggregate operation.

## Components To Reuse

- Existing permission enforcement, tenant context, rental conversion, assignment,
  occupied scheduling, local dispatch, Shipment, and Delivery services.
- Existing `XianyuOrderShipRespVO`.

## Components To Extract

- Keep the aggregate transaction in `XianyuOrderShipService`; do not duplicate
  assignment, schedule, dispatch, or Delivery rules in the controller.

## TDD Requirement

- Add focused service tests for every eligibility, idempotency, conflict,
  rollback, and no-remote-write contract.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderShipServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `git diff --check -- camera-rental-server/yudao-module-rental`

## Stop Conditions

- Scope lock mismatch.
- A remote XianGuanJia call or write-enabled configuration dependency appears.
- The existing Shipment business key cannot safely represent the approved
  one-device-per-waybill limitation.
- The transaction cannot roll back all local mutations on Delivery failure.

## Unsafe Assumptions

- Existing local service contracts must be verified from current code and tests;
  method names alone are not proof of transactional behavior.
- A converted channel order may be absent and must be created through the
  existing conversion boundary rather than ad hoc persistence.
