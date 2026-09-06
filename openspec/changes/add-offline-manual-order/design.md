# Design: add-offline-manual-order

## 关键决策

### D1 表结构克制拆分，不向 rental_order 堆渠道特定列

`rental_order` 已被 ALTER 五次、约 20 个业务列。客户/配送信息对全部存量闲鱼订单
永远为 NULL，加在订单表上是渠道包袱。因此：

- `rental_order` 只加 `customer_id`（指针）与 `deposit_amount`（订单金额属性）。
- 客户主档独立成表 `rental_customer`：线下业务以"微信老客户复购"为常态，主档
  支持手机号反查自动带出，客户信息有唯一权威实现。
- 订单配送独立成表 `rental_order_delivery`（1:0..1）：配送是履约关注点，收货人
  可能≠下单客户；未来线下快递单也写此表，不动快递专用的 `rental_delivery`
  （waybill_no NOT NULL、挂物流订阅/Outbox）。

### D2 订单号与幂等

- `orderNo = 'OFF-' + 19 位零填充自增 id`，对齐既有 `XY-%019d` 风格；事务内插入
  后按回填 id 更新。
- `source_order_id` 留空：`uk_rental_order_source` 对 NULL 放行（已知行为，见
  `docs/engineering/tech-debt-rental-schema.md`），幂等由 `uk_rental_order_order_no`
  与服务层校验兜底。

### D3 直接置 READY 进入待分配

线下订单无渠道对账，创建即 `status=PENDING_ALLOCATION` +
`preparationStatus=READY`，流入既有待分配→分配→履约链路，分配/排期零改动。
实现时必须核实对账 job / `RentalFulfillmentUpdateGuard` 不会把 OFFLINE 订单的
preparationStatus 回退；若会，则其查询条件加 `source_type='XIANYU'` 过滤（属受控
修改，diff review 单独说明）。

### D4 日期与金额契约

- 显示闭区间 `[start, end]`；存储 `occupy_start_date=start`、
  `occupy_end_date_exclusive=end+1`、`expected_send_back_date=end`。
- 金额整数分（Long）；前端仅做元↔分单位换算与展示。
- 业务日期按 Asia/Shanghai。

### D5 敏感信息

手机号/地址列使用既有 `EncryptTypeHandler`（参照 `RentalDeliveryDO.trackingPhone`）；
加密列只支持等值匹配，客户反查按完整手机号精确匹配；管理端展示遵循既有看板
脱敏惯例。

### D6 出库确认的边界

- 分配复用 `POST /admin-api/rental/device/assign`（渠道中立，已验证）。
- `confirm-outbound` 仅面向 `delivery_method != EXPRESS` 的订单，校验设备已分满，
  写履约状态；**不写** `rental_delivery`/`rental_device_shipment`。
- EXPRESS 线下订单本期仅提示线下发货，不接 OCR/物流跟踪。

## 明确不做

归还登记挂非闲鱼订单、闲鱼订单回填 customer_id、线下快递 OCR、App 自助下单、
线下订单专页。
