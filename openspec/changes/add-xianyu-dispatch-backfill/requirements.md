# Requirements: add-xianyu-dispatch-backfill

## Summary

为已经在闲鱼侧发货、但平台内尚未登记实际出库设备的订单，提供 Web 管理后台人工补录流程。店铺管理员无需物理扫码设备，直接输入设备编号、现有运单信息、实际发货时间和补录原因；后端以本地事务补齐租赁订单转换、设备分配、占用排期、设备出库、发货记录和 outbound Delivery。

该流程是历史事实补录，不是再次发货。它不得读取闲管家写开关、不得要求闲管家店铺授权仍然有效、不得调用 `XianyuWriteClient` 或任何闲管家写接口。

## Users & Actors

- 店铺管理员 / 租赁运营人员：在 Web 管理后台查看本租户闲鱼订单，对已发货订单录入实际寄出的设备与物流事实。
- 后端租赁服务：校验租户、订单状态、设备状态、排期和幂等关系，并在事务中写入本地领域记录。
- 闲管家：不参与本流程，不接收任何请求。

## In Scope

- 管理端订单列表仅对 `orderStatus` 为 `21`（已发货）或 `22`（交易成功）且未取消的订单显示“补录出库设备”。
- 操作入口与后端接口复用权限 `rental:xianyu:ship`；前端隐藏无权限操作，后端通过 `@PreAuthorize` 强制校验。
- 新接口 `POST /admin-api/rental/xianyu/order/dispatch-backfill` 接收：
  - `channelOrderId`；
  - `deviceId` 或 `deviceNo`，至少一个；
  - `idempotencyKey`；
  - `expressCode`、`expressName`、`waybillNo`；
  - `consignTime`，格式遵循项目 `yyyy-MM-dd HH:mm:ss` 约定；
  - `reason`，必填且最多 480 字符。
- 管理端弹窗以订单已有运单、承运商和发货时间为默认值，管理员输入实际设备编号并确认补录原因。
- 弹窗必须明确警示：“仅补齐本地设备、排期和物流记录，不会再次调用闲管家发货接口。”
- 后端仅接受状态 `21`、`22`；待发货、已退款、已取消或已关闭订单必须拒绝，且不得产生本地写入或远程调用。
- 后端按当前租户锁定渠道订单，并确认订单所属店铺存在于当前租户；本流程不要求店铺当前闲管家授权状态为有效。
- 后端按 `deviceId` 或规范化后的 `deviceNo` 锁定设备：
  - 新建分配或推进 `ASSIGNED -> DISPATCHED` 时，设备必须启用且状态为 `AVAILABLE`；
  - 已存在同一订单明细、同一设备的 `DISPATCHED` 分配时，仅当设备状态为 `RENTED` 才允许复用；
  - 其他设备/分配状态组合必须返回冲突。
- 渠道订单尚未转换为内部租赁订单时，使用所选设备的器材型号执行现有转换；转换成功后关联当前实现选择的首个租赁订单明细。
- 创建或复用设备分配及对应占用排期，并通过现有 `RentalDeviceOpsService.dispatch` 把设备置为 `RENTED`、分配置为 `DISPATCHED`。
- 新增或复用 `rental_device_shipment` 记录，写入 `source=ADMIN_BACKFILL`、请求审计哈希、补录原因和既有物流信息。
- 使用稳定的 `shipment-backfill:<idempotency hash>` 来源标识创建或复用 outbound `RentalDelivery`，并将 Delivery 关联回 Shipment。
- 更新渠道订单的运单号、承运商、实际发货时间；仅当 `shipDate` 为空时，以 `consignTime` 的 `Asia/Shanghai` 日期补齐。
- 相同幂等键与完全相同业务参数重放时返回既有结果，不重复创建分配、排期、Shipment 或 Delivery。
- 相同幂等键绑定不同订单、设备或请求摘要时拒绝；同一订单、运单号、承运商已绑定其他设备时返回补录冲突。
- 所有本地写入在同一事务边界内失败回滚。

## Out of Scope

- 不调用闲管家 `/api/open/order/ship`，也不调用任何其他闲管家写接口。
- 不修改闲管家远端订单状态、备注、物流或库存。
- 不为待发货订单执行正式发货；待发货继续使用原有发货流程。
- 不支持退款、取消、关闭订单的设备补录。
- 不要求物理扫码枪、摄像头、移动端扫码或员工 uni-app。
- 不新增店铺级用户数据权限；本 change 沿用现有租户隔离和 `rental:xianyu:ship` 权限。
- 不支持同一运单对应多台设备；现有 Shipment 业务键/唯一约束将其视为冲突。
- 不改变多明细订单的分配策略；当前实现关联首个租赁订单明细。
- 不新增数据库表、迁移、配置项、菜单权限码或角色。

## UI Design Impact

- Foundation spec: `openspec/specs/ui-design/design.md`
- 在现有闲鱼订单表的操作列增加文字按钮，复用 Element Plus 表格、按钮、对话框、Descriptions、Alert、Form、Input 和 DatePicker。
- 弹窗宽度为桌面管理端的有界操作对话框；窄屏仍需保持表单可读和可操作。
- 订单号、状态和商品标题作为上下文只读展示；实际设备编号、物流、时间和原因保持可见标签。
- 提交期间显示 loading 并防止重复点击；只有服务端成功后关闭弹窗、显示成功消息并刷新订单列表。
- 后端校验错误由现有统一错误处理展示，表单草稿在失败时保留。
- 状态和风险不能仅依赖颜色；“不会远程发货”使用明确文本警告。

## Theme & Locale Capability Impact

- Theme support: `light-dark`
- Theme toggle policy: 复用管理后台现有用户主题切换，不新增独立开关。
- Internationalization: `enabled`
- Supported locales: `zh-CN`, `en`
- Default locale: `zh-CN`
- Prototype coverage: 管理端桌面与窄屏弹窗均覆盖 light/dark × `zh-CN`/`en`；至少检查默认值、校验失败、提交中、后端冲突和成功状态。

## Architecture & Database Impact

- Foundation spec: `openspec/specs/system-architecture/design.md`
- 权威业务逻辑位于 `yudao-module-rental-biz` 的 `XianyuOrderShipService.backfillDispatch`，Controller 不直接访问 Mapper。
- API 使用管理端前缀 `/admin-api/rental/**`、统一响应结构、Bean Validation 和后端权限校验。
- 复用现有租赁订单转换、设备分配、设备出库、Shipment 和 Delivery 服务，不创建第二套库存或排期逻辑。
- 使用数据库行锁/现有分配服务的并发控制，在事务中重新检查订单、设备、分配和业务键。
- 复用现有数据表，无数据库迁移：
  - `xianyu_order`；
  - `rental_order`、`rental_order_item`；
  - `rental_device`、`rental_device_assignment`、`rental_schedule`；
  - `rental_device_shipment`、`rental_delivery`。
- 新增三个稳定错误码：未发货、退款/关闭、补录冲突。
- `XianyuWriteClient` 依赖保留在既有正式发货方法中；补录调用路径不得触达该依赖或运行时写配置。
- 已接受限制：
  - 授权边界为租户级，不提供用户到具体闲鱼店铺的数据级隔离；
  - 同一运单多设备受现有 Shipment 唯一约束限制；
  - 多明细订单当前选择首个租赁订单明细。

## Frontend-Backend Data Flow Impact

- Foundation spec: `openspec/specs/frontend-backend-data-flow/design.md`
- 新流程 `FLOW-XIANYU-DISPATCH-BACKFILL`：
  1. 管理员在状态 `21`/`22` 的订单行点击“补录出库设备”。
  2. 前端展示订单上下文，并预填已有物流与发货时间。
  3. 管理员输入实际设备编号、确认物流、时间和原因。
  4. 前端生成本次弹窗会话的幂等键并提交 typed request。
  5. 后端验证权限、租户店铺、订单状态、设备和现有绑定。
  6. 后端转换订单（如需要）、创建/复用分配与排期、执行本地 dispatch、写 Shipment、创建/复用 Delivery、更新渠道订单物流。
  7. 后端返回本地 Shipment、Delivery、设备编号和 `DISPATCHED` 状态；前端提示成功并刷新列表。
- 所有权威状态由后端返回；前端不计算设备可用性、不创建排期、不推断订单能否补录。
- 网络或业务失败时不做乐观更新；管理员可保留表单并重试，同一弹窗会话沿用同一幂等键。
- 本流程没有外部 API 调用，不能以闲管家写开关或授权失效阻止历史本地事实补录。

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`
- 新增 `XianyuDispatchBackfillDialog`，只负责表单、客户端基础校验、提交态和完成事件，不包含设备状态、排期或转换规则。
- 订单页只负责入口可见性、当前订单选择和成功后的列表刷新。
- typed API 定义位于现有 `src/api/rental/xianyu.ts`，弹窗不得直接拼接 URL 或使用原始 HTTP 客户端。
- 后端复用 `XianyuOrderShipService` 作为同一领域的协调服务，但将正式远程发货与本地补录保持两个显式入口。
- 当前只出现一个补录弹窗，不抽取新的 hook 或跨页面组件；若后续员工端或其他页面复用该流程，再提取共享表单/submit guard。

## Unresolved Gaps

- 无。上述限制作为本 change 的明确边界接受，后续扩展必须建立独立 change。
