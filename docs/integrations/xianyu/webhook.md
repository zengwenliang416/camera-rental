# 闲管家推送回调

## 接收范围

推送由后端接收，不能由 Web 或 uni-app 客户端接收。当前文档覆盖：

- 商品回调通知。
- 商品推送通知。
- 订单推送通知。

订单推送接口已于 `2026-07-23` 根据官方 Markdown 核对。商品推送接口已于
`2026-07-25` 根据官方 Markdown 核对。

## 订单推送当前契约

- URL：由商家在闲管家开放平台配置，文档中的 `/order/receive` 只是示例。
- 方法：`POST`，JSON body。
- 查询参数：`appid`、秒级 `timestamp`、`sign`。
- 触发：订单信息、订单状态或退款状态发生变化。
- 请求超时：三秒。
- 推送失败：平台最多重试三次。
- 成功判定：HTTP 响应 JSON 的 `result` 必须严格为 `success`。

成功响应：

```json
{"result":"success","msg":"接收成功"}
```

推送 body 当前包含 `order_no`、`order_status`、`refund_status`、
`modify_time`、`seller_id`、`user_name`、`order_type`、`product_id` 和
`item_id`。推送只用于触发和去重，完整业务字段通过订单详情补拉。

## 商品推送当前契约

- URL：由商家在闲管家开放平台配置，文档中的 `/product/receive` 只是示例。
- 方法：`POST`，JSON body。
- 查询参数：`appid`、秒级 `timestamp`、`sign`。
- 成功判定：HTTP 响应 JSON 的 `result` 必须严格为 `success`。
- 当前后端实际接收地址：`POST /xianyu/webhooks/product`。

推送 body 当前包含 `seller_id`、`product_id`、`product_status`、
`publish_status`、`item_biz_type`、`price`、`stock`、`user_name` 和
`modify_time`。推送只用于触发和去重，完整商品字段通过商品详情
`POST /api/open/product/detail` 只读补拉；不会触发商品创建、编辑、库存修改、
上下架或删除。

## 处理流程

```text
接收请求
  ↓
校验签名 / 身份
  ↓
严格校验官方字段类型
  ↓
保存脱敏原始请求和幂等事件
  ↓
快速返回平台要求的响应
  ↓
异步拉取详情并处理业务
  ↓
记录成功、失败和有界重试信息
```

回调线程不得执行耗时的订单转换、排期计算或完整详情同步。

在三秒预算内，接收器只执行签名校验、最小字段校验、幂等落库和任务登记；
事务成功后立即返回 `success`。若本地持久化失败，应返回 `fail`，让平台重试，
不能先返回成功再丢弃事件。

匿名入口在绑定 JSON 前按 UTF-8 原始字节有界读取，最大 `65536` 字节。
`Content-Length` 超限和分块请求实际读取超限都会直接返回 `fail`，避免先把任意
大请求体完整分配到内存。

当前后端实际接收地址：

```text
POST /xianyu/webhooks/order
POST /xianyu/webhooks/product
```

事件状态：

```text
RECEIVED -> PROCESSING -> SUCCEEDED
                         -> FAILED
```

详情补拉通过事务提交后的 Spring 异步事件执行。订单推送补拉订单详情，商品
推送补拉商品详情。`FAILED` 和陈旧 `RECEIVED` 事件由 `xianyuPushRetryJob` 使用芋道
`infra_job + Quartz` 有界恢复，默认每五分钟扫描一次，单次最多一百条，且只处理
至少两分钟未更新的 `RECEIVED/FAILED` 事件。

当签名有效但 `seller_id` 暂时无法唯一映射到有效授权店铺时，事件仍以
`SHOP_MAPPING_UNAVAILABLE` 安全持久化并返回 `success`。授权店铺同步修复映射后，
重试任务会重新补拉详情。若同一 `seller_id` 对应多个授权，系统只在已同步订单
能唯一证明所属店铺时恢复；否则继续保留失败状态，不静默猜测店铺。

## 安全要求

- 生产和测试回调地址隔离。
- 回调地址使用 HTTPS。
- 校验失败的请求不得进入业务处理。
- 重复推送不能重复创建订单。
- 原始请求保存时脱敏，完整敏感内容不得进入普通日志。
- 回调失败必须可人工重放。
- 自动重试不得重放 `SUCCEEDED/PROCESSING` 事件，也不得跨租户扫描。
- 没有处理所有权 token 前，不自动抢占陈旧 `PROCESSING`；该状态需要运行态告警
  和人工恢复，避免旧、新消费者互相覆盖最终状态。

## 当前核对状态

| 项目 | 状态 |
| --- | --- |
| 回调 URL 和 HTTP 方法 | 商家配置 HTTPS URL，`POST` |
| 签名 / 身份校验字段 | `appid`、秒级 `timestamp`、`sign`，按原始 body 验签 |
| 平台成功响应格式 | `{"result":"success","msg":"..."}` |
| 推送重试行为 | 失败最多重试三次，单次超时三秒 |
| 后端接收入口 | `POST /xianyu/webhooks/order`、`POST /xianyu/webhooks/product` |
| 幂等落库 | `xianyu_raw_payload` + `xianyu_push_event` |
| 详情处理 | 提交后异步补拉 `/api/open/order/detail` 或 `/api/open/product/detail` |
| 本地失败恢复 | `xianyuPushRetryJob`，默认每五分钟有界扫描 |

## 测试

- 签名失败。
- 重复推送。
- 无法解析的原始请求。
- 快速响应与异步处理分离。
- 持久化失败返回 `fail`。
- 三秒响应预算。
- 失败自动重试和人工重放。
