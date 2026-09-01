# 闲鱼租赁配置发布手册

## 适用范围

本文用于发布 `add-rental-configuration` 的数据库、后端和管理端变更。它只准备
可复核的发布步骤，不构成生产授权。

禁止在未单独获批时执行：

- 连接或修改 80 服务器。
- 对生产数据库运行 migration、rollback 或 controlled seed。
- 开启真实历史补建。
- 调用闲管家商品、库存、发货、改价或退款写接口。
- 对真实店铺执行自动化写入。

## 发布前确认

1. 记录目标环境、租户 ID、数据库实例、后端版本和管理端版本。
2. 核对备份已完成且恢复流程已演练。
3. 确认 migrations `052` 至 `056` 的生产文件 SHA-256 与
   `development/migrations/manifest.json` 一致。
4. 确认所有启用店铺已有非空 `xianyu_user_name`，授权状态为 `VALID`，未过期。
5. 保持 `rental.historical-backfill.write-enabled=false`。
6. 确认不存在正在运行的历史补建或规则重评任务。
7. 核对 `rental:configuration:query` 和 `rental:configuration:update` 只授予
   预期管理员。
8. 完成本地 Maven、管理端构建和 SpecNav 验证；任何失败都必须保留为阻塞。

## 发布顺序

1. 在已备份的目标数据库依次执行：
   `20260831_052_rental_configuration_foundation.sql`、
   `20260831_053_rental_configuration_backend.sql`、
   `20260831_054_rental_fulfillment_facts.sql`、
   `20260901_055_rental_historical_reconciliation.sql`、
   `20260901_056_rental_channel_reconciliation_run.sql`。
2. 执行只读校验，确认列、索引、唯一约束和新增表存在，且没有重复活动规则。
3. 部署后端，先保持历史补建写开关关闭。
4. 重新同步授权店铺，再同步商品详情和商品规格，使店铺 item ID 与 SKU 标识完整。
5. 部署管理端，验证租赁配置菜单、权限、目录、商品规则和备注规范可读取。
6. 先为一个测试商品配置规则，核对影响预览和异步重评结果。
7. controlled seed 与真实历史补建仍保持禁用，等待独立生产授权。

## 只读校验

```sql
SELECT COUNT(*) AS enabled_shops_missing_user_name
FROM xianyu_shop
WHERE deleted = b'0'
  AND authorization_status = 'VALID'
  AND (xianyu_user_name IS NULL OR xianyu_user_name = '');

SELECT COUNT(*) AS unresolved_order_identifiers
FROM xianyu_order
WHERE deleted = b'0'
  AND (xgj_product_id IS NULL OR xianyu_item_id IS NULL);

SELECT tenant_id, shop_id, xianyu_item_id, COUNT(*) AS duplicate_count
FROM rental_channel_product_rule
WHERE deleted = b'0'
GROUP BY tenant_id, shop_id, xianyu_item_id
HAVING COUNT(*) > 1;

SELECT status, COUNT(*) AS run_count
FROM rental_channel_reconciliation_run
WHERE deleted = b'0'
GROUP BY status;

SELECT status, COUNT(*) AS run_count
FROM rental_historical_reconciliation_run
WHERE deleted = b'0'
GROUP BY status;
```

预期：

- `enabled_shops_missing_user_name = 0`。
- 不存在重复活动商品规则。
- 不存在长期停留在 `PENDING`、`RUNNING` 或 `PAUSE_REQUESTED` 的任务。
- `unresolved_order_identifiers` 只能作为待详情重同步清单，不能用旧字段回填。

## Controlled Seed

29 个 `CONFIG_SKIPPED` 商品的 seed 不是普通 migration。只有在以下条件全部满足时
才可单独申请执行：

- 已确认目标租户。
- “小疆”和“发发”分别精确匹配唯一、有效、未过期的店铺记录。
- 已备份 `rental_channel_product_rule`。
- 当前规则不存在冲突。
- 已设置固定确认串 `SEED_RENTAL_CONFIGURATION_SKIPPED_ITEMS`。

任一店铺零匹配、重复匹配、授权过期或规则冲突都必须停止，不能改成模糊匹配或
部分写入。

## 历史补建

1. 先创建固定范围 dry-run，使用后端返回的 `endIdInclusive` 作为真实边界。
2. 从小批次开始，例如 `batchSize=100`、`maxBatches=1`。
3. 核对扫描、跳过、创建、更新、未变化、冲突、失败和待复核计数。
4. 抽查普通订单、`CONFIG_SKIPPED`、已有内部订单和已有履约事实四类结果。
5. dry-run 通过后，另行申请真实写入授权。
6. 获批后才临时开启 `rental.historical-backfill.write-enabled=true`，并使用
   `EXECUTE_HISTORICAL_RECONCILIATION` 确认串。
7. 每批核对完成后再恢复下一批；停止后立即关闭写开关。

## 监控与停止条件

监控：

- 订单详情持久化失败和渠道同步错误码。
- 内部订单创建数量与唯一外部订单映射。
- `WAITING_MODEL`、`WAITING_REMARK`、`REVIEW_REQUIRED` 和 `READY` 分布。
- 规则重评任务的八类计数及 `last_error_code`。
- 历史补建的 checkpoint、lease、heartbeat、失败边界和人工复核数量。
- 分配/排期冲突、已履约事实保护和跨店/跨租户拒绝。

立即停止：

- 发现 `product_id` 与 `item_id` 或 SKU 标识发生回退/互填。
- 同一店铺和闲鱼商品出现重复活动规则。
- 同一渠道订单出现多个内部订单。
- 已分配、已出库、已回仓、已检测或已结算事实被自动覆盖。
- 重评或补建游标越过失败记录。
- 失败数持续增长、租约无法收敛或任务长期无 heartbeat。
- 发现客户隐私、凭据或原始载荷进入普通日志。

## 回滚

首选应用回滚并保留所有新增列、规则、备注历史、任务账本和失败证据。这样可以停止
新行为，同时保留审计和恢复能力。

生产环境不得直接运行仓库中的 destructive rollback：

- `052` 会移除明确标识、规则、SKU 映射、备注历史和准备状态。
- `053` 会移除目录乐观锁字段。
- `054` 会移除预计发回、检测和结算相关字段。
- `055` 会删除历史补建运行与失败证据。
- `056` 会删除异步规则重评结果。

如确需数据库回滚，必须先停止相关任务、导出新增数据、完成数据保留评审、验证备份
可恢复并获得单独批准。已经创建或更新的内部订单不能通过删除任务表或规则表回滚，
必须按正常订单、配置和人工复核流程处理。
