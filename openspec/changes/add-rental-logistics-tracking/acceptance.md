# Acceptance Criteria: add-rental-logistics-tracking

## User-Visible Criteria

- 运营人员通过现有发货工作台提交闲鱼发货后，能看到发货成功以及本地物流追踪
  已创建、待映射、Provider 未启用或待重试的明确结果；快递100失败不把已经
  成功的闲鱼发货显示为失败。
- 一个订单拆成多个包裹时，排期中心显示包裹总数和状态分布；点击后可以逐包裹
  查看方向、承运商、脱敏运单、状态和完整轨迹。
- 一个包裹关联多台设备时，同一物流摘要可由相关排期块引用，不产生重复包裹或
  重复轨迹。
- 单包裹摘要显示方向、平台状态、承运商、脱敏运单号和最近更新时间。
- 物流详情抽屉显示当前状态、最后同步时间、ETA（如有）、按业务时间倒序的完整
  轨迹、数据 stale 状态和刷新操作。
- 用户点击刷新后立即收到“已排队”或“受限频限制”的稳定反馈；页面不等待
  快递100网络请求，受限频时显示下一次允许时间。
- 排期页面可见时每 60 秒刷新本地摘要，隐藏时暂停，重新显示时立即刷新；该轮询
  不直接请求快递100。
- 映射缺失、Provider 关闭、无轨迹、订阅失败、物流异常、数据过旧和权限不足均
  有明确、安全且可操作的界面状态。
- 物流风险出现在排期异常中心，包含严重度、受影响订单/设备和安全下一步。
- light/dark 和 `zh-CN`/`en` 下的桌面、窄屏、抽屉、时间线、状态和错误界面
  均可使用，不出现原始翻译 key、只靠颜色表达状态或页面级横向溢出。

## System Criteria

- 闲鱼发货事务成功时原子写入 shipment、Delivery、设备关系及
  `SUBSCRIBE/INITIAL_QUERY` Outbox；任何本地写入失败时不留下部分物流数据。
- Provider 网络调用只发生在提交后的 Worker 中，不持有发货或轨迹数据库事务。
- 缺少承运商映射、Provider 配置或启用开关时，Delivery 仍可创建且状态明确，
  不产生真实 Provider 请求。
- 快递100订阅、查询和回调通过供应商无关接口进入领域层；SDK 类型只存在于
  kuaidi100 适配包。
- 回调先完成 token 定位和签名验证，再幂等写 Inbox、快速 ACK 并异步处理。
- 重复订阅任务、查询任务、回调 payload、Worker 重启和租约过期不会产生重复
  Provider 调用结果、有效快照或状态推进。
- 主动查询受到后端最小间隔限制；前端刷新和 60 秒本地轮询不能绕过限制。
- 快照历史变化会创建新版本，相同完整快照不会增加 `tracking_version`。
- 乱序或迟到数据不能把 `DELIVERED/RETURNED` 回退为非终态。
- 客户主动退回正常送达仓库表示 `direction=RETURN + DELIVERED`，不会误映射为
  承运商异常退回。
- 物流签收和风险变化不自动改变设备可用、回仓、验收、检测或排期释放状态。
- 历史回填、失败任务重试、Reconcile 和清理均受权限、租户、开关、幂等和审计
  约束。

## Data Criteria

- 新 migration 在执行时未占用的最新编号后创建 8 张物流表，给
  `rental_delivery` 增加可空 `provider_credential_id`，并给
  `rental_device_shipment` 增加可空 `delivery_id`，不修改历史 migration。
- 空库完整 migration 和当前数据库升级均成功，现有订单、设备、assignment、
  shipment 和运单值不丢失、不改写。
- 一个订单可保存多个 Delivery，一个 Delivery 可关联多台设备，同一关系不会
  重复。
- Delivery、关系、trace、Inbox、Outbox、mapping、Provider config 和 Provider
  credential 全部按租户隔离；不同租户可使用相同运单、hash、dedupe key、
  Provider 编码和凭据名称。
- `tracking_phone`、callback token/salt、回调参数、Provider 回调密钥和每组
  Provider 凭据使用项目加密 TypeHandler；对应 DO 的敏感字段不进入 `toString`。
- 同一租户可配置多组完整启用的快递100凭据。未绑定 Delivery 按稳定规则从有序
  凭据池中选择一组，已绑定 Delivery 持续复用该凭据；停用、删除、不完整或跨租户
  凭据不可使用并触发安全重选。
- Outbox payload、日志、错误、指标、测试数据和前端响应不包含完整手机号、
  地址、运单、密钥、token、salt 或回调原文。
- 完整轨迹保留 snapshot version、事件顺序、fingerprint、业务/原始时间、
  平台/供应商状态、文本、位置、来源和 Inbox 引用。
- 历史回填不在结构 migration 中执行，也不自动订阅或查询真实 Provider。

## Component Criteria

- Reusable components, hooks, utilities, or services named in
  `component-impact-map.json` are extracted instead of duplicated.
- `RentalDeliveryService`、`RentalTrackingSnapshotService`、
  `RentalDeliveryOutboxService`、Inbox/Outbox Worker、Provider 适配器和风险
  服务职责分离，Controller 不直接访问 Mapper。
- 前端页面复用 `StatusBadge`、`DetailDrawerShell`、`EmptyState`、
  `PermissionAwareAction` 和当前主题/语言偏好，不建立第二套设计系统。
- `trackingByOrderId` 是排期中心物流摘要的唯一共享读模型；完整轨迹按需加载。
- 页面和前端模型不实现 Provider 状态映射、查询限频、风险计算或设备生命周期
  转换。

## Verification Surfaces

- Facticity: 快递100官方 SDK/订阅/查询/回调契约、当前闲鱼发货实现、迁移编号、
  排期中心类型和现有权限。
- Static: `git diff --check`、Maven 编译、TypeScript 类型检查、生产构建、
  禁止 SDK 跨层依赖、敏感信息扫描和 migration 检查。
- Unit: Delivery 幂等、多设备关系、映射、Provider 状态转换、签名、快照 hash、
  终态保护、Outbox/Inbox dedupe、限频、重试、风险和前端展示模型。
- Redteam: 跨租户 IDOR、伪造 callback token/sign、重复 payload、租约抢占、
  限频绕过、敏感字段泄露、原始错误展示和自动设备释放尝试。
- E2E: MySQL migration、mock 快递100订阅/查询/回调、闲鱼发货到 Delivery、
  排期批量摘要、详情、刷新、风险和历史回填闭环。
- Sensory: 现有排期中心壳层下的 light/dark、`zh-CN`/`en`、桌面/窄屏、
  loading/empty/error/permission/multi-package/exception/delivered 矩阵。

## Unresolved Gaps

- 无。
