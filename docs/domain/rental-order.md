# 租赁订单领域

## 核心对象

| 对象 | 含义 |
|---|---|
| 商品 SKU | 可被客户选择的商品类型，例如“索尼 A7M4” |
| 设备实例 | 一台可追踪的具体设备，例如 `A7M4-0001` |
| 租赁订单 | 客户、渠道、租期和金额组成的业务订单 |
| 租赁明细 | 订单中的商品、数量、租期和计价信息 |
| 设备分配 | 将具体设备实例分配给租赁明细 |
| 渠道订单 | 闲鱼、Web、小程序等外部订单的映射 |

SKU 是商品销售和展示单位，设备实例是排期、出库、归还和维修单位。二者不能混为一个库存字段。

## 建议表

```text
rental_product_config
rental_device
rental_package
rental_package_item
rental_order
rental_order_item
rental_schedule
rental_device_assignment
rental_delivery
rental_inspection
rental_maintenance
rental_deposit
rental_channel_order
```

最终表结构必须遵循后端现有逻辑删除、租户字段、审计字段和数据库命名规范。新增表必须提供增量 SQL，不能修改已执行的历史迁移。

## 订单生命周期

订单状态应以明确的状态机实现，至少覆盖：

```text
待确认 -> 待支付（SKU 容量临时预留） -> 已支付（预留确认）
       -> 待分配 -> 已分配
       -> 待出库 -> 已出库 -> 租赁中 -> 待归还
       -> 待检测 -> 已完成
```

异常分支包括取消、支付失败、库存冲突、换机、提前归还、退款、损坏赔偿和人工复核。具体状态值在实现前应结合现有项目字典和支付模块确认。

订单创建时按 SKU、数量和设备占用半开区间预留容量，不立即绑定具体设备。
未支付预留必须过期释放；支付成功后确认预留。具体设备由运营人员在拣货前
选择或由员工扫码确认，并在同一事务中完成实例冲突复查、设备分配和排期写入。

## 金额规则

- 后端使用整数分保存金额。
- Java、JavaScript 和数据库结算不得使用浮点数。
- 闲鱼订单的 `pay_amount` 按当前接口文档和项目业务口径作为实付金额来源。
- 退款金额、押金退还和设备赔偿分别建模，不默认互相抵扣。
- 未能解析租期的订单仍计入收款统计，但不能伪装成已完成按服务日分摊。
- 每日分摊金额之和必须严格等于订单实付金额，分币余数放到最后一个计租日。

## 渠道幂等

渠道订单必须保存：

- 渠道标识
- 应用或店铺授权标识
- 外部订单号
- 外部状态
- 原始响应
- 标准化字段
- 最近同步时间
- 转换版本和错误信息

`channel + application_id + external_order_no` 应形成唯一业务约束。同步、推送重放和详情补拉不得重复创建租赁订单。

## 敏感信息

订单详情和日志应遵守最小披露原则。手机号、地址、身份证、支付凭证、AppSecret 和完整签名不得出现在普通看板、日志、测试快照或前端返回中。

## 订单来源

`rental_order.source_type` 区分来源：`XIANYU` 由闲鱼渠道订单转换产生，
`OFFLINE` 由管理端手动录单（`POST /admin-api/rental/order/create-manual`，权限
`rental:order:create`）产生。线下订单创建即 `PENDING_ALLOCATION` + `READY`，
订单号为 `OFF-` + 19 位零填充自增 id；客户主档存于 `rental_customer`（手机号
AES 加密、完整号码等值反查），配送信息存于 `rental_order_delivery`
（EXPRESS/ERRAND/SELF_DELIVERY，收货人手机号与地址加密）。跑腿/自送订单在分配
入口选好设备时即同事务完成出库（assignment 推进 DISPATCHED、设备推进 RENTED，
复用设备出库写路径）；`POST /admin-api/rental/order/confirm-outbound` 保留为兜底
入口，仅推进仍为 ASSIGNED 的分配且幂等。两条路径都不产生 `rental_delivery` /
`rental_device_shipment` 记录；快递配送的线下订单走线下快递流程。报表按
`source_type` 实际值统计来源。
