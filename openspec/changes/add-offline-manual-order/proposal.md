# add-offline-manual-order

## Why

运营收到线下微信老客户租车需求（如"租一台 Pocket、跑腿送货"）时无法录入系统：
`rental_order` 唯一写入路径是闲鱼渠道订单转换
（`RentalChannelOrderReconciliationService.createRentalOrder`），管理端没有手动
创建订单的入口，导致线下业务完全在系统外流转，设备占用、租金、归还均无据可查。

设备分配、待分配列表、排期工作台经调研确认是渠道中立的——缺的只是"非闲鱼订单
的产生入口"与"客户/配送信息的落点"。

## What Changes

- 新增订单来源 `source_type = 'OFFLINE'` 与管理端手动录单 API
  `POST /admin-api/rental/order/create-manual`（权限 `rental:order:create`）。
- 新增客户主档表 `rental_customer`（姓名、加密手机号、微信号、备注），录单时按
  手机号反查复用老客户；`rental_order` 增加 `customer_id`、`deposit_amount` 两列。
- 新增订单配送表 `rental_order_delivery`（1:0..1）：配送方式
  EXPRESS/ERRAND/SELF_DELIVERY、加密收货人信息、跑腿备注。
- 新增出库确认 `POST /admin-api/rental/order/confirm-outbound`，覆盖跑腿/自提的
  无运单送出场景；快递线下单本期只提示线下发货，不做运单 OCR。
- 新增客户反查 `GET /admin-api/rental/customer/suggest?mobile=` 支持录单页自动带出。
- 管理端新增"线下录单"页面（菜单+按钮权限迁移）。
- 修正 `RentalReportMapper.xml` 硬编码 `'XIANYU'` 来源为实际 `source_type` 列。
- 本 Change 不修改闲鱼转换链路、设备分配、排期、归还逻辑；不扩展归还登记以支持
  非闲鱼订单（列入后续）；不做 App 端自助下单。

## Capabilities

### New Capabilities

- `rental-offline-manual-order`: 线下订单手动录入、客户主档、订单配送信息、
  无运单出库确认、录单页面与权限。

### Modified Capabilities

- 租赁报表：订单来源统计改用 `rental_order.source_type` 实际值而非硬编码。

## Impact

- Backend: `yudao-module-rental-api`（新错误码段 1-040-005）、
  `yudao-module-rental-biz`（新 DO/Mapper/枚举/Service/Controller/VO）。
- Database: 一个加法迁移（2 张新表 + `rental_order` 2 个新列 + 菜单/权限 SQL），
  登记进 `ops/github-deploy/migrations.txt`。
- Frontend: `camera-rental-admin` 新增 `src/api/rental/orderCreate.ts` 与
  `src/views/rental/order-create/index.vue`。
- Documentation: `docs/domain/rental-order.md`、`docs/domain/device-scheduling.md`
  同步 OFFLINE 来源语义。
- Security: 客户手机号/地址按既有 EncryptTypeHandler 惯例加密存储，展示脱敏。
- Out of scope: 归还登记挂非闲鱼订单、闲鱼订单回填 customer_id、线下快递运单
  OCR/物流跟踪、App 自助下单、线下订单列表专页。
