# 闲管家字段映射

## 原则

字段映射集中维护，不散落在 Controller、页面或报表中。每次官方字段或业务
解析规则变化，都要保留原始值、标准化值、转换版本和异常原因。

## 订单映射基线

| 外部字段 | 内部字段 / 用途 | 当前规则 |
| --- | --- | --- |
| `order_no` | `external_order_no` | 外部订单幂等键，始终按字符串处理 |
| `authorize_id` | 渠道店铺授权 | 同步前校验有效性 |
| `seller_remark` | `rental_remark_raw` | 仅用于日期解析 |
| `total_amount` | `external_total_amount` | 官方订单接口明确为分 |
| `pay_amount` | `rental_income_amount` | 官方订单接口明确为分，作为总租金收入 |
| `refund_amount` | `refund_amount` | 订单接口明确为分；售后接口单位仍需确认 |
| `express_fee` | `express_fee` | 官方订单接口明确为分 |
| `goods.price` | `external_unit_price` | 官方订单接口明确为分 |
| `update_time` | `external_update_time` | Unix 秒，参与增量游标 |
| `consign_type` | `external_consign_type` | `1` 物流发货，`2` 虚拟发货 |

外部标识不参与算术。即使官方 schema 标为 `int64`，只要字段是 ID 而不是数量
或金额，内部领域模型和前端 API 都优先按字符串表达，避免 JSON / JavaScript
精度丢失。

## 必须保存的原始信息

- 原始订单 JSON。
- 原始售后 JSON。
- 原始推送 JSON 或脱敏摘要。
- 原始卖家备注。
- 原始金额字段及其单位说明。
- `raw_payload_hash`。
- `schema_version` 和 `parser_version`。

## 日期解析字段

| 结果 | 说明 |
| --- | --- |
| 发货日期 | 用于设备占用周期，不参与计租天数 |
| 收货日期 | 默认计租开始的前一天 |
| 计租开始 | 明确租期优先，否则收货次日 |
| 计租结束 | 明确租期优先，否则发回当天 |
| 发回日期 | 用于业务流程和占用周期 |
| 解析状态 | 成功、待复核或失败 |
| 解析错误 | 保存可解释的错误原因 |

## 金额规则

- 订单列表 / 详情的 `total_amount`、`pay_amount`、`refund_amount`、
  `express_fee` 和 `goods.price` 已确认单位为分。
- 售后列表 / 详情的 `refund_amount` 当前文档未明确单位，必须保存原始值和
  单位确认状态。
- 内部统一用整数分存储。
- 渠道订单和已转换的租赁订单金额都使用 `BIGINT` 分值列，避免官方 `int64`
  金额在转换时截断。
- 不使用浮点数结算。
- 每日分摊金额之和必须严格等于实付金额。
- 无法整除的分币余数放在最后一个计租日。
- 金额关系异常时保留官方原值并标记待核对，不静默修正。

## 当前字段核对

| 项目 | 状态 |
| --- | --- |
| 订单金额单位 | 已确认：分 |
| 售后金额单位 | 文档未明确，待进一步确认 |
| 官方时间字段 | Unix 秒；业务日期按 `Asia/Shanghai` 派生 |
| 商品、SKU、规格字段 | 订单接口已确认 `product_id`、`item_id`、`outer_id`、`sku_id`、`sku_outer_id`、`sku_text` |
| 售后状态枚举 | 已取得当前枚举；部分 OpenAPI 字段类型异常，保留未知原值 |
