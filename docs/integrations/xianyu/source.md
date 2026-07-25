# 闲管家官方接口来源

最近在线核对日期：`2026-07-25`。

## 官方索引

```text
https://s.apifox.cn/3ac13d69-5a38-4536-ae9b-a54001854ef8/llms.txt
```

当前索引包含以下类别：

- 接入说明、代码示例和应用接入教程
- 用户：查询闲鱼店铺
- 商品：类目、属性、列表、详情、规格、创建、编辑、库存、上下架和删除
- 订单：列表、详情、卡密、物流发货和修改价格
- 售后：列表、详情、同意退款、拒绝退款和拒绝原因
- 推送：商品回调、商品推送和订单推送
- 其他：快递公司查询

## 使用规则

1. 开发前重新读取官方 `llms.txt`。
2. 从索引定位目标接口。
3. 打开目标接口的完整 Markdown 文档。
4. 记录本次实现使用的请求地址、方法、认证 / 签名、字段、响应、错误码和限制。
5. 用脱敏 Fixture 或 Mock Server 编写测试。
6. 完成后在任务报告中说明是否调用过真实接口、是否执行过写操作。

本文件是项目导航，不复制完整接口参数。若本地说明与在线文档冲突，以在线文档为准，并在领域文档或决策记录中记录兼容性影响。

## 本地文档导航

| 文档 | 用途 |
| --- | --- |
| [overview.md](overview.md) | 集成范围、阶段和风险边界 |
| [authentication.md](authentication.md) | 凭据、请求签名和客户端边界 |
| [shop-authorization.md](shop-authorization.md) | 应用、店铺授权和授权生命周期 |
| [product-sync.md](product-sync.md) | 商品、规格和渠道库存同步 |
| [order-sync.md](order-sync.md) | 订单列表、详情、幂等和增量同步 |
| [after-sale-sync.md](after-sale-sync.md) | 售后订单、退款和人工复核 |
| [webhook.md](webhook.md) | 商品和订单推送回调 |
| [field-mapping.md](field-mapping.md) | 外部字段到内部领域字段的映射 |

这些文件是项目内部的待实现说明，不复制官方接口全文，也不能替代本次开发前
对在线目标接口 Markdown 文档的核对。

## 2026-07-23 首期只读核对

本次已打开并核对：

- 接入说明和代码示例。
- 查询闲鱼店铺。
- 查询商品类目、属性、列表、详情和规格。
- 查询订单列表和订单详情。
- 售后订单列表和售后订单详情。
- 订单推送通知。
- 查询快递公司。

当前结论：

- 生产地址为 `https://open.goofish.pro`。
- API 使用 JSON `POST`，查询参数包含 `appid`、秒级 `timestamp` 和 `sign`。
- 签名绑定最终发送的原始 JSON 字符串。
- 订单金额字段明确以分为单位。
- 商品和订单列表只支持最近六个月的 `update_time` 范围，且单个查询条件最多
  翻取一万条，必须按固定时间窗口切片同步。
- 售后接口当前 OpenAPI 存在字段类型和金额单位不够明确的问题，首期保留原始
  JSON 并采用宽容解析，不静默猜测。
- 2026-07-23 已使用进程级运行时凭据执行一次 `POST
  /api/open/user/authorize/list` 只读授权店铺探测：HTTP `200`、业务码
  `0`、授权店铺数量 `1`。请求没有执行写操作，未持久化或输出原始响应、店铺
  字段、签名或凭据。

## 2026-07-24 交付前复核

本次重新读取在线 `llms.txt`、接入说明、订单列表和订单详情文档，确认：

- 订单列表仍为 `POST /api/open/order/list`，`update_time` 只允许最近六个月，
  单次查询条件最多翻取一万条，`page_no` 和 `page_size` 上限均为 `100`。
- 订单详情仍为 `POST /api/open/order/detail`，请求体使用字符串
  `order_no`；`pay_amount`、`refund_amount` 等金额字段以分为单位。
- 签名仍绑定最终发送的 JSON 原文，时间戳单位为秒且五分钟内有效。

本次只核对公开文档，没有使用运行时凭据调用真实接口，也没有执行任何第三方
写操作。

## 2026-07-25 配置复核

本次重新读取在线 `llms.txt`，确认索引仍包含店铺、商品、订单、售后、推送和
快递公司查询等接口。当前变更仅涉及本地运行配置和验证材料，未新增或修改
闲管家请求字段、签名、分页或响应解析逻辑，因此没有打开新的目标接口文档。

本次没有使用运行时凭据调用真实接口，也没有执行任何第三方写操作。

## 2026-07-25 安全重放复核

本次继续重新读取在线 `llms.txt`，确认索引仍包含订单推送和商品推送。当前
变更只增加本地持久化订单推送事件和订单详情原始载荷的后台手动重放入口：
它复用已保存的本地原始推送或订单详情载荷，重新排队或重建本地订单详情，
不构造新的闲管家请求字段，不推进订单分页游标，也不执行商品、发货、改价、
退款或其他第三方写操作。

本次没有使用运行时凭据调用真实接口，也没有执行任何第三方写操作。

## 2026-07-25 保证金健康复核

本次重新读取在线 `llms.txt`，并打开当前“查询闲鱼店铺”接口 Markdown：

```text
POST /api/open/user/authorize/list
```

确认店铺授权响应仍包含 `is_deposit_enough`、`service_support`、`is_valid` 和
`valid_end_time` 等字段。本次实现仅消费 `is_deposit_enough` 做本地店铺健康
状态和后台去重告警：`true` 记录为 `HEALTHY`，`false` 记录为
`DEPOSIT_INSUFFICIENT`，缺失或不可识别值记录为 `UNKNOWN`。

本次没有使用运行时凭据调用真实接口，也没有执行任何第三方写操作。

## 2026-07-25 订单列表页重放复核

本次重新读取在线 `llms.txt`，并打开当前订单列表和订单详情接口 Markdown：

```text
POST /api/open/order/list
POST /api/open/order/detail
```

确认订单列表仍使用 `authorize_id`、`update_time`、`page_no` 和 `page_size`，
并返回 `list`、`count`、`page_no` 和 `page_size`；订单详情仍以 `order_no`
查询。本次实现只增加本地 `ORDER_PAGE` 原始页持久化和后台安全重放：重放会用
本地已保存列表页判断哪些订单详情缺失或过期，并按订单详情只读接口补拉详情；
不会推进分页 cursor，也不会执行商品、发货、改价、退款或其他第三方写操作。

本次没有使用运行时凭据调用真实接口，也没有执行任何第三方写操作。

## 2026-07-25 商品推送与详情复核

本次重新读取在线 `llms.txt`，并打开当前商品推送和商品详情接口 Markdown：

```text
商品推送通知：商家配置的 POST 回调 URL
POST /api/open/product/detail
```

确认商品推送请求包含 `seller_id`、`product_id`、`product_status`、
`publish_status`、`item_biz_type`、`price`、`stock`、`user_name` 和
`modify_time`；商品详情请求体使用 `product_id`，响应 `data` 包含
`product_id`、`product_status`、`channel_cat_id`、`title`、`price`、
`stock`、`publish_status` 和 `update_time` 等字段。本次实现只增加本地
`PRODUCT_PUSH` 脱敏事件登记、后台安全重放，以及通过商品详情只读接口补拉并
upsert `xianyu_product`；不会执行商品创建、编辑、库存修改、上下架、删除或其他
第三方写操作。

本次没有使用运行时凭据调用真实接口，也没有执行任何第三方写操作。

## 2026-07-25 商品列表与规格增量复核

本次重新读取在线 `llms.txt`，并打开当前商品列表和商品规格接口 Markdown：

```text
POST /api/open/product/list
POST /api/open/product/sku/list
```

确认商品列表仍使用 `update_time`、`page_no` 和 `page_size`，只允许最近六个月
更新范围，`page_no` 和 `page_size` 均最大 `100`，且单个查询条件最多翻取
一万条；列表只返回基础信息，详情仍需通过商品详情接口补拉。商品规格接口请求体
使用数组字段 `product_id`，一次最多 `100` 个商品 ID，且仅多规格商品适用。

本次实现只增加本地 `PRODUCT_PAGE` 原始页持久化、固定窗口分页编排、缺失或过期
商品详情补拉、多规格 SKU 分块补拉、独立 `PRODUCT` cursor 推进，以及
`xianyuProductSyncJob` / `XGJ_JOB_PRODUCT_CRON` 定时配置。商品请求可携带授权店铺
保存的 `seller_id` 查询参数以降低多店铺串店风险；不会执行商品创建、编辑、
库存修改、上下架、删除或其他第三方写操作。

本次没有使用运行时凭据调用真实接口，也没有执行任何第三方写操作。

## 2026-07-25 快递公司查询证据复核

本次重新读取在线 `llms.txt`，并打开当前快递公司查询接口 Markdown：

```text
POST /api/open/express/companies
```

确认接口仍返回 `data.list[]`，列表项包含 `code`、`express_name`、
`express_alias` 和 `is_hot`。本次实现保留后台实时查询行为，同时将完整响应
以 `EXPRESS_COMPANIES` 写入受限原始载荷表，便于审计接口字典变化和真实数据
回放分析；不会执行订单发货或其他第三方写操作。

本次没有使用运行时凭据调用真实接口，也没有执行任何第三方写操作。

## 风险边界

默认只读。以下操作需要用户明确授权，并且默认只允许测试店铺：

```text
创建 / 编辑商品
修改库存
上架 / 下架商品
删除商品
订单发货
订单改价
同意 / 拒绝退款
```

接口原始数据应保留在受控存储中，日志和导出必须脱敏。
