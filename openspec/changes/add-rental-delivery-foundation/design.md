## Context

`rental_device_shipment` 当前记录一次闲鱼发货写操作，包含单台设备、assignment、
运单、幂等键和渠道响应。它是发货审计凭证，不适合作为一单多包裹、一包多设备
和完整物流轨迹的聚合根。

本 Change 只在 `yudao-module-rental-biz` 建立本地物流领域基础。后续
`integrate-kuaidi100-tracking` 会实现供应商适配器和 Worker，
`connect-xianyu-shipment-delivery` 会接入现有发货事务，
`add-schedule-logistics-preview` 会提供本地读 API 和 UI。

约束如下：

- Java 17、Spring Boot 3.5、MyBatis Plus、MySQL、`TenantBaseDO`。
- 使用项目现有 `EncryptTypeHandler`，加密字段的 `@TableName` 必须启用
  `autoResultMap=true`。
- 增量 migration 当前最大编号为 `_031`，Apply 时必须重新确认。
- 不引入快递100 SDK，不增加 HTTP API，不执行真实网络请求。
- 工作区已有 SpecNav 修改必须保留。

## Goals / Non-Goals

**Goals:**

- 用 `rental_delivery` 表示真实包裹，并支持一单多包裹和四种物流方向。
- 用关系表支持一个包裹关联多台具体设备。
- 建立完整轨迹快照、事件指纹、快照 hash、版本推进和终态保护。
- 建立可恢复的 Inbox/Outbox 持久化结构和幂等服务边界。
- 建立承运商映射、租户级 Provider 配置、平台枚举和供应商无关接口。
- 通过 ADR、migration、Java 单元测试和静态检查锁定边界。

**Non-Goals:**

- 不实现快递100 Provider、签名、订阅、查询、回调 Controller 或 Worker。
- 不修改 `XianyuOrderShipService` 或任何现有 HTTP API。
- 不做历史回填、运营管理、排期中心 UI、SSE 或物流风险。
- 不根据物流签收修改设备可用状态。

## Decisions

### 1. Shipment 与 Delivery 分离

`rental_device_shipment` 保持渠道发货审计语义，仅新增可空 `delivery_id`。
`rental_delivery` 是物流聚合根，保存包裹身份、承运商映射、当前状态、订阅/
查询元数据、回调安全字段和当前快照摘要。

**Alternative considered:** 继续向 shipment 增加状态和轨迹字段。

**Rejected because:** shipment 当前是一台设备的一次渠道写操作，无法稳定表达
多设备同箱、退回和换机的独立包裹，也会把渠道审计与物流生命周期耦合。

### 2. 一个能力、七个新表

新增：

1. `rental_delivery`
2. `rental_delivery_device_rel`
3. `rental_delivery_trace`
4. `rental_delivery_callback_inbox`
5. `rental_delivery_outbox`
6. `rental_logistics_carrier_mapping`
7. `rental_logistics_provider_config`

所有表使用 `tenant_id`、项目审计字段和 `deleted`。不引入物理外键，保持当前
rental migration 的约定；一致性由 Service 事务校验、唯一索引和查询索引保证。

### 3. Delivery 业务键与包裹编号

幂等业务键为：

```text
tenant_id
+ rental_order_id
+ direction
+ source_carrier_code
+ normalized waybill_no
```

运单号在服务入口去除空白并转大写后保存。`delivery_no` 使用 `DLV` 加雪花 ID
生成，并按租户唯一。`package_seq` 是同一订单同一方向的展示顺序，不作为
幂等键。

**Trade-off:** 同一订单同一方向不能用同一承运商和运单号创建两个 Delivery。
这符合真实包裹语义；一个真实运单只对应一个包裹聚合。

### 4. 设备关系必须由事务校验

`RentalDeliveryService` 在一个事务中：

1. 按业务键锁定或查找已有 Delivery。
2. 锁定/读取 order、order item、assignment 和 device。
3. 校验同租户可见性以及
   `assignment.rentalOrderId/orderItemId/deviceId` 与命令一致。
4. 创建 Delivery。
5. 幂等补齐缺失的设备关系。

重复调用可以修复缺失关系，但不能创建重复 Delivery 或重复 relation。允许
创建暂未绑定设备的 Delivery，以支持后续人工录入和历史回填 Change。

### 5. 映射缺失不阻塞本地包裹

创建 Delivery 时按 `source_type + source_carrier_code` 查询启用的承运商映射。
找到映射则写入规范编码和 Provider 编码，状态为 `MAPPED`；否则状态和订阅状态
为 `MAPPING_REQUIRED`。Provider 配置是否存在不影响本地创建。

### 6. 完整轨迹快照而非增量追加

`RentalTrackingSnapshotAggregator` 是供应商无关纯函数：

1. 规范化文本、状态和时间。
2. 按 `event_time`、原始时间和 fingerprint 稳定排序。
3. 为每个事件计算 SHA-256 fingerprint。
4. 对有序 fingerprint 序列计算 snapshot hash。
5. 选取最新摘要并应用终态和迟到保护。

`RentalTrackingSnapshotService` 锁定 Delivery 后比较
`current_snapshot_hash`：

- hash 相同：不插入新轨迹、不增加 `tracking_version`，只更新供应商同步时间。
- hash 不同：创建下一 `snapshot_version`，批量写入事件，原子更新当前快照
  指针、摘要和 `tracking_version + 1`。

供应商修正历史轨迹会形成新快照；旧快照保留，清理由后续 Change 处理。

### 7. 状态优先级与终态保护

平台状态只使用 `RentalTrackingStatusEnum`。摘要选择先比较业务事件时间，再比较
状态优先级。当前为 `DELIVERED` 或 `RETURNED` 时，迟到的非终态事件不能覆盖
当前摘要；较早业务时间的事件也不能回退当前摘要。

客户主动退回到仓库仍表示 `direction=RETURN + tracking_status=DELIVERED`。
`RETURNING/RETURNED` 仅表示承运商异常退回寄件人。

### 8. Outbox 不携带敏感业务数据

`RentalDeliveryOutboxService` 只接收 `delivery_id`、事件类型、dedupe key 和
可执行时间。第一阶段不接受任意 payload；数据库保留可空 payload 字段供后续
经过安全审查的元数据使用。Worker 后续通过 `delivery_id` 读取必要数据。

唯一键为 `tenant_id + event_type + dedupe_key`。状态初始为 `PENDING`。

### 9. Inbox 只提供持久化模型

Inbox 保存 Provider、Delivery、任务 ID、payload hash、加密 callback 参数、
处理状态、租约锁和重试元数据。公开 token 路由和验签属于 Change 2，本 Change
不提供写入入口；重复键为 `tenant_id + provider_code + delivery_id +
payload_hash`。

### 10. 敏感字段加密和日志隔离

以下字段使用 `EncryptTypeHandler` 并加 `@ToString.Exclude`：

- `rental_delivery.tracking_phone`
- `rental_delivery.callback_token`
- `rental_delivery.callback_salt`
- `rental_delivery_callback_inbox.callback_param`
- Provider 配置的 `customer_code`、`api_key`、`api_secret`

运单号为供应商查询所需值，不加密，但日志和后续 API 必须脱敏。本 Change 的
异常对象不包含手机号、地址、完整运单号、密钥、token、salt 或回调原文。

### 11. Provider 契约隔离供应商 SDK

`integration.logistics.LogisticsProvider` 只暴露平台
Command/Result/Event：

```java
String getProviderCode();
LogisticsSubscribeResult subscribe(LogisticsSubscribeCommand command);
LogisticsTrackingResult query(LogisticsTrackingQuery query);
LogisticsCallbackResult parseVerifiedCallback(LogisticsCallbackCommand command);
```

快递100 SDK 类型只能在后续 Provider 适配器内部出现。普通单测和 CI 不访问
真实供应商。

### 12. 包结构

```text
cn.iocoder.yudao.module.rental
├── dal.dataobject.logistics
├── dal.mysql.logistics
├── enums.logistics
├── service.logistics
└── integration.logistics
    └── model
```

不创建空 Controller、Webhook 或 Job 类；后续 Change 在需要时创建实际实现。

## Risks / Trade-offs

- [迁移字段较多] → 使用单个加法 migration、明确索引和空库/升级库检查，不做
  历史回填或 DROP 回滚。
- [应用层校验替代物理外键] → 使用事务锁定、租户过滤、唯一索引和关系单测，
  保持仓库现有迁移风格。
- [保留完整历史快照会增长数据量] → Change 1 只保证正确性；保留期限和清理
  策略在 `harden-logistics-operations` 中实现。
- [纯状态优先级不能覆盖所有供应商语义] → 平台只定义稳定通用状态，供应商
  状态映射留给 Change 2，未知状态归一化为 `UNKNOWN`。
- [加密 TypeHandler 依赖运行时密钥] → 单元测试以注解/契约为主，集成环境必须
  提供项目既有 `mybatis-plus.encryptor.password`。
- [当前 Change 创建 Outbox 但无 Worker] → 所有功能开关默认关闭，后续
  Change 2 才消费任务。

## Migration Plan

1. Apply 前重新扫描 `camera-rental-server/sql/mysql/migrations` 最大编号。
2. 新建候选 `20260731_032_rental_delivery_tracking.sql`；若 `_032` 已占用则
   顺延编号。
3. 创建 7 张新表，再为 `rental_device_shipment` 加可空 `delivery_id` 和索引。
4. 部署 Change 1 代码；没有 Worker、Controller 或外部网络行为。
5. 验证旧闲鱼发货、设备分配和排期流程未改变。

回滚优先回滚应用版本并保留新表/新列。结构迁移不提供紧急 DROP 脚本；如需
物理删除，必须在确认没有后续数据后单独审批。

## Open Questions

- 无。Change 1 的领域边界、数据模型、加密、幂等、迁移和后续 Change 分界
  已在 Requirements 和引用会话中确定。
