-- Queryable result ledger for asynchronous reconciliation triggered by rule changes.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `rental_channel_reconciliation_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `product_rule_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `xianyu_item_id` varchar(64) NOT NULL,
  `trigger_type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `scanned_count` int NOT NULL DEFAULT 0,
  `skipped_count` int NOT NULL DEFAULT 0,
  `created_count` int NOT NULL DEFAULT 0,
  `updated_count` int NOT NULL DEFAULT 0,
  `unchanged_count` int NOT NULL DEFAULT 0,
  `conflict_count` int NOT NULL DEFAULT 0,
  `failed_count` int NOT NULL DEFAULT 0,
  `review_required_count` int NOT NULL DEFAULT 0,
  `last_error_code` varchar(128) DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_rental_channel_reconcile_status`
    (`tenant_id`, `status`, `create_time`),
  KEY `idx_rental_channel_reconcile_rule`
    (`tenant_id`, `product_rule_id`, `id`),
  KEY `idx_rental_channel_reconcile_scope`
    (`tenant_id`, `shop_id`, `xianyu_item_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道商品规则异步重评任务';
