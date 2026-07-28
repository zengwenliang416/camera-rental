-- Persist seller-remark rental-period parsing on the channel order.
-- Existing rows are backfilled in bounded batches by xianyuOrderSyncJob.

ALTER TABLE `xianyu_order`
  ADD COLUMN `billable_start_date` date DEFAULT NULL COMMENT '计租开始日期(含)' AFTER `remark_parse_status`,
  ADD COLUMN `billable_end_date` date DEFAULT NULL COMMENT '计租结束日期(含)' AFTER `billable_start_date`,
  ADD COLUMN `rental_period_status` varchar(32) DEFAULT NULL COMMENT '租期解析状态' AFTER `billable_end_date`,
  ADD COLUMN `rental_period_reason_code` varchar(64) DEFAULT NULL COMMENT '租期解析状态原因' AFTER `rental_period_status`;
