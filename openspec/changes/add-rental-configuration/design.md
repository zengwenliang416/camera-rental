## Context

订单详情官方响应同时提供 `goods.product_id`（闲管家商品 ID）、
`goods.item_id`（闲鱼商品 ID）和 `goods.sku_id`（闲管家 SKU ID），但当前解析器
使用 `product_id` 优先、`item_id` 回退的单字段模型。闲鱼 `xy_sku_id` 不在订单详情
中，只存在于多规格商品查询响应，必须通过同步商品和闲管家 SKU 的唯一关系关联。

当前转换服务只有在实付金额、有效租期、占用日期和商品型号映射全部齐全时才创建
`rental_order`，使无备注或未配置型号的已收款订单长期停留在 `PENDING` 或
`REVIEW_REQUIRED`。设备目录维护入口又嵌在“租赁设备”的创建对话框中，无法承载
商品规则、SKU 映射、过滤策略和备注规范。

## Goals / Non-Goals

**Goals:**

- 建立独立、权限化、可审计的租赁配置入口。
- 精确区分并保存闲管家和闲鱼的商品/SKU 标识，不使用回退或文本推断。
- 普通渠道订单先创建内部订单，再增量补全计划和型号。
- 支持单型号和多型号商品，后者严格按同步的闲管家 SKU 匹配。
- 配置过滤不影响渠道收入统计和原始证据。
- 保护设备分配、出库、换机、回仓、检测和财务事实。
- 提供幂等、可恢复的历史补建。

**Non-Goals:**

- 不调用第三方写接口。
- 不从商品标题、SKU 文本或备注推断型号或具体设备。
- 不根据备注执行实际换机、回仓、检测、退款或赔偿。
- 不修改客户、员工或 PC 官网。

## Decisions

### 1. Add explicit identifiers and stop runtime use of ambiguous columns

新增并使用：

- `xianyu_product.xgj_product_id`
- `xianyu_product.xianyu_item_id`
- `xianyu_product_sku.xgj_sku_id`
- `xianyu_product_sku.xianyu_sku_id`
- `xianyu_order.xgj_product_id`
- `xianyu_order.xianyu_item_id`
- `xianyu_order.xgj_sku_id`
- `xianyu_order.xianyu_sku_id`

现有 `external_product_id`、`external_sku_id` 只作为迁移前历史证据保留，完成精确
回填后应用代码不再读取或写入它们，也不把它们作为兼容回退。

选择新增明确字段而非直接重命名，是因为当前历史值包含错误语义，直接重命名会把错误
值伪装成已确认数据，并使旧版本代码无法安全回滚。

### 2. Derive Xianyu SKU only through synchronized product data

订单解析器直接保存 `product_id`、`item_id`、`sku_id`，订单的 `xianyu_sku_id`
初始允许为空。商品同步保存 `publish_shop.item_id`，SKU 同步保存
`sku_id + xy_sku_id`。订单重评服务只有在以下关系唯一时才补充 `xianyu_sku_id`：

```text
tenant + shop + xgj_product_id + xianyu_item_id + xgj_sku_id
```

找不到或存在多个候选时保持为空并记录原因，不使用 SKU 文本或商家编码。

### 3. Use parent product rules and child SKU mappings

新增父表 `rental_channel_product_rule`：

- `shop_id`
- `xianyu_item_id`
- `xgj_product_id`（同步证据，可为空）
- `handling_policy`: `CREATE_RENTAL | CONFIG_SKIPPED`
- `model_mode`: `SINGLE | BY_SKU`
- `equipment_model_code`（仅 `SINGLE`）
- `enabled`
- `note`

新增子表 `rental_channel_product_sku_mapping`：

- `product_rule_id`
- `xgj_sku_id`
- `xianyu_sku_id`（同步证据，可为空）
- `equipment_model_code`
- `enabled`

父表以 `tenant_id + shop_id + xianyu_item_id` 唯一，子表以
`tenant_id + product_rule_id + xgj_sku_id` 唯一。`BY_SKU` 不允许父级默认型号，
从数据结构上阻止回退。

不继续扩展 `xianyu_product_mapping`，因为它使用含混外部字段且没有父级处理策略。
迁移只对能通过同步商品数据唯一关联的旧映射生成新规则；无法精确关联的旧映射保留
原表供审计，但不参与运行时匹配。

### 4. Separate order existence from preparation readiness

未命中过滤规则的成功订单详情在同一事务中幂等创建：

- 一个 `rental_order`
- 一个 `rental_order_item`
- 渠道订单与内部订单关联

订单生命周期状态继续表达业务履约；新增 `preparation_status` 和
`preparation_reason_code` 表达是否具备分配条件：

- `WAITING_IDENTIFIERS`
- `WAITING_REMARK`
- `WAITING_MODEL`
- `WAITING_OCCUPANCY`
- `CONFLICT_REVIEW`
- `READY`

明细的型号和日期继续允许为空。设备分配和排期入口统一调用
`RentalOrderPreparationPolicy.requireReady(...)`，不能只检查某个字段非空。

渠道订单 `conversion_status` 表达关联结果：

- `CONVERTED`: 已创建内部订单，不代表已具备分配条件。
- `CONFIG_SKIPPED`: 命中过滤策略。
- `FAILED`: 本地持久化或不可恢复数据错误。

缺备注、缺型号等正常待补全情况不再创建 `ORDER_CONVERSION` 人工复核。

### 5. Centralize reconciliation

新增 `RentalChannelOrderReconciliationService`，作为以下入口的唯一补全编排：

- 订单详情持久化
- 商品详情/SKU 同步
- 备注回放
- 商品规则创建、修改或启用
- 管理员手工重评
- 历史补建

处理顺序：

```text
lock channel order
-> resolve exact product rule
-> CONFIG_SKIPPED or ensure internal order
-> resolve exact SKU/model
-> evaluate latest valid remark plan
-> apply fulfillment-safe plan/model changes
-> compute preparation status
-> resolve or open only conflict/manual reviews
```

所有入口共享同一事务服务，避免转换、发货和回填各自生成不同映射规则。

### 6. Preserve valid remark snapshots

新增 `xianyu_order_remark_history` 保存每次同步的：

- remark hash 和受限原文
- parser version/status/reason
- parsed plan dates
- whether the snapshot became effective
- change classification

`xianyu_order` 保存最新原始解析结果；`rental_order`/item 保存最后一个有效计划。
新快照失败时只更新审计和准备原因，不清空有效计划。

### 7. Apply fulfillment guards before changes

`RentalFulfillmentUpdateGuard` 根据当前事实选择：

- 未分配：自动更新型号和计划。
- 已分配未出库：锁定分配和排期，检查新占用范围，无冲突才调整。
- 已出库：保持当前设备；续租无冲突可延长预计范围，有冲突转复核。
- 早退：更新预计发回，不释放有效占用。
- 换机提示：只开复核；实际换机必须通过关闭旧分配和创建新分配的命令。
- 已回仓/检测/结算：不从备注或配置反向修改事实。

### 8. Move catalog mutation behind configuration permissions

目录查询仍由租赁设备页复用。目录新增、编辑和启停迁移到
`/admin-api/rental/config/**`，使用：

- `rental:configuration:query`
- `rental:configuration:update`

旧 `/rental/device/catalog/*/create` 写入口在生产代码和管理端调用中移除，不提供
兼容代理；`/rental/device/catalog` 只保留读取。

### 9. Run historical reconciliation in bounded batches

补建服务按 `xianyu_order.id` 稳定分页并记录任务游标、边界和计数。单条失败不回滚
已提交批次，但不会把不可恢复游标推进到失败记录之后。重复运行使用渠道订单唯一键和
内部订单来源唯一键保证幂等。

29 个过滤规则通过部署前解析得到的内部 `shop_id` 写入；店铺显示名不能唯一对应时，
迁移/运维步骤失败并停止该店铺的过滤导入。

## API Shape

- 配置页使用 `GET/POST/PUT /admin-api/rental/config/**`。
- 外部长标识全部按字符串收发。
- 商品规则写请求引用内部 `shopId`、`xianyuItemId`、模式、策略和已同步 SKU ID。
- 危险修改先调用 impact 只读接口，返回受影响的未分配、已分配、已出库和冲突数量。
- 写接口接受稳定幂等键或版本号，并返回重评任务 ID 和初始计数。

## Migration Plan

1. 部署增量 schema、菜单、权限和后台任务结构，旧代码仍可运行。
2. 从原始订单详情回填 `xgj_product_id`、`xianyu_item_id`、`xgj_sku_id`。
3. 从商品详情和 SKU 原始数据回填商品与 SKU 明确字段。
4. 仅迁移可唯一关联的旧型号映射；其余记录输出待配置报告。
5. 唯一解析“小疆”“发发”店铺并导入 29 个过滤规则；解析失败则停止该店铺导入。
6. 部署新后端，启用即时建单和重评，但先以小批量历史补建验证计数。
7. 部署管理端配置页并移除设备页快捷新增。
8. 分批运行历史补建，核对金额守恒、创建数量、过滤数量和冲突列表。
9. 观察同步、重评、分配拒绝和人工复核指标后扩大批次。

回滚只回退应用流量和暂停补建任务；新增表、明确字段、内部订单和历史审计不删除。

## Risks / Trade-offs

- [历史原始载荷可能缺失 `item_id`] -> 保持明确字段为空，普通订单仍建单但不应用过滤
  或型号规则，进入 `WAITING_IDENTIFIERS`。
- [旧型号映射基于错误商品字段] -> 只迁移可唯一证明的记录，不批量猜测。
- [配置变更影响大量订单] -> 先展示 impact，异步分批重评，已履约订单只报告不改写。
- [续租与未来排期冲突] -> 事务锁和统一冲突检查，冲突保留原排期并进入复核。
- [新旧应用版本并存] -> schema 先行且保持加法兼容；新代码不读取含混字段。
- [过滤规则错误导致漏建单] -> 严格店铺 + item 匹配，已履约历史不逆转，规则变更有审计和影响预览。

## Open Questions

None.
