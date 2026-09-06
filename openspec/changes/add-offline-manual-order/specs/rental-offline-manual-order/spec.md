# Spec: rental-offline-manual-order

## ADDED Requirements

### Requirement: 管理端手动创建线下租赁订单

系统 SHALL 提供 `POST /admin-api/rental/order/create-manual`（权限
`rental:order:create`），入参包含客户信息（姓名、手机号、微信号可空）、订单明细
（设备型号、数量、租金）、租期起止（闭区间）、押金、配送方式与配送信息。

- 创建的订单 MUST 落库为 `source_type='OFFLINE'`、`status='PENDING_ALLOCATION'`、
  `preparation_status='READY'`，从而直接出现在既有待分配列表。
- 订单号 MUST 为 `OFF-` + 19 位零填充自增 id；金额 MUST 以整数分存储；占用区间
  MUST 按半开区间存储（`occupy_end_date_exclusive = 结束日期 + 1`）。
- 设备型号 MUST 在租赁设备型号目录中存在且启用；租期 MUST 满足 start <= end 且
  不早于当天（Asia/Shanghai）；金额与押金 MUST NOT 为负。

#### Scenario: 录单后进入待分配

- GIVEN 运营提交合法录单表单 WHEN 创建成功 THEN 订单可在待分配列表出现，并可用
  既有 `/rental/device/assign` 完成设备分配。

#### Scenario: 非法输入拒绝

- WHEN 型号不存在、租期倒置、金额为负或配送必填项缺失 THEN 返回 1-040-005 段
  业务错误码，不产生任何落库记录。

### Requirement: 客户主档与反查

系统 SHALL 维护 `rental_customer` 主档（姓名、加密手机号、微信号、备注）。录单时
MUST 先按完整手机号等值反查：命中则复用并在有变更时更新姓名/微信号，未命中则
新建。系统 SHALL 提供 `GET /admin-api/rental/customer/suggest?mobile=` 供录单页
自动带出老客户信息。

#### Scenario: 老客户复购

- GIVEN 手机号已存在于主档 WHEN 再次录单 THEN 不新增客户行，订单关联既有客户。

### Requirement: 订单配送信息与无运单出库确认

系统 SHALL 以 `rental_order_delivery`（1:0..1）记录配送方式
（EXPRESS/ERRAND/SELF_DELIVERY）、加密收货人姓名/电话/地址与配送备注；
ERRAND/SELF_DELIVERY 时收货信息 MUST 必填。

ERRAND/SELF_DELIVERY 的 OFFLINE 订单 SHALL 在既有分配入口（`assign` /
`assignPendingPlan`）新建分配落库后同事务直接完成出库：assignment 推进为
DISPATCHED、设备推进为 RENTED，复用仓库出库同一写路径；无配送记录（渠道订单）或
EXPRESS 时 MUST 保持分配后不出库。

系统 SHALL 提供 `POST /admin-api/rental/order/confirm-outbound` 作为兜底：仅当订单
delivery_method 非 EXPRESS 且设备已分满时，推进仍为 ASSIGNED 的分配；重复调用
MUST 幂等；MUST NOT 写 `rental_delivery` 或 `rental_device_shipment`。

#### Scenario: 跑腿订单分配即送出

- GIVEN OFFLINE 订单配送方式为 ERRAND WHEN 运营在分配入口选好设备 THEN 该分配
  同事务推进为 DISPATCHED、设备为 RENTED，且不产生任何快递运单记录。

#### Scenario: 兜底确认补送

- GIVEN OFFLINE 跑腿订单存在仍为 ASSIGNED 的历史分配 WHEN 调用确认 THEN 仅这些
  分配被推进，已 DISPATCHED 的分配不受影响。

#### Scenario: 快递方式拒绝确认

- GIVEN 订单 delivery_method=EXPRESS WHEN 调用确认 THEN 返回业务错误，提示走线下
  快递流程。

### Requirement: 报表来源修正

租赁报表 SQL MUST 使用 `rental_order.source_type` 实际值，MUST NOT 硬编码
`'XIANYU'`。

#### Scenario: 线下订单计入报表

- GIVEN 存在 OFFLINE 订单 WHEN 查询报表 THEN 其来源显示为 OFFLINE 而非 XIANYU。
