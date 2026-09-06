# Tasks: add-offline-manual-order

## 1. Database

- [x] 1.1 新迁移 `camera-rental-server/sql/mysql/migrations/20260906_058_rental_offline_manual_order.sql`
  （写前 `ls` 确认最大编号）：`rental_order` 加 `customer_id`/`deposit_amount`；
  新表 `rental_customer`、`rental_order_delivery`（敏感列加密宽）；菜单"线下录单"
  （type=2，挂目录 7000）+ 按钮 `rental:order:create`（type=3）+ 角色授权，幂等写法。
- [x] 1.2 登记迁移到 `ops/github-deploy/migrations.txt` 末尾。

## 2. Backend — 基础

- [x] 2.1 `RentalOrderDO` 加 `customerId`、`depositAmount`。
- [x] 2.2 新 DO/Mapper：`RentalCustomerDO`、`RentalOrderDeliveryDO`（敏感字段
  `@TableField(typeHandler = EncryptTypeHandler.class)`）。
- [x] 2.3 新枚举 `RentalDeliveryMethodEnum { EXPRESS, ERRAND, SELF_DELIVERY }`。
- [x] 2.4 `ErrorCodeConstants` 新增 1-040-005 段错误码。

## 3. Backend — 服务与接口

- [x] 3.1 `RentalManualOrderService.createManualOrder`：校验（型号/租期/金额/配送必填项）、
  客户反查复用或新建、日期半开换算、插订单（OFFLINE/PENDING_ALLOCATION/READY）、
  回填 orderNo、插明细、插配送，整体事务。
- [x] 3.2 `RentalOrderManualController`：`POST /rental/order/create-manual`、
  `POST /rental/order/confirm-outbound`、`GET /rental/customer/suggest`；
  VO 放 `controller/admin/rental/vo/`。
- [x] 3.3 核实对账 job / `RentalFulfillmentUpdateGuard` 不回退 OFFLINE 订单
  preparationStatus；若会，加 `source_type='XIANYU'` 过滤。
- [x] 3.4 分配写路径挂钩：`assign` / `assignPendingPlan` 新建分配落库后查
  `rental_order_delivery`，ERRAND/SELF_DELIVERY 即复用 `RentalDeviceOpsService.dispatch()`
  同事务出库（assignment→DISPATCHED、设备→RENTED）；EXPRESS 与无配送记录的渠道订单
  保持 assign-only；`confirm-outbound` 降为兜底且幂等。

## 4. Backend — 既有链路适配

- [x] 4.1 `RentalReportMapper.xml` 移除硬编码 `'XIANYU' AS source_type`，改用实际列。
- [x] 4.2 验证待分配列表 `channelOrderId=null` 展示兜底、`POST /rental/order/cancel`
  对 OFFLINE 订单可用（状态机渠道中立则不改代码，仅验证）。

## 5. Frontend — camera-rental-admin

- [x] 5.1 `src/api/rental/orderCreate.ts`：明细提交 `deviceIds`。
- [x] 5.2 `src/views/rental/order-create/index.vue`：先填租期、按编号/序列号选择具体
  设备、自动带出型号和数量、金额（元→分）、配送区块；提交同时要求录单和设备分配
  权限，成功定位到设备排期页。
- [ ] 5.3 待分配列表外部单号空时兜底显示 orderNo（按 4.2 验证结果决定）。

## 6. Tests

- [x] 6.1 `RentalManualOrderServiceTest`：正常创建（status/preparationStatus/日期换算/
  金额分/客户复用与新建/delivery 落库）、校验失败分支、orderNo 回填。
- [x] 6.2 Controller 安全测试（反射注解）。
- [x] 6.3 迁移文本断言测试（新表/新列/菜单幂等）。
- [x] 6.4 分配即送出用例：ERRAND/SELF_DELIVERY 分配后 DISPATCHED+RENTED、EXPRESS 与
  无配送记录不 dispatch、dispatch 失败分配回滚、`assignPendingPlan` 覆盖。
- [x] 6.5 前端 `pnpm ts:check`；补充具体设备选择与绑定服务测试。

## 7. Docs & Verify

- [x] 7.1 `docs/domain/rental-order.md`、`docs/domain/device-scheduling.md` 同步。
- [x] 7.2 `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test` 通过。
- [x] 7.3 最终 diff review：无敏感明文、无历史迁移改动、既有逻辑改动单独列出。
