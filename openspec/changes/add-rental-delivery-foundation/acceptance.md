# Acceptance Criteria: add-rental-delivery-foundation

## Domain Criteria

- 系统能在同一租户内按稳定业务键创建一个 `rental_delivery`，重复请求返回原
  Delivery，不新增重复包裹。
- 一个租赁订单可以保存多个不同 `package_seq` 或不同方向的 Delivery；
  `OUTBOUND`、`RETURN`、`EXCHANGE_OUT`、`EXCHANGE_RETURN` 互不覆盖。
- 一个 Delivery 可以在一次事务中关联多台设备；重复关联同一设备不会新增关系。
- order item、assignment、device、delivery 的租户或订单关系不一致时，创建/
  绑定操作失败且不留下部分 Delivery、关系或 Outbox 数据。
- 缺少承运商映射或 Provider 配置时，本地 Delivery 仍可创建，并进入明确的
  `MAPPING_REQUIRED` 或禁用状态，不执行外部调用。
- `rental_device_shipment.delivery_id` 可空，旧发货数据和旧代码在未写入该字段
  时仍保持兼容。

## Tracking Criteria

- 完整轨迹事件被规范化并按业务事件时间排序，相同事件指纹在同一快照版本内唯一。
- 对同一完整快照重复调用聚合逻辑时，`current_snapshot_hash` 保持一致，
  `tracking_version` 不增加，也不产生新的有效快照版本。
- 完整轨迹或摘要发生有效变化时，系统创建下一快照版本并原子更新 Delivery
  当前快照指针、最新轨迹摘要和 `tracking_version`。
- 当前状态为 `DELIVERED` 或 `RETURNED` 时，迟到的 `IN_TRANSIT` 或
  `OUT_FOR_DELIVERY` 数据不能使状态回退。
- ETA 缺失时快照仍可成功保存和聚合，不阻塞 Delivery 状态更新。
- 客户主动退回包裹到仓库时可以表示为 `direction=RETURN` 且
  `tracking_status=DELIVERED`，不会误写成承运商异常退回状态。

## Reliability Criteria

- 相同租户、事件类型和 dedupe key 重复写 Outbox 时只保留一个有效任务。
- Outbox 可以保存 `SUBSCRIBE`、`INITIAL_QUERY`、`REFRESH_QUERY`、
  `RECONCILE`，但本 Change 中没有 Worker 消费这些任务。
- Outbox payload 和错误摘要不包含手机号、地址、完整运单号、密钥、token、
  salt 或回调原文。
- Inbox 能按 Provider、Delivery 和 payload hash 幂等识别重复回调记录，并
  保存处理状态、租约锁和重试元数据；本 Change 不暴露回调入口。
- 不同租户可以使用相同运单号、Provider 编码、payload hash 或 dedupe key，
  且数据、唯一约束和服务查询保持隔离。
- Provider 开关默认关闭，运行测试或启动普通应用不会访问快递100或其他真实
  物流供应商。

## Security Criteria

- `tracking_phone`、`callback_token`、`callback_salt`、回调参数以及 Provider
  凭据字段使用项目 `EncryptTypeHandler`，数据库中不保存对应明文。
- `customer_code`、`api_key`、`api_secret` 不进入 Provider 配置 DO 的
  `toString` 输出。
- 日志测试、异常测试和静态扫描证明完整手机号、地址、运单号、密钥、token、
  salt 和回调原文不会被输出。
- Provider、Service、DO、Mapper 和领域测试的编译依赖中不存在快递100 SDK
  类型。

## Migration Criteria

- 新迁移使用执行时未被占用的最大编号之后的编号，不修改任何历史 migration。
- 从空库执行完整 migration 链可以成功创建 7 个物流实体和
  `rental_device_shipment.delivery_id`。
- 从当前 `_031` 数据库升级可以成功，现有订单、设备、assignment、shipment
  和运单字段不丢失、不改值。
- 迁移不执行外部请求、历史回填、历史订阅、大批量状态变更或 DROP 操作。
- 新表和索引遵循租户、审计、逻辑删除及命名约定；同一 migration 不依赖真实
  Provider 配置或凭据。

## Boundary Criteria

- 本 Change 只修改 rental backend、migration、测试和 ADR；不修改
  `XianyuOrderShipService`、闲鱼 Controller、排期中心或任何前端。
- `LogisticsProvider` 的订阅、查询和已验证回调解析方法只使用平台
  Command/Result/Event 模型。
- Controller、Webhook、Job、快递100 Provider、Inbox Worker 和 Outbox
  Worker 均不存在实际实现。
- `RentalDeliveryService`、`RentalTrackingSnapshotService` 和
  `RentalDeliveryOutboxService` 的事务与幂等职责可由单元测试独立验证。
- 物流状态变化不会自动修改设备可用性、排期、回仓、验收或检测状态。

## Component Criteria

- `component-impact-map.json` 中命名的 DO、Mapper、Service、Provider 抽象、
  纯函数和安全工具按现有 rental 模块边界组织，没有 Controller-to-Mapper
  或领域层到供应商 SDK 的依赖。
- 快照规范化、hash、状态聚合和终态保护是可复用的供应商无关组件，不在后续
  查询与回调路径中各自复制。
- 没有新增前端组件、hook、主题实现、语言包或跨客户端共享包。

## Verification Surfaces

- Facticity: Foundation Specs、当前 migration 最大编号、现有 shipment/assignment
  DO、`EncryptTypeHandler` 和 rental 模块依赖树。
- Static: `git diff --check`、禁止依赖扫描、敏感信息日志扫描、Java 编译和
  migration 文件检查。
- Unit: Delivery 幂等、多设备关联、关系不匹配、Outbox dedupe、轨迹 fingerprint、
  完整快照 hash、状态排序、终态保护、加密字段和租户隔离。
- Redteam: 重复创建、跨租户 ID、伪造关联、重复 payload、重复 dedupe key、
  终态回退、敏感字段 `toString`/异常/日志泄露。
- E2E: MySQL 空库 migration、从 `_031` 升级、rental 模块持久化测试；所有
  外部 Provider 使用禁止网络或 mock 断言。
- Sensory: 不适用。本 Change 没有 UI 或原型。

## Unresolved Gaps

- 无。
