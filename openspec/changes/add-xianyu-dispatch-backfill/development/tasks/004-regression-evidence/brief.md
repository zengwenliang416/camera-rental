# Task Brief: 004-regression-evidence

## Goal

Provide repeatable evidence that the local correction is tenant-safe,
idempotent, rollback-safe, remote-write-free, and type/format clean.

## Parent Artifacts

- `openspec/changes/add-xianyu-dispatch-backfill/requirements.md`
- `openspec/changes/add-xianyu-dispatch-backfill/acceptance.md`
- `openspec/changes/add-xianyu-dispatch-backfill/prototype/handoff.md`

## Vertical Slice

The focused backend suite covers success and all approved negative paths, while
the admin checks prove the typed dialog integration builds and conforms to the
repository format rules.

## In Scope

- Checklist items `4.1` through `4.4`.
- Missing refunded/closed, cross-tenant, same-waybill/different-device,
  unmapped-order conversion, Delivery rollback, and no-remote-write tests.
- Exact Maven, Vue type, ESLint, Prettier, and diff-check evidence.

## Out Of Scope

- Real XianGuanJia calls, production tenant data, browser camera, broad
  repository formatting, or unrelated test repair.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipServiceTest.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-api/src/main/java/cn/iocoder/yudao/module/rental/enums/ErrorCodeConstants.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/XianyuOrderController.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/xianyu/vo/XianyuOrderDispatchBackfillReqVO.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/dal/mysql/rental/RentalDeviceShipmentMapper.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/admin/XianyuOrderShipService.java`
- `camera-rental-admin/src/api/rental/xianyu.ts`
- `camera-rental-admin/src/views/rental/order/index.vue`
- `camera-rental-admin/src/views/rental/order/components/XianyuDispatchBackfillDialog.vue`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/locales/en.ts`
- `openspec/changes/add-xianyu-dispatch-backfill/development`
- `openspec/changes/add-xianyu-dispatch-backfill/tasks.md`

## Components To Create

- Focused regression cases and file-backed validation evidence.

## Components To Reuse

- Existing `XianyuOrderShipServiceTest` fixtures, mock boundaries, Maven module
  test setup, and admin repository scripts.

## Components To Extract

- Reuse focused test setup helpers when repeated state construction obscures
  the behavior under test; do not create a new production abstraction for tests.

## TDD Requirement

- Add the missing negative/rollback tests before accepting implementation as
  complete, then rerun the full focused class.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderShipServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-admin && pnpm exec eslint src/api/rental/xianyu.ts src/views/rental/order/index.vue src/views/rental/order/components/XianyuDispatchBackfillDialog.vue src/locales/zh-CN.ts src/locales/en.ts`
- `cd camera-rental-admin && pnpm exec prettier --check src/api/rental/xianyu.ts src/views/rental/order/index.vue src/views/rental/order/components/XianyuDispatchBackfillDialog.vue src/locales/zh-CN.ts src/locales/en.ts`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- A test requires real external credentials, a production shop, or external
  mutation.
- A failure is unrelated to allowed files and cannot be repaired without scope
  expansion.

## Unsafe Assumptions

- A previously reported test count is not current evidence; only commands
  executed after the final edits count.
- A passing controller test alone does not prove transaction rollback or
  absence of `XianyuWriteClient` calls.
