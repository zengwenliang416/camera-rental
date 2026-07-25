# 闲管家认证与签名

## 文档状态

已于 `2026-07-23` 重新读取官方 `llms.txt`、接入说明、代码示例和首期只读
目标接口 Markdown。本文记录当前认证契约；实现前仍需再次核对在线文档。

## 官方来源

1. 重新读取官方 `llms.txt`。
2. 定位接入说明、代码示例和应用接入教程。
3. 打开目标接口文档，记录版本和访问日期。

官方索引：<https://s.apifox.cn/3ac13d69-5a38-4536-ae9b-a54001854ef8/llms.txt>

## 配置边界

```yaml
rental:
  xianyu:
    enabled: false
    base-url: https://open.goofish.pro
    app-key: ${XGJ_APP_KEY:}
    app-secret: ${XGJ_APP_SECRET:}
    webhook-base-url: ${XGJ_WEBHOOK_BASE_URL:}
```

- 示例只允许出现空占位符。
- `AppSecret` 只能在后端运行时注入。
- 一个应用可以授权多个店铺，不能把店铺写死在应用配置中。
- 管理端响应不得返回 `AppSecret`。

## 当前请求契约

生产基础地址：

```text
https://open.goofish.pro
```

当前目标接口统一使用：

```text
POST {baseUrl}{path}?appid={appKey}&timestamp={epochSeconds}&sign={sign}
Content-Type: application/json
```

- `appid` 传开放平台提供的 AppKey。
- AppKey 在配置和 URL 构造中按字符串处理，不做数值运算。
- `timestamp` 使用 Unix 秒；目标接口说明其五分钟内有效。
- 即使 OpenAPI 将 `timestamp` 标为非必填，签名规则和示例都依赖该值，客户端
  必须始终发送。
- 请求体必须是 JSON 原文。无业务参数的接口发送并签名同一个空对象字符串
  `{}`，不能签名 `{}` 后发送空 body。
- 所有字段强校验，必须按官方字段类型传参。
- 外部订单号、退款单号以及返回前端的其他长标识统一按字符串处理，避免
  JavaScript 安全整数精度丢失。

## 签名算法

自研 / 第三方 ERP 模式：

```text
bodyMd5 = md5Hex(utf8(bodyString))
signSource = appKey + "," + bodyMd5 + "," + timestamp + "," + appSecret
sign = md5Hex(utf8(signSource))
```

商务对接模式会在时间戳和密钥之间增加 `sellerId`：

```text
appKey + "," + bodyMd5 + "," + timestamp + "," + sellerId + "," + appSecret
```

本项目默认按自研应用模式设计。若实际账号属于商务对接，必须单独确认后再启用
`seller_id`，不能自动猜测模式。

## 客户端职责

建议由统一的 `XgjClient` 负责：

- 使用统一 JSON 配置完成一次最终序列化，保存同一个 `bodyString`。
- 按官方规则生成认证信息。
- 使用签名时相同的 UTF-8 字节发送请求体，发送前不得格式化或重新序列化。
- 设置连接、读取和整体超时。
- 将网络、认证、限流、参数和服务端错误转换为内部异常。
- 对允许重试的临时失败执行有限次退避。

签名完成后不得再次修改或重新序列化参与签名的请求体。

## 当前核对结果

| 项目 | 官方文档依据 | 状态 |
| --- | --- | --- |
| 认证字段 | 接入说明 / 目标接口 Markdown | `appid`、`timestamp`、`sign` 查询参数 |
| 请求头 | 代码示例 / 目标接口 Markdown | `Content-Type: application/json` |
| 签名原文和算法 | 接入说明 / 代码示例 | 两次 MD5，绑定原始 JSON 字符串 |
| 时间戳单位和容差 | 目标接口 Markdown | Unix 秒，五分钟内有效 |
| 认证错误码 | 目标接口 Markdown | 未提供完整枚举，保留原始 `code`、脱敏 `msg` |
| 限流和通用重试限制 | 目标接口 Markdown | 未给出统一规则，不对未知错误盲目重试 |

## 测试

- 签名固定向量测试。
- `{}` 空请求体固定向量测试。
- 含中文、空字符串、数组和不同字段顺序的原始 body 一致性测试。
- JSON 序列化稳定性测试。
- 秒 / 毫秒混淆和五分钟边界测试。
- 认证失败不重试测试。
- 限流和临时错误的有限重试测试。
- 日志不包含密钥、签名和完整请求头测试。
