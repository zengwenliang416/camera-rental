# Requirements: add-rental-logistics-tracking

## Summary

为 Camera Rental 建立从闲鱼发货到排期中心展示的完整物流追踪闭环。运营人员在
现有发货工作台确认运单、承运商、订单和具体设备后，系统创建真实包裹
`rental_delivery`，通过事务内 Outbox 在提交后异步订阅并查询快递100，
接收并验签快递100回调，将完整物流轨迹保存为本地快照和摘要，最终在排期中心
展示单包裹或多包裹的最新状态、完整轨迹、刷新结果和物流风险。

完整功能按五个顺序垂直切片交付，但只有五个切片全部通过验收才算完成：

1. 物流领域、数据库、Inbox/Outbox 和 Provider 抽象。
2. 快递100订阅、查询、回调、状态映射、Worker、限频和重试。
3. 闲鱼发货事务创建 Delivery、设备关系和异步任务。
4. 排期中心批量摘要、完整轨迹、手动刷新和物流预览 UI。
5. 历史回填、承运商映射、Provider 配置、失败任务、风险、监控和清理。

`rental_device_shipment` 继续表示一次闲鱼发货操作和设备出库审计；
`rental_delivery` 表示一个真实物流包裹。一个订单可以有多个包裹，一个包裹
可以关联多台设备。寄出、客户退回、换机寄出和换机退回必须创建独立 Delivery，
不得覆盖原运单。

## Users & Actors

- 发货运营人员：在现有发货工作台录入或确认运单、承运商、待发货订单和设备，
  提交闲鱼发货并看到物流追踪已创建或明确降级原因。
- 排期运营人员：在排期中心查看当前窗口订单的物流摘要、多包裹状态、完整轨迹、
  最近同步时间、刷新限频和物流风险。
- 仓库与设备运营人员：根据寄出、退回和换机物流判断设备在途状态，但必须继续
  完成回仓、验收和检测后才能释放设备。
- 租户管理员：管理快递100租户级配置、承运商编码映射、功能开关和授权权限，
  只能看到脱敏凭据状态。
- 物流运营人员：查看订阅/查询/回调失败任务，执行安全重试、Reconcile、历史
  回填和清理，不直接修改权威轨迹。
- 系统定时任务：租户隔离地领取 Outbox、Inbox、补偿查询和风险计算任务。
- 快递100：接收订阅/查询请求并向公开回调入口推送完整物流轨迹。
- 后续 Provider 适配器：通过供应商无关 `LogisticsProvider` 契约转换供应商
  请求、响应、状态和已验证回调。

## In Scope

### End-to-End Business Outcome

- 在现有 `POST /admin-api/rental/xianyu/order/ship` 成功完成闲鱼发货、设备分配
  和出库后，同一数据库事务创建或复用真实 Delivery、绑定本包裹设备，并写入
  `SUBSCRIBE` 与 `INITIAL_QUERY` Outbox。
- 快递100不可用、承运商映射缺失、Provider 配置缺失或 Provider 功能关闭时，
  已成功的闲鱼发货不得回滚；Delivery 保存明确的禁用、待映射或可重试状态。
- 事务提交后 Worker 调用快递100。任何 Provider 网络调用不得在发货、Delivery
  创建、Inbox 保存或快照持久化事务中执行。
- 快递100回调通过不可猜测 token 定位 Delivery 和租户，完成协议验签后先写入
  Inbox 并快速 ACK，再异步解析完整轨迹并更新 Delivery。
- 排期中心只查询 camera-rental-server 的本地物流读模型，不直接调用快递100。

### Delivery and Package Model

- 新增 `rental_delivery` 作为真实包裹聚合根，保存订单、方向、包裹序号、来源、
  运单、承运商映射、Provider、订阅、查询、回调安全和当前轨迹摘要。
- `direction` 支持 `OUTBOUND`、`RETURN`、`EXCHANGE_OUT`、
  `EXCHANGE_RETURN`。
- 同一租户、订单、方向、规范承运商和规范化运单号形成稳定幂等业务键；重复发货
  回放不得创建重复 Delivery。
- 同一订单可以有多个方向或多个真实包裹；`package_seq` 表示同一方向下的展示
  顺序。
- `rental_delivery_device_rel` 关联 order item、assignment 和具体设备；
  一个包裹可以关联多台设备，同一 Delivery 不得重复关联同一设备。
- order item、assignment、device、delivery 必须属于同一租户和兼容订单关系；
  不匹配时整个创建/绑定事务失败且不留下部分记录。
- `rental_device_shipment` 仅增加可空 `delivery_id`，旧数据和旧接口继续兼容。

### Tracking Snapshots and Platform Status

- 平台统一状态为 `CREATED`、`INFO_RECEIVED`、`PICKED_UP`、
  `IN_TRANSIT`、`OUT_FOR_DELIVERY`、`DELIVERED`、`EXCEPTION`、
  `RETURNING`、`RETURNED`、`CUSTOMS`、`UNKNOWN`。
- 客户主动退回包裹正常送达仓库表示
  `direction=RETURN + tracking_status=DELIVERED`；`RETURNING/RETURNED`
  仅表示承运商异常退回寄件人。
- 快递100状态只能在 Provider 适配层转换为平台状态；前端、领域 Service、
  DO 和 Mapper 不依赖供应商 SDK 枚举或 DTO。
- 快递100返回的完整轨迹按快照保存。每个事件包含
  `snapshot_version`、`event_seq`、fingerprint、业务时间、原始时间、
  平台/供应商状态、文本、位置、来源和 Inbox 引用。
- 聚合器规范化文本和时间、稳定排序、计算事件 fingerprint 和 snapshot hash。
  相同完整快照不写新有效版本、不增加 `tracking_version`。
- 完整历史或有效摘要变化时创建下一快照版本，并原子更新 Delivery 当前快照、
  最新轨迹、位置、ETA 和 `tracking_version`。
- 乱序、迟到或重复数据不能把 `DELIVERED`、`RETURNED` 等终态回退为非终态。
- ETA 可空，不作为排期和风险判断的必要字段。

### Kuaidi100 Provider Integration

- 快递100 Java SDK 和供应商专用 DTO 只允许存在于
  `integration.logistics.kuaidi100`。
- `LogisticsProvider` 提供供应商无关的订阅、查询和已验证回调解析接口。
- 租户级 Provider 公共配置保存启用、查询、订阅、回调密钥、回调基础地址、
  最小查询间隔、结果版本、配置状态和最后验证时间。
- 一个租户可为同一 Provider 配置多组命名凭据，每组独立保存客户编码、API Key、
  启用状态、排序、配置状态和最后验证时间。
- Delivery 第一次执行 Provider 任务时，从当前租户同一 Provider 的完整启用凭据中
  稳定选择并绑定一组；已绑定凭据可用时继续复用，只有被停用、删除或变得不完整时
  才重新选择，避免同一包裹在重试过程中无故切换账号。
- Provider、查询和订阅功能默认关闭。仅有配置记录不代表功能已启用。
- 发货后异步提交一次订阅和一次首次查询；后续以订阅推送为主，主动查询用于首次
  预览、订阅失败、长期无回调、供应商中止、人工刷新和 Reconcile。
- 主动查询使用后端配置的最小间隔，默认不得短于 30 分钟；前端点击刷新只创建
  `REFRESH_QUERY` Outbox，不同步等待快递100。
- 保存按月订阅实际调用次数和下一次允许尝试时间，避免无界重复订阅。
- 支持启用的承运商映射、手机号要求 `NONE/OPTIONAL/REQUIRED` 和映射缺失
  `MAPPING_REQUIRED`。
- CI、普通测试和本地默认启动不得访问真实快递100；使用 mock gateway、
  fixture 和禁止网络断言。

### Inbox, Outbox, Workers, Retry and Callback

- `rental_delivery_outbox` 支持 `SUBSCRIBE`、`INITIAL_QUERY`、
  `REFRESH_QUERY`、`RECONCILE`，使用租户、事件类型和稳定 dedupe key 幂等。
- Outbox 只保存 `delivery_id` 和安全元数据，不保存手机号、地址、完整运单号、
  密钥、token、salt 或回调原文。
- Worker 使用短事务领取任务并提交租约，再在事务外调用 Provider，最后在新事务
  保存结果；失败按可重试/最终失败分类并使用有界退避。
- `rental_delivery_callback_inbox` 保存 Provider、Delivery、任务 ID、
  payload hash、加密回调参数、处理状态、租约锁、重试和脱敏错误。
- 回调入口不要求后台登录，通过 callback token hash 定位，再校验快递100协议
  签名；明文租户 ID、订单号和运单号不得出现在回调 URL。
- 重复回调、重复 payload、Worker 重启和租约超时必须幂等恢复。
- 回调 ACK 与异步处理分离；轨迹处理失败不得诱导供应商无限重复已经安全保存的
  payload。

### Carrier Mapping and Provider Operations

- `rental_logistics_carrier_mapping` 映射来源承运商编码、平台规范编码和
  Provider 编码，例如闲鱼编码到快递100编码。
- 缺少映射时闲鱼发货成功，Delivery 显示“承运商映射待配置”，不调用 Provider。
- 提供租户管理员可用的承运商映射和 Provider 配置页面/API；支持多组 Provider
  凭据新增、更新、启停、排序、删除和本地完整性验证。密钥只支持保留、替换或
  清除，响应永不返回明文。
- 提供失败 Outbox/Inbox 查询、安全重试、Reconcile 和历史 Delivery 回填入口。
- 历史回填只基于现有本地 shipment/order 数据创建 Delivery，不在结构 migration
  中执行，不自动调用生产 Provider；是否订阅由独立显式操作和开关控制。
- 提供状态数量、处理延迟、失败数、重试数、最后成功时间和 stale 数量等安全指标。
- 提供轨迹快照保留和 Inbox/Outbox 清理策略；删除只作用于超过保留期且不再需要
  审计的技术数据，不删除 Delivery、shipment 或订单历史。

### Schedule Center APIs and Read Model

- `POST /admin-api/rental/delivery/tracking-summary/batch` 按一组 orderId
  一次返回当前窗口所需的所有 Delivery 摘要，禁止每个排期块单独请求。
- `GET /admin-api/rental/delivery/{deliveryId}/tracking` 只在打开物流详情时
  返回当前有效完整轨迹和必要包裹信息。
- `POST /admin-api/rental/delivery/{deliveryId}/refresh` 只提交安全的
  `REFRESH_QUERY` Outbox，返回 `accepted`、稳定 reason code 和
  `nextAllowedAt`，不执行同步供应商查询。
- 提供受权限控制的 Delivery 列表、承运商映射、Provider 配置、失败任务和
  Reconcile API。
- 排期中心维护 `trackingByOrderId`，同一订单关联多台设备时不在每个
  `ScheduleBlock` 复制完整物流对象。
- 页面初次加载排期后收集当前窗口 orderId 并批量加载本地摘要；页面可见时每
  60 秒刷新本地摘要，隐藏时暂停，重新可见时立即刷新。
- 第一版不使用 SSE；本地摘要轮询频率与 Provider 主动查询限频完全分离。

### Schedule Center User Experience

- 单包裹紧凑状态至少显示方向、平台状态、承运商、脱敏运单号和最近更新时间。
- 多包裹显示包裹总数和状态分布，例如“2 个包裹，1 个已签收，1 个运输中”。
- 点击物流摘要打开详情抽屉，按包裹展示方向、状态、脱敏运单、最近同步时间、
  ETA（如有）、完整轨迹、刷新按钮和刷新结果。
- 轨迹按业务时间倒序展示，状态必须同时使用文字和图标/颜色，不依赖颜色表达。
- 支持 loading、empty、partial error、permission denied、mapping required、
  provider disabled、stale、query throttled、refresh queued、exception 和
  delivered 等状态。
- 普通排期视图默认只展示脱敏运单和安全物流文本，不展示完整手机号、地址或
  Provider 原始错误。
- 保留现有发货工作台“运单草稿 -> 设备 -> 待发货订单 -> 确认发货”流程；
  OCR 仍只是人工复核草稿，不能自动发货或自动绑定 Delivery。

### Logistics Risk and Device Lifecycle

- 后端结合 Delivery 状态、租期、占用区间和下一单排期计算稳定风险码：
  `OUTBOUND_NOT_PICKED_UP`、`OUTBOUND_DELIVERY_DELAY`、
  `RETURN_NOT_SHIPPED`、`RETURN_DELIVERY_DELAY`、`TRACKING_STALE`、
  `LOGISTICS_EXCEPTION`、`MAPPING_REQUIRED`、`SUBSCRIPTION_FAILED`。
- 排期中心异常页扩展 `LOGISTICS_RISK`，显示严重度、受影响订单/设备、安全说明
  和允许的下一步。
- 物流签收只更新物流状态和风险，不自动修改设备可用性、回仓、验收、检测或
  排期释放。设备仍需员工完成现有回仓和检测流程。

### Security, Privacy and Tenancy

- 所有新表遵循 `TenantBaseDO`、审计、逻辑删除、索引和现有无物理外键约定。
- 不同租户可使用相同运单、Provider 编码、payload hash 或 dedupe key，数据和
  唯一约束保持隔离。
- `tracking_phone`、callback token/salt、回调参数、Provider 回调密钥和每组
  Provider 凭据使用项目 `EncryptTypeHandler`；敏感 DO 字段加
  `@ToString.Exclude`。
- 日志、异常、指标、测试 fixture、前端状态和审计不得输出完整运单号、手机号、
  地址、密钥、token、salt 或回调原文。
- 管理端查询、刷新、配置、映射、任务重试、Reconcile 和历史回填分别使用明确
  后端权限，不能只依赖前端隐藏按钮。

## Out of Scope

- 客户 uni-app、客户 Nuxt Web 或公开查件页面；本 Change 的用户界面只覆盖
  现有管理排期中心和必要物流运营配置。
- GPS 车辆位置、秒级实时轨迹或对快递公司更新频率作产品承诺。
- 第一版 SSE/WebSocket；页面使用本地摘要轮询。
- 根据物流签收自动释放设备、完成回仓、通过检测或关闭租赁订单。
- 自动决定退款、赔偿、延误责任或客户通知。
- 在测试中使用真实快递100账号、生产密钥、生产运单或真实客户隐私。
- 修改除发货以外的闲管家写操作，或让前端直接调用快递100/闲管家。
- 引入消息队列或拆分物流微服务；复用当前 Spring 单体、MySQL、Redis 和 Job
  基础设施。
- 覆盖、删除或重写已经执行的 migration、shipment、订单、设备或轨迹审计历史。
- 自动提交、推送、创建 PR、部署或执行生产数据库 migration。

## UI Design Impact

- Foundation spec: `openspec/specs/ui-design/design.md`
- 复用现有排期中心 React 19、Tailwind 4、语义 CSS 变量、`StatusBadge`、
  `DetailDrawerShell`、`EmptyState`、`PermissionAwareAction` 和响应式外壳。
- 新增排期物流摘要、物流详情抽屉、轨迹时间线、刷新状态、物流风险以及必要的
  Provider/映射/失败任务运营界面。
- 保持桌面高密度排期表，移动端通过内部横向滚动和抽屉完成详情，不制造页面级
  横向溢出。
- 所有状态使用文字加视觉语义，客户隐私和运单默认脱敏。

## Theme & Locale Capability Impact

- Theme support: `light-dark`。
- Theme toggle policy: 复用现有 `theme-toggle:user`，不得新增第二套偏好来源。
- Internationalization: `enabled`。
- Supported locales: `zh-CN,en`。
- Default locale: `zh-CN`，fallback 为 `zh-CN`。
- Prototype coverage: 完整业务原型必须覆盖 light/dark、`zh-CN`/`en`，
  桌面和窄屏，以及 loading、empty、permission、mapping-required、
  provider-disabled、throttled、exception、multi-package 和 delivered 状态。

## Architecture & Database Impact

- Foundation spec: `openspec/specs/system-architecture/design.md`
- 影响 `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`、
  MySQL migration、排期中心 React 应用和物流 ADR。
- 新增 8 张物流表：
  `rental_delivery`、`rental_delivery_device_rel`、
  `rental_delivery_trace`、`rental_delivery_callback_inbox`、
  `rental_delivery_outbox`、`rental_logistics_carrier_mapping`、
  `rental_logistics_provider_config`、`rental_logistics_provider_credential`。
- `rental_delivery.provider_credential_id` 保存包裹当前稳定绑定的 Provider 凭据。
- 为 `rental_device_shipment` 增加可空 `delivery_id`。
- migration 只做加法结构变化；历史回填是独立运营任务。
- 后端始终是物流状态、幂等、限频、权限、风险和隐私的权威来源。

## Frontend-Backend Data Flow Impact

- Foundation spec: `openspec/specs/frontend-backend-data-flow/design.md`
- 新增：
  `FLOW-XIANYU-SHIPMENT-DELIVERY`、
  `FLOW-DELIVERY-SUBSCRIBE`、
  `FLOW-DELIVERY-INITIAL-QUERY`、
  `FLOW-KUAIDI100-CALLBACK`、
  `FLOW-DELIVERY-QUERY-COMPENSATION`、
  `FLOW-SCHEDULE-TRACKING-SUMMARY`、
  `FLOW-DELIVERY-MANUAL-REFRESH`、
  `FLOW-LOGISTICS-RISK`、
  `FLOW-DELIVERY-BACKFILL`。
- 所有第三方流量终止在 Java Provider 适配层；排期中心只使用管理端本地 API。
- 外部状态最终一致，本地 Delivery/trace/Inbox/Outbox 更新使用明确事务和幂等。

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`
- 后端 Service 负责事务和跨实体校验，Mapper 只负责持久化，Provider 适配器只
  负责供应商协议，Controller 不直接调用 Mapper。
- 快照规范化、fingerprint、hash、状态聚合、终态保护、脱敏、风险计算和承运商
  映射必须提取为独立可测组件。
- 前端提取物流摘要、轨迹抽屉、时间线、状态展示、轮询/可见性和刷新命令，页面
  不计算权威状态、限频或风险。
- 禁止把完整轨迹复制到每个 ScheduleBlock，禁止前端持有第二套订单/物流真相。

## Unresolved Gaps

- 无。完整业务目标、五阶段范围、排期中心展示、快递100交互、降级、限频、
  安全、租户隔离和设备生命周期边界均已由用户确认及引用会话锁定。
