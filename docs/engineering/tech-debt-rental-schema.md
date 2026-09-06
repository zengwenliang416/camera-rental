# 技术债登记：rental 域数据库设计（2026-09-06 评审）

来源：对 `camera-rental-server/sql/mysql/migrations/` 全部迁移叠加后生效 schema 的评审。
本文件只做记录与优先级建议，**不代表已授权修复**；修复需单独变更并按
[验证规则](validation.md) 执行。迁移执行机制见 `ops/github-deploy/apply-migrations.sh`
（`migrations.txt` 显式登记、文件名+sha256 记账、失败中止发布）。

## P0 需要立即核实

- [ ] `20260904_055_rental_pending_plan_dispatch.sql` 未登记进 `ops/github-deploy/migrations.txt`
  （该文件将 `rental_device_assignment.schedule_id` 改可空）。需核实生产库是否已执行
  （`SELECT ... FROM camera_rental_schema_migration WHERE migration_id LIKE '20260904%'`），
  以及当前代码是否已依赖该改动；未执行则下次发布前必须补登记或手工对齐。

## P1 正确性风险

- [ ] 设备占用防重叠无 DB 级保障：`rental_schedule` 只有幂等键与查询索引
  （`20260723_001:349-352`），防重叠全靠"先锁 device 行再事务内检查"的应用层约定
  （`RentalDeviceAssignmentServiceImpl.java:99-105`）。MySQL 无范围排他约束；
  **约定：任何写 `rental_schedule` 的新路径必须走统一 Service，禁止直写**。
- [ ] 可空列参与唯一键导致约束失效：`uk_rental_order_source`（source_order_id 可空，
  `20260723_001:269,286`）、`rental_schedule.idempotency_key`、
  `rental_device_assignment.schedule_id`（055 改可空后）。OFFLINE 订单 source_order_id
  为空即属此类，幂等只能靠 `uk_rental_order_order_no`。
- [ ] 待分配列表查询与索引错位：查询按 `status + preparation_status` 双等值过滤并按
  `create_time DESC` 排序（`RentalScheduleAllocationMapper.java:29-30,69`），现有索引各管一半
  （`20260723_001:287`、`20260831_052:145-148`），需复合索引。
- [ ] 列宽不匹配静默截断：`rental_channel_reconciliation_run.xianyu_item_id varchar(64)`
  （`20260901_056`）vs `xianyu_order.xianyu_item_id varchar(128)`（`20260831_052:29-30`）。
- [ ] `rental_order.status DEFAULT 'REVIEW_REQUIRED'`（`20260723_001:271`）从未被使用，
  唯一写入路径显式写 `PENDING_ALLOCATION`；任何新写入路径必须显式赋 status，漏传即埋雷。
- [ ] 迁移编号 055 重复使用（`20260901_055` 与 `20260904_055`）。执行器按文件路径记账故
  无害，但新迁移编号前必须 `ls` 确认最大值。

## P2 数据安全与一致性

- [ ] 客户 PII 明文：`xianyu_order.receiver_name/mobile/address` 明文（`20260728_026`），
  未应用项目既有的 EncryptTypeHandler 惯例（对比 `rental_delivery.tracking_phone`）。
  修复需双读过渡 + 存量回填 + 验证后切换，属独立合规项目。
- [ ] 金额类型残留 int：`xianyu_after_sale.refund_amount`（`20260723_001:140`）、
  `rental_device.purchase_amount`（`20260723_001:320`）未随 002/003 加宽 bigint，
  Java 侧 Integer→Long 连带改动需逐处核对。
- [ ] `rental_return_registration.tenant_id` NOT NULL 无 `DEFAULT 0`（`20260801_036`），
  与全库约定不一致；同表 creator/updater 默认值写法偏离 yudao 基线。
- [ ] `xianyu_application.app_key` 全局唯一且可空（`20260729_029`）：跨租户互堵 + NULL 放行，
  约束既过强又过弱。

## P3 演进性债（记账，不单独修复，随相关改动顺势收敛）

- 全库唯一键均不含 `deleted`：逻辑删除行永久占用编号（设备号/序列号/外部订单号/订单号）。
  **约定：业务编号删除后不复用**；正确修复需引入删除令牌列，blast radius 全库，不划算。
- 型号引用风格分裂：`equipment_model_code` 字符串（device/order_item/schedule）vs
  `device_model_id` bigint（`rental_channel_product_rule/sku_mapping`，`20260831_052`）。
- 两套商品→型号映射并存：`xianyu_product_mapping`（001 旧）与
  `rental_channel_product_rule + sku_mapping`（052 新），schema 层面无法分辨活跃路径。
- `xianyu_order` 索引冗余：14 个二级索引中 `(tenant_id,id)` 被主键覆盖、两个 updated
  索引左前缀相同、report_product 仍索引已废弃的 external_product_id。
- 同一事实三副本：`xianyu_order` 列值 + `goods_json` + `detail_json`（006）。
- 布尔类型不统一：`xianyu_order.is_tax_included` tinyint（006）vs 全库 bit(1)。
- `rental_delivery` 双业务唯一键割裂（`rental_order_id` 可空后旧键失效、新键兜底，
  `20260731_032:49-52` + `20260801_034`）；`rental_delivery_device_rel` 订单列 NOT NULL
  与主表可空不一致，"先物流后转换"的包裹无法提前绑设备关系。

## 修复策略（评审结论）

- 借"线下录单"变更顺手做：待分配复合索引、录单显式写 status、新客户表加密存储。
- 卫生冲刺（独立小变更）：编号规范化、列宽对齐、金额加宽、索引瘦身。
- 独立项目：PII 存量加密（双读 + 回填 + 回滚预案）。
- 不做 schema 修复、用约定/评审红线覆盖：唯一键含 deleted、占用防重叠。
