# 闲鱼历史订单补建

## 目标与边界

历史订单补建用于重新执行已经落库的渠道订单转换与准备状态计算。它复用实时订单
同步使用的 `RentalChannelOrderReconciliationService`，不会创建第二套备注解析、
商品映射或履约更新规则。

补建仅处理本地数据库记录：

- 不调用闲管家商品、订单、发货、改价或退款写接口。
- 不删除渠道订单、内部订单、设备分配、排期或履约历史。
- 已有关联内部订单或履约事实的记录只会报告更新、未变化、冲突或待复核，不会逆转。
- 外部订单号、商品 ID 和 SKU ID 不作为分页游标；分页只使用当前租户内的
  `xianyu_order.id`。

## 上线前置条件

1. 先部署 `20260901_055_rental_historical_reconciliation.sql`。
2. 部署包含历史补建 Service 和管理 API 的后端版本。
3. 保持 `rental.historical-backfill.write-enabled=false`。
4. 可使用只读 SQL 预估本次任务的内部订单主键上界：

```sql
SELECT COALESCE(MAX(id), 0) AS end_id_inclusive
FROM xianyu_order
WHERE tenant_id = <tenant_id>
  AND deleted = b'0';
```

创建任务时，后端会按当前租户重新查询 `MAX(id)`，并将实际
`endIdInclusive` 固定为“请求上界”和“创建时当前最大主键”中的较小值。即使请求
传入更大的上界，之后新同步的更大主键订单也不会进入当前任务。若当前最大主键不大于
`startAfterId`，任务按空范围完成并返回零计数。

## 管理 API

接口统一位于：

```text
/admin-api/rental/configuration/historical-reconciliation
```

权限：

- 查询任务：`rental:configuration:query`
- 创建、暂停和恢复：`rental:configuration:update`

### Dry-run

先用 dry-run 预演一个固定范围：

```http
POST /admin-api/rental/configuration/historical-reconciliation/run
Content-Type: application/json

{
  "startAfterId": 0,
  "endIdInclusive": 100000,
  "batchSize": 100,
  "maxBatches": 10,
  "dryRun": true
}
```

dry-run 会调用真实 reconciliation 逻辑，但当前业务批次强制回滚；任务记录、游标和
计数在单独事务中保存。每次请求最多处理 `batchSize * maxBatches` 条记录，达到边界后
状态为 `PAUSED`，可以继续恢复。响应中的 `endIdInclusive` 是后端最终冻结的实际上界，
运营核对和后续恢复必须以该值为准。

若暂停请求恰好发生在 dry-run 业务事务回滚之后、checkpoint 保存之前，系统会先保存
该批 dry-run 的游标和计数，再将任务收敛为 `PAUSED` 并立即返回，不会继续启动下一批。

### 查询、暂停和恢复

```http
GET /admin-api/rental/configuration/historical-reconciliation/get?id=<run_id>
PUT /admin-api/rental/configuration/historical-reconciliation/pause?id=<run_id>
```

恢复 dry-run：

```http
PUT /admin-api/rental/configuration/historical-reconciliation/resume
Content-Type: application/json

{
  "runId": 1,
  "maxBatches": 10
}
```

状态含义：

- `READY`：任务已创建，尚未开始。
- `RUNNING`：当前请求正在执行批次。
- `PAUSE_REQUESTED`：暂停请求已记录，当前批次结束后暂停。
- `PAUSED`：已停在持久化游标，可恢复。
- `FAILED`：当前批次已回滚，失败记录已保存，可修复后恢复。
- `SUCCEEDED`：固定范围已完成，不允许再次恢复。

每次执行或恢复都会生成新的内部 UUID `execution_token`，并持有 5 分钟执行租约。
每批开始时更新 `heartbeat_at` 和 `lease_until`。有效租约内的 `RUNNING` 或
`PAUSE_REQUESTED` 任务不能被第二个请求接管；旧执行者的后续 checkpoint 也必须匹配
当前 token，否则会失去写入资格。只有租约已经过期的 `RUNNING` /
`PAUSE_REQUESTED` 任务才能通过显式 `resume` 获取新 token 并从最后提交游标继续。
`execution_token` 是服务端 fencing token，不需要也不会由运营人员在 API 中传入。

## 真实执行

真实执行需要独立的生产变更授权、备份和 dry-run 结果复核。批准后才可临时设置：

```yaml
rental:
  historical-backfill:
    write-enabled: true
```

请求必须同时使用固定确认串：

```text
EXECUTE_HISTORICAL_RECONCILIATION
```

```http
POST /admin-api/rental/configuration/historical-reconciliation/run
Content-Type: application/json

{
  "startAfterId": 0,
  "endIdInclusive": 100000,
  "batchSize": 100,
  "maxBatches": 1,
  "dryRun": false,
  "confirmation": "EXECUTE_HISTORICAL_RECONCILIATION"
}
```

真实执行应从 `maxBatches=1` 开始，核对一批后再恢复。执行完成或停止后应将
`rental.historical-backfill.write-enabled` 恢复为 `false`。

## 计数与失败恢复

任务返回并持久化：

- `scannedCount`
- `skippedCount`
- `createdCount`
- `updatedCount`
- `unchangedCount`
- `conflictCount`
- `failedCount`
- `reviewRequiredCount`

每个成功批次和 checkpoint 在同一事务提交。某条记录失败时，整个当前批次回滚，
之前已经提交的批次保留；游标停在失败批次之前。失败表只保存任务、订单主键、
批次前游标、尝试次数和安全错误码，不保存原始载荷或客户隐私。

`createdCount`、`updatedCount`、`unchangedCount`、`skippedCount` 和
`conflictCount` 使用中央 reconciliation 在订单锁内返回的 `mutationKind`
（`CREATED`、`UPDATED`、`UNCHANGED`、`SKIPPED`、`CONFLICT_REVIEW`）累加，
不会通过锁外前后快照猜测本次任务的变更类型。重复补建同一已收敛订单应计为
`UNCHANGED`，不会重复创建内部订单。

处理步骤：

1. 查询任务的 `lastFailedOrderId` 和 `lastErrorCode`。
2. 查询 `rental_historical_reconciliation_failure` 的对应失败边界。
3. 修复配置、备注或数据完整性问题，不能删除历史订单规避失败。
4. 使用同一个 `runId` 恢复，任务会从持久化游标重新执行失败批次。
5. 核对计数、人工复核记录、内部订单幂等和履约历史未被逆转。

## 回滚

优先回滚应用代码并保留任务和失败记录。055 的 rollback SQL 会删除两个历史补建表，
因此只适用于无保留要求的临时或预生产数据库。

生产环境不得直接运行 055 rollback。若确需移除表，必须先导出运行记录和失败记录，
完成数据保留评审并获得单独批准。已经由补建创建或更新的业务订单不能通过删除任务表
回滚，应按正常订单、配置和人工复核流程处理。
