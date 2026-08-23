# Acceptance Criteria: add-xianyu-dispatch-backfill

## User-Visible Criteria

- 具有 `rental:xianyu:ship` 权限的管理员在闲鱼订单列表中，仅能对状态 `21`、`22` 且未取消的订单看到“补录出库设备”操作。
- 弹窗展示外部订单号、订单状态和商品标题，并允许管理员输入实际设备编号、运单号、承运商编码/名称、实际发货时间和补录原因。
- 弹窗必须明确显示“不会再次调用闲管家发货接口”的警告；流程不要求连接扫码设备。
- 必填、运单号格式和原因长度校验失败时不得提交，且对应字段显示可理解的 `zh-CN`/`en` 文案。
- 提交中按钮进入 loading；后端失败时弹窗和草稿保留，成功时显示实际设备编号、关闭弹窗并刷新订单列表。
- 管理后台现有 light/dark 主题和 `zh-CN`/`en` 语言切换下，入口、弹窗、校验、警告和结果均可读、可操作。

## System Criteria

- `POST /admin-api/rental/xianyu/order/dispatch-backfill` 必须由 `rental:xianyu:ship` 后端权限保护，并验证请求中至少存在 `deviceId` 或 `deviceNo`。
- 只有状态 `21`、`22` 且未取消的渠道订单可以补录；待发货返回 `XIANYU_DISPATCH_BACKFILL_ORDER_NOT_SHIPPED`，退款/关闭返回 `XIANYU_DISPATCH_BACKFILL_ORDER_CLOSED`。
- 补录只要求渠道订单的店铺属于当前租户；不得读取闲管家 write-enabled 配置，不得要求店铺授权状态为 `VALID`。
- 补录执行期间 `XianyuWriteClient.execute` 调用次数必须为零。
- 新设备绑定必须通过现有分配服务创建占用排期，并通过 `RentalDeviceOpsService.dispatch` 将设备变为 `RENTED`、分配置为 `DISPATCHED`。
- 已存在同设备的 `ASSIGNED` 分配时直接推进出库；已存在一致的 `DISPATCHED` + `RENTED` 状态时复用，不重复 dispatch。
- 已有设备/分配状态不一致、同一业务运单绑定其他设备或其他不可兼容关系时返回 `XIANYU_DISPATCH_BACKFILL_CONFLICT`。
- 未转换渠道订单必须使用所选设备型号完成现有租赁订单转换，转换或首个订单明细不完整时不得产生部分写入。
- 相同幂等键和相同规范化请求返回原结果；相同幂等键用于不同订单、设备、物流或原因时返回 `XIANYU_SHIP_IDEMPOTENT_KEY_REUSED`。
- 任一持久化或 Delivery 创建失败时，事务必须回滚本次分配、排期、设备状态、Shipment 和渠道订单更新。

## Data Criteria

- 成功补录恰好形成或复用一个有效设备分配与占用排期，并存在一个 `source=ADMIN_BACKFILL` 的 `rental_device_shipment`。
- Shipment 保存当前渠道订单、分配、设备、幂等键、运单、承运商、请求审计哈希、补录原因和 Delivery 关联。
- outbound Delivery 使用 `shipment-backfill:<idempotency hash>` 作为稳定来源标识，并关联实际设备。
- 渠道订单的 `waybillNo`、`expressCode`、`expressName` 和 `consignTime` 更新为提交值；仅在 `shipDate` 原为空时补齐上海时区日期。
- 无数据库迁移或配置变更；现有表、索引、逻辑删除和租户字段保持不变。
- 普通列表、消息和验证证据不得泄露客户完整手机号、地址、第三方密钥或生产凭据。

## Component Criteria

- `XianyuDispatchBackfillDialog` 通过 typed API 模块提交，不直接访问 Mapper、数据库、闲管家或原始 HTTP 客户端。
- 订单页只根据服务端订单状态与权限显示入口，不复制后端设备、排期和幂等业务规则。
- 后端补录入口复用现有转换、分配、dispatch、Shipment 和 Delivery 能力，不新增平行业务实现。
- Reusable components, hooks, utilities, or services named in
  `component-impact-map.json` are extracted instead of duplicated.

## Verification Surfaces

- Facticity: 将 requirements、acceptance、API/VO、Service、管理端表单与当前代码逐项比对，确认端点、状态 `21/22`、权限和“零远程写调用”一致。
- Static: 后端目标模块测试编译通过；管理端 `vue-tsc --noEmit`、目标 ESLint、目标 Prettier 和 `git diff --check` 通过。
- Unit: 覆盖本地成功补录、待发货拒绝、退款/关闭拒绝、同键重放、同键冲突、同运单异设备冲突、已有 `DISPATCHED` 复用、未转换订单转换、Delivery 失败回滚和零 `XianyuWriteClient` 调用。
- Redteam: 覆盖跨租户订单/店铺、伪造设备、停用或忙碌设备、篡改幂等键、冲突业务键、退款/关闭订单，并证明所有拒绝路径无远程写调用、无部分本地写入。
- E2E: 具有权限的管理员从状态 `21/22` 订单打开弹窗，以网页键盘输入设备与物流信息，提交后看到成功提示和刷新结果；无权限及非 eligible 状态不能操作。
- Sensory: 在 light/dark × `zh-CN`/`en` 下检查桌面和窄屏的默认、校验、提交中、冲突、错误和成功状态，确认警告、焦点、标签、对比度和非颜色状态提示。

## Unresolved Gaps

- 无。
