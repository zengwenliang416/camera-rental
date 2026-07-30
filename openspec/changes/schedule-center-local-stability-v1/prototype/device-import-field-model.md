# 设备批量入库字段模型

## 数据口径

- 线上 `rental_device` 当前只有 4 台已建档设备，不代表实际资产总量。
- 排期、分配、出库、回仓和二维码只能对已建档的独立设备实例生效。
- 原型中的库存数字统一标记为“系统已建档”，实际几百台资产在完成导入前不展示虚构总数。

## 当前后端支持字段

| 中文字段 | API / 数据库字段 | 规则 | 用途 |
|---|---|---|---|
| 设备编号 | `deviceNo` / `device_no` | 必填；`tenant_id + device_no` 唯一 | 稳定资产身份、二维码和操作主键 |
| 机身序列号 | `serialNumber` / `serial_number` | 选填；有值时 `tenant_id + serial_number` 唯一 | 厂家 SN、扫码与去重 |
| 型号代码 | `equipmentModelCode` / `equipment_model_code` | 必填；需先统一字典 | 型号分组、订单映射和可用性 |
| 设备状态 | `status` | `AVAILABLE`、`RENTED`、`MAINTENANCE` | 分配、出库、回仓后的权威状态 |
| 仓库或库位 | `warehouseCode` / `warehouse_code` | 选填；建议导入时必填 | 当前阶段承载仓库和库位 |
| 采购金额 | `purchaseAmount` / `purchase_amount` | 选填；整数分 | 设备成本与收益统计 |
| 是否启用 | `enabled` | 默认 `true` | 停用设备不可参与分配 |
| 来源类型 | `sourceType` / `source_type` | 系统字段 | 例如 `ERP_PURCHASE_IN` |
| 来源业务 ID | `sourceBizId` / `source_biz_id` | 系统字段 | 关联采购入库单 |
| 来源明细 ID | `sourceItemId` / `source_item_id` | 系统字段 | 关联采购入库明细并保证幂等 |

## 建议导入模板

首批导入文件建议只包含当前后端能够可靠接收的字段：

```text
device_no,serial_number,equipment_model_code,warehouse_code,status,purchase_amount_fen,enabled
```

规则：

1. 先整理型号字典，再导入设备实例；同一型号只能使用一个标准代码。
2. 保留现有设备编号，不批量改号；新设备编号使用统一且稳定的编号规则。
3. 有厂家 SN 的设备必须填写，空 SN 仅用于确实没有可用序列号的设备。
4. `warehouse_code` 先统一为编码，不在同一列混用仓库名称、货架描述和自由文本。
5. 初始状态通常为 `AVAILABLE`；已出租设备必须同时补录对应订单、分配和排期，不能只把状态改为 `RENTED`。
6. 采购金额以分填写，例如 `129900` 表示 `1299.00` 元。
7. 导入前做设备编号、SN、型号代码和状态枚举校验；重复记录进入人工复核，不覆盖线上数据。

## 后续扩展字段

品牌、型号展示名、品类、采购日期、供应商、质保截止日、成色、附件清单、精细库位、图片和备注目前不属于
`rental_device` 的正式接口字段。若业务需要，应先建立型号主数据和增量数据库方案，不能临时塞进
`warehouseCode`、`deviceNo` 或备注字符串。
