## Why

现有 `rental_device_shipment` 只适合记录一次闲鱼发货写操作和设备出库审计，
无法可靠表达一单多包裹、一包多设备、寄出/退回/换机独立物流以及完整轨迹
快照。先建立供应商无关的本地物流领域基础，可以避免后续快递100 SDK、回调、
闲鱼发货和排期中心各自定义不兼容的数据与事务边界。

## What Changes

- 新增以真实包裹为聚合根的 `rental_delivery`，支持一个订单多个包裹以及
  `OUTBOUND`、`RETURN`、`EXCHANGE_OUT`、`EXCHANGE_RETURN`。
- 新增 Delivery 与订单明细、assignment、具体设备的关联模型，支持一包多设备。
- 新增完整物流轨迹快照、事件指纹、快照 hash、版本推进和终态防回退模型。
- 新增 Callback Inbox、Delivery Outbox、承运商映射和租户级 Provider 配置
  的本地持久化基础。
- 为 `rental_device_shipment` 增加可空 `delivery_id`，保留 shipment 作为
  渠道发货审计凭证。
- 新增平台统一物流枚举、Delivery/快照/Outbox 领域服务和供应商无关
  `LogisticsProvider` Command/Result/Event 契约。
- 新增物流架构 ADR、增量 MySQL migration、数据约束和领域单元测试。
- 本 Change 不调用快递100，不实现 Controller、Webhook、Job 或 Worker，
  不修改闲鱼发货 Service 和排期中心。

## Capabilities

### New Capabilities

- `rental-delivery-foundation`: 租赁包裹聚合、设备关系、完整轨迹快照、
  Inbox/Outbox、承运商映射、Provider 配置、平台状态和供应商无关领域接口。

### Modified Capabilities

- 无。

## Impact

- Backend:
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz` 中新增
  logistics DO、Mapper、枚举、领域模型、Service 和 Provider 抽象。
- Database: 新增 7 张租赁物流表，并为 `rental_device_shipment` 增加可空
  `delivery_id`；仅使用新的加法 migration。
- Documentation: `docs/decisions` 下新增物流领域 ADR。
- Tests: 增加 Delivery 幂等、多设备关系、快照去重、终态保护、Outbox
  dedupe、加密字段和租户隔离测试。
- APIs/UI: 不新增 HTTP API，不修改任何前端。
- Dependencies: 不引入快递100 SDK 或其他新供应商依赖。
