-- Persist logistics dates parsed from seller remarks for authoritative occupied ranges.

ALTER TABLE `xianyu_order`
  ADD COLUMN `ship_date` date DEFAULT NULL COMMENT '备注解析发货日期' AFTER `billable_end_date`,
  ADD COLUMN `receive_date` date DEFAULT NULL COMMENT '备注解析收货日期' AFTER `ship_date`,
  ADD COLUMN `return_date` date DEFAULT NULL COMMENT '备注解析发回日期' AFTER `receive_date`;
