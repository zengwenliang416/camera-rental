## Why

部分闲鱼订单已经在闲鱼侧完成发货，但平台内尚未关联实际寄出的设备，也没有同步形成设备分配、占用排期和本地出库物流证据。店铺管理员需要在没有物理扫码设备的情况下，通过 Web 管理后台安全补齐这些本地记录，同时避免再次调用闲管家发货接口造成重复发货。

## What Changes

- 在闲鱼订单列表中，仅为状态 `21`（已发货）和 `22`（交易成功）的未取消订单提供“补录出库设备”操作。
- 新增管理端接口 `POST /admin-api/rental/xianyu/order/dispatch-backfill`，接收实际设备、既有运单、承运商、实际发货时间和补录原因。
- 后端在一个本地事务中创建或复用租赁订单转换、设备分配、占用排期、出库状态、设备发货记录和 outbound Delivery。
- 补录操作写入 `source=ADMIN_BACKFILL`，支持请求幂等重放与业务键冲突保护。
- 复用 `rental:xianyu:ship` 权限，但不读取闲管家写开关、不要求店铺当前授权有效，也不调用 `XianyuWriteClient`。
- 管理端提供中英文表单、校验、警示文案和成功反馈，不依赖扫码枪、摄像头或移动端扫码能力。
- 不新增数据库迁移或运行时配置，复用现有租赁订单、设备、排期、发货和物流表。

## Capabilities

### New Capabilities

- `xianyu-dispatch-backfill`: 已在外部发货的闲鱼订单，通过 Web 管理后台补录实际设备出库及本地物流证据。

### Modified Capabilities

- 无。

## Impact

- 后端：`camera-rental-server/yudao-module-rental` 的闲鱼订单 Controller、请求 VO、出库协调 Service、Shipment Mapper、错误码和单元测试。
- 管理端：`camera-rental-admin` 的闲鱼订单 API、订单列表、补录弹窗及 `zh-CN`/`en` 文案。
- API：新增一个管理端本地写接口，继续使用现有统一返回结构和 `rental:xianyu:ship` 权限。
- 数据：复用现有 `rental_device_shipment`、`rental_device_assignment`、`rental_schedule`、`rental_delivery`、`rental_order` 和 `xianyu_order` 结构，无新增迁移。
- 外部系统：不得产生任何闲管家写请求。
