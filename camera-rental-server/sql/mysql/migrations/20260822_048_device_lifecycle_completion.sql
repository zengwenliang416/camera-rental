-- Device lifecycle completion: persist return inspection audit, allow allocation cancel cascade.
SET NAMES utf8mb4;

ALTER TABLE `rental_device_assignment`
  ADD COLUMN `returned_at` datetime DEFAULT NULL COMMENT '回仓登记时间' AFTER `assigned_at`,
  ADD COLUMN `return_note` varchar(512) DEFAULT NULL COMMENT '回仓检测备注' AFTER `returned_at`;

ALTER TABLE `rental_order`
  ADD COLUMN `cancel_reason` varchar(512) DEFAULT NULL COMMENT '订单取消原因' AFTER `conversion_version`;
