# Requirements: add-rental-delivery-foundation

## Summary

在 `yudao-module-rental-biz` 中建立与物流供应商无关的租赁包裹、设备关联、
完整轨迹快照、Inbox、Outbox、承运商映射和供应商配置基础。该 Change 只建立
本地领域模型、持久化契约、Provider 抽象和纯领域逻辑，不调用快递100，不接入
现有闲鱼发货流程，也不修改排期中心。

`rental_device_shipment` 继续表示一次发货操作及闲鱼调用的审计凭证；
`rental_delivery` 表示一个真实包裹。一个租赁订单可以有多个包裹，一个包裹
可以关联多台具体设备。寄出、客户退回、换机寄出和换机退回必须保留为相互独立
的物流单，不能覆盖历史包裹。

## Users & Actors

- 租赁领域服务：创建或复用包裹、绑定设备、维护本地物流状态和可靠任务。
- 后续物流 Provider 适配器：通过供应商无关接口执行订阅、查询和回调解析。
- 后续 Inbox/Outbox Worker：读取本 Change 建立的可靠任务和回调记录；本
  Change 不实现 Worker。
- 租赁运营与排期系统：后续只读取本地物流读模型；本 Change 不新增 UI 或
  HTTP API。
- 数据库迁移与测试执行者：验证空库、升级库、租户隔离、幂等和状态聚合。

## In Scope

### Architecture Decision

- 在 `docs/decisions` 下记录统一物流领域 ADR，明确 shipment 是发货审计、
  delivery 是真实包裹、订单与包裹是一对多、包裹与设备是多对多。
- ADR 必须锁定订阅推送为主、主动查询补偿、事务内写 Outbox、事务外调用
  Provider、回调先写 Inbox 再异步处理、排期中心只读本地读模型。
- ADR 必须明确物流签收不自动恢复设备可用；回仓、验收和检测仍由原租赁
  生命周期负责。
- 第一版不使用 SSE，`estimated_arrival_time` 可空且不参与核心排期。

### Database Foundation

- 使用新的、执行时重新确认未被占用的时间戳迁移文件，创建或加法演进以下
  7 个持久化实体，并给 `rental_device_shipment` 增加可空 `delivery_id`：
  - `rental_delivery`
  - `rental_delivery_device_rel`
  - `rental_delivery_trace`
  - `rental_delivery_callback_inbox`
  - `rental_delivery_outbox`
  - `rental_logistics_carrier_mapping`
  - `rental_logistics_provider_config`
- 当前迁移最大编号是 `_031`；`20260731_032_rental_delivery_tracking.sql`
  仅为候选名称，Apply 前必须重新检查，不得覆盖并行分支或已执行迁移。
- 迁移必须是加法变更，不删除或重命名现有列，不修改现有运单值，不执行历史
  回填，不访问外部服务，不自动订阅历史运单。
- 所有新表遵循项目的 `tenant_id`、审计字段、逻辑删除和索引命名约定。
  是否使用物理外键必须遵循 rental 模块现状，不得为了本 Change 引入不一致
  的外键策略。

### Delivery Model

- `rental_delivery` 至少保存业务身份、运单、状态、订阅、查询/同步、回调安全
  和当前快照摘要字段。
- 业务身份包括 `delivery_no`、`rental_order_id`、`channel_order_id`、
  `direction`、`package_seq`、`create_source`。
- `direction` 支持 `OUTBOUND`、`RETURN`、`EXCHANGE_OUT`、
  `EXCHANGE_RETURN`。
- 运单字段包括来源承运商、规范承运商、Provider、Provider 承运商编码、
  `waybill_no`、映射状态和 `tracking_phone`。
- 映射状态至少支持 `MAPPED` 和 `MAPPING_REQUIRED`。缺少承运商映射或
  Provider 配置时仍允许创建本地 Delivery。
- `tracking_phone`、`callback_token`、`callback_salt` 使用项目
  `EncryptTypeHandler` 加密保存；`callback_token_hash` 用于公开回调定位。
- `waybill_no` 保留供应商查询所需值，但未来 API 默认脱敏，日志只能记录
  哈希或尾号。
- 平台统一轨迹状态支持 `CREATED`、`INFO_RECEIVED`、`PICKED_UP`、
  `IN_TRANSIT`、`OUT_FOR_DELIVERY`、`DELIVERED`、`EXCEPTION`、
  `RETURNING`、`RETURNED`、`CUSTOMS`、`UNKNOWN`。
- 客户主动退回的包裹到达仓库时仍使用 `DELIVERED`；`RETURNING` 和
  `RETURNED` 只表示承运商异常退回寄件人。
- 订阅状态支持 `NOT_STARTED`、`MAPPING_REQUIRED`、`PENDING`、
  `SUBSCRIBED`、`FAILED_RETRYABLE`、`FAILED_FINAL`、`ABORTED`、
  `CLOSED`、`DISABLED`，并预留按月调用计数和下次尝试时间。
- 查询/同步字段至少支持 `last_query_at`、`next_query_at`、
  `last_callback_at`、`last_provider_sync_at`。
- 当前快照摘要至少支持 `current_snapshot_version`、
  `current_snapshot_hash`、`latest_trace_id`、`latest_trace_time`、
  `latest_trace_context`、`latest_location`、`estimated_arrival_time`
  和 `tracking_version`。

### Device Relationship

- `rental_delivery_device_rel` 关联 `delivery_id`、`rental_order_item_id`、
  `assignment_id` 和 `device_id`。
- 同一个 Delivery 不能重复关联同一设备。
- order item、assignment、device 和 delivery 必须属于同一租户及兼容的
  订单分配关系；不匹配时服务拒绝写入且不留下部分数据。
- 数据模型必须支持一个包裹绑定多台设备，即使当前闲鱼发货流程通常一次只处理
  一台设备。

### Full Tracking Snapshots

- `rental_delivery_trace` 按完整轨迹快照保存事件，不把每次供应商结果当成简单
  增量无限追加。
- 每条事件保存 `snapshot_version`、`event_seq`、
  `event_fingerprint`、业务事件时间及原始时间、平台状态、供应商状态、
  文本、位置、来源和 Inbox 引用。
- 同一租户、Delivery、快照版本和事件指纹必须唯一。
- `RentalTrackingSnapshotService` 或等价纯领域聚合器执行：规范化事件、
  按业务时间排序、计算快照 hash、识别重复快照、创建新版本并生成当前摘要。
- 相同完整快照不增加 `tracking_version`；有效轨迹或摘要变化才增加版本。
- 状态聚合必须防止乱序或迟到数据把 `DELIVERED`、`RETURNED` 等终态回退。
  比较顺序使用最新业务时间、状态优先级、终态保护和接收时间。
- 轨迹快照历史可以保留；清理策略属于后续
  `harden-logistics-operations`，本 Change 不实现清理任务。

### Inbox and Outbox Foundation

- `rental_delivery_callback_inbox` 保存 Provider、Delivery、任务 ID、
  `payload_hash`、加密后的回调参数、接收/处理/重试/锁定状态和脱敏错误。
- 只有后续回调入口完成 token 定位和签名校验后才允许保存完整回调参数；
  本 Change 不实现回调 Controller 或验签流程。
- 重复 Provider/Delivery/payload hash 必须可幂等识别。
- `rental_delivery_outbox` 支持 `SUBSCRIBE`、`INITIAL_QUERY`、
  `REFRESH_QUERY`、`RECONCILE`，包含稳定 `dedupe_key`、状态、可执行时间、
  租约锁、重试次数和脱敏错误。
- Outbox payload 不保存手机号、地址、完整运单号、密钥或回调原文。后续 Worker
  根据 `delivery_id` 在执行时读取必要数据。
- `RentalDeliveryOutboxService` 对相同租户、事件类型和 dedupe key 幂等。
- 本 Change 可以写入 Outbox，但没有 Worker 消费，也不得触发任何网络调用。

### Carrier Mapping and Provider Configuration

- `rental_logistics_carrier_mapping` 保存来源类型/编码/名称、平台规范编码/名称、
  Provider 编码、Provider 承运商编码、手机号要求和启用状态。
- 手机号要求支持 `NONE`、`OPTIONAL`、`REQUIRED`。
- `rental_logistics_provider_config` 按租户和 Provider 唯一，保存启用开关、
  查询/订阅开关、加密凭据、回调基础地址、最小查询间隔、结果版本、配置状态和
  最后验证时间。
- `customer_code`、`api_key`、`api_secret` 使用项目加密 TypeHandler，
  对应 DO 字段禁止进入 `toString`。
- 全部新能力默认关闭；仅存在配置记录不能代表 Provider 已启用。

### Services, Persistence, and Provider Abstraction

- 为 7 个新实体建立 DO 和 Mapper，遵循 `TenantBaseDO`、MyBatis Plus 和
  rental 模块现有包结构。
- 建立 `RentalDeliveryService`、`RentalTrackingSnapshotService`、
  `RentalDeliveryOutboxService` 及其明确的事务边界。
- `RentalDeliveryService` 支持按稳定业务键幂等创建或获取 Delivery，并在
  同一事务中校验和绑定零到多台设备；重复调用不得创建重复 Delivery 或关系。
- 建立供应商无关的 `LogisticsProvider` 接口以及 Command/Result/Event 模型，
  覆盖订阅、查询和已验证回调解析能力。
- Provider 接口、领域 Service、DO、Mapper 和测试中不得出现快递100 SDK
  类型或供应商专用 DTO。
- 可以预留 `controller.admin.logistics`、`controller.webhook.logistics`、
  `integration.logistics`、`service.logistics`、`job.logistics` 包边界，
  但本 Change 不实现 Controller、Webhook、Job 或 Provider 适配器。

### Security and Observability

- 日志、异常、测试快照和审计信息不得出现完整运单号、手机号、地址、密钥、
  callback token、callback salt 或回调原文。
- Outbox/Inbox 错误字段只保存分类码和脱敏摘要。
- 所有读写必须受租户隔离；不同租户使用相同运单号、dedupe key 或 Provider
  编码时不得相互冲突或读取。
- CI 和普通单元测试不得访问真实物流供应商。

## Out of Scope

- 快递100 SDK 依赖、Provider 适配器、订阅调用、主动查询调用和供应商状态映射。
- 公开回调 Controller、token 路由、MD5 验签、快速 ACK 和 Inbox Worker。
- Outbox Worker、定时 Job、查询限频执行、月度订阅限制执行、供应商重试。
- 修改 `XianyuOrderShipService`、闲鱼发货事务、现有发货 API 或历史运单字段。
- 创建历史 Delivery、批量回填、自动订阅历史运单或 Reconcile 运营入口。
- 排期中心批量摘要 API、完整轨迹 API、手动刷新 API、轮询、UI 或 SSE。
- 物流风险、运营后台、指标、告警、清理任务和密钥管理 API。
- 根据物流签收自动修改设备、排期、回仓、验收或可用状态。
- commit、push、PR、部署或生产数据库执行。

## UI Design Impact

- Foundation spec: `openspec/specs/ui-design/design.md`
- 本 Change 不新增或修改生产 UI、文案、交互、主题控件或语言控件。
- 私密字段默认脱敏、状态不只依赖颜色等 UI 基线继续有效，供后续 Change 使用。

## Theme & Locale Capability Impact

- Theme support: 项目基线保持 `light-dark`。
- Theme toggle policy: 项目基线保持 `theme-toggle:user`；本 Change 不创建或
  修改 toggle。
- Internationalization: 项目基线保持 `i18n:enabled`。
- Supported locales: `zh-CN,en`。
- Default locale: `zh-CN`。
- Prototype coverage: 本 Change 无 UI、无可运行原型要求；后续 UI Change
  必须遵循 Foundation Spec 的四种主题/语言组合。

## Architecture & Database Impact

- Foundation spec: `openspec/specs/system-architecture/design.md`
- 影响仅限 `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`、
  rental 模块测试、增量 MySQL migration 和 `docs/decisions`。
- Backend 继续是物流、状态、幂等、租户隔离和敏感信息处理的权威来源。
- 本 Change 扩展 Foundation Spec 中的 `rental_delivery` 概念为真实包裹模型，
  不改变 modular monolith 边界，不增加消息中间件或独立服务。

## Frontend-Backend Data Flow Impact

- Foundation spec: `openspec/specs/frontend-backend-data-flow/design.md`
- 新增内部 `FLOW-DELIVERY-CREATE`、`FLOW-DELIVERY-SNAPSHOT-APPLY` 和
  `FLOW-DELIVERY-OUTBOX-ENQUEUE`，均只在本地服务与数据库之间运行。
- 不新增 HTTP API，不改变前端请求，不执行第三方网络调用。
- 后续外部订阅、查询和回调必须复用本 Change 的本地模型和快照聚合逻辑。

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`
- 物流领域 Service 负责事务和跨实体校验，Mapper 只负责持久化，Provider
  适配器负责供应商协议转换，禁止 Controller-to-Mapper。
- 快照规范化、hash、状态聚合和终态保护必须提取为可独立测试的供应商无关逻辑，
  供后续查询与回调共同复用。
- 本 Change 不创建前端组件、hook 或跨客户端共享包。

## Unresolved Gaps

- 无。附件中的 Change 1 架构、数据、安全、边界和验收决策已完整锁定。
