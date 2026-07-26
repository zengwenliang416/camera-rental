# 0002 — 租赁设备与 ERP 进销存 / 出库回仓边界

## 状态

Accepted（2026-07-25）

## 背景

相机租赁需要「进货—在库—出库—回仓—检测」闭环。上游芋道提供 ERP（采购/数量库存）与 WMS（仓单），
但租赁的权威单位是**设备实例**（`rental_device`），不是 SKU 数量。

## 决策

1. **出库 / 回仓 / 检测** 以 `yudao-module-rental` 为权威：
   - 扫码认机 → 设备状态流转
   - 占用排期（`rental_schedule`）决定能否再租
   - 不把「在租台数」写成 ERP 库存减 1 作为唯一真相

2. **进销存（采购、供应商、型号级库存概览）** 启用 `yudao-module-erp`：
   - 采购入库确认后，运营在租赁侧**创建设备实例**（或后续做一键生成）
   - ERP 记录「进了几台 A7M4」；租赁记录「A7M4-0001 这台在哪」

3. **WMS** 本阶段不强制启用。仓位字段用 `rental_device.warehouse_code` 即可；
   若日后要多仓位拣货单，再评估 WMS 映射。

## 设备状态（出库回仓）

```text
AVAILABLE  --分配--> 仍为 AVAILABLE（已绑单未出库）
AVAILABLE  --出库--> RENTED
RENTED     --回仓通过--> AVAILABLE
RENTED     --回仓不通过--> MAINTENANCE
```

分配状态：`ASSIGNED` → `DISPATCHED` → `RETURNED`。

## 后果

- 侧栏 ERP 菜单在启用依赖后可用；租赁「出库/回仓」走 `/admin-api/rental/device/**`。
- 禁止用 ERP 数量库存替代设备排期冲突检查。

## 采购入库 → 设备实例

- 入口：ERP 采购入库列表「生成设备」（仅已审批 status=20）。
- API：`POST /admin-api/rental/device/generate-from-purchase-in`。
- 规则：入库数量取整台；型号优先产品条码；设备号 `条码-0001` 递增；`source_type=ERP_PURCHASE_IN` 幂等。
