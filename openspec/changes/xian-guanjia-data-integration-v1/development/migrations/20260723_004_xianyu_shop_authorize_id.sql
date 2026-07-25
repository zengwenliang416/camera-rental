-- Persist XianGuanJia authorize_id for order-list window queries.
ALTER TABLE `xianyu_shop`
  ADD COLUMN `authorize_id` varchar(64) DEFAULT NULL COMMENT '闲管家授权ID' AFTER `external_shop_id`,
  ADD KEY `idx_xianyu_shop_authorize` (`tenant_id`, `authorize_id`);
