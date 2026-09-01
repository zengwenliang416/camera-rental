SET NAMES utf8mb4;

CREATE TABLE `xianyu_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `shop_id` bigint NOT NULL,
  `external_order_id` varchar(128) NOT NULL,
  `xgj_product_id` varchar(128) DEFAULT NULL,
  `xianyu_item_id` varchar(128) DEFAULT NULL,
  `xgj_sku_id` varchar(128) DEFAULT NULL,
  `xianyu_sku_id` varchar(128) DEFAULT NULL,
  `external_product_id` varchar(128) DEFAULT NULL,
  `external_sku_id` varchar(128) DEFAULT NULL,
  `preparation_status` varchar(32) NOT NULL DEFAULT 'WAITING_RECONCILIATION',
  `preparation_reason_code` varchar(64) DEFAULT NULL,
  `preparation_updated_at` datetime DEFAULT NULL,
  `order_status` varchar(64) NOT NULL DEFAULT 'UNKNOWN',
  `pay_amount` bigint NOT NULL DEFAULT 0,
  `currency` varchar(16) NOT NULL DEFAULT 'CNY',
  `seller_remark` text,
  `remark_parse_version` varchar(32) DEFAULT NULL,
  `remark_parse_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `remark_parse_source` varchar(16) DEFAULT NULL,
  `remark_parse_confidence` decimal(5,4) DEFAULT NULL,
  `remark_parse_model` varchar(128) DEFAULT NULL,
  `remark_parse_evidence_json` longtext,
  `billable_start_date` date DEFAULT NULL,
  `billable_end_date` date DEFAULT NULL,
  `ship_date` date DEFAULT NULL,
  `receive_date` date DEFAULT NULL,
  `return_date` date DEFAULT NULL,
  `rental_period_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `rental_period_reason_code` varchar(64) DEFAULT NULL,
  `source_created_at` datetime DEFAULT NULL,
  `source_updated_at` datetime DEFAULT NULL,
  `raw_payload_id` bigint DEFAULT NULL,
  `conversion_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `rental_order_id` bigint DEFAULT NULL,
  `detail_json` longtext,
  `receiver_name` varchar(128) DEFAULT NULL,
  `receiver_mobile` varchar(64) DEFAULT NULL,
  `receiver_address` varchar(1024) DEFAULT NULL,
  `order_type` int DEFAULT NULL,
  `order_time` datetime DEFAULT NULL,
  `total_amount` bigint DEFAULT NULL,
  `pay_no` varchar(128) DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `refund_status` int DEFAULT NULL,
  `refund_amount` bigint DEFAULT NULL,
  `refund_time` datetime DEFAULT NULL,
  `waybill_no` varchar(128) DEFAULT NULL,
  `express_code` varchar(64) DEFAULT NULL,
  `express_name` varchar(128) DEFAULT NULL,
  `express_fee` bigint DEFAULT NULL,
  `consign_type` int DEFAULT NULL,
  `consign_time` datetime DEFAULT NULL,
  `confirm_time` datetime DEFAULT NULL,
  `cancel_reason` varchar(512) DEFAULT NULL,
  `cancel_time` datetime DEFAULT NULL,
  `buyer_nick` varchar(256) DEFAULT NULL,
  `seller_name` varchar(256) DEFAULT NULL,
  `goods_title` varchar(512) DEFAULT NULL,
  `goods_quantity` int DEFAULT NULL,
  `goods_price` bigint DEFAULT NULL,
  `goods_json` longtext,
  `xyb_seller_amount` bigint DEFAULT NULL,
  `is_tax_included` bit(1) DEFAULT NULL,
  `idle_biz_type` int DEFAULT NULL,
  `pin_group_status` int DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xianyu_order_shop_external` (`tenant_id`, `shop_id`, `external_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `order_no` varchar(64) NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_order_id` varchar(128) DEFAULT NULL,
  `channel_order_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'REVIEW_REQUIRED',
  `rent_amount` bigint NOT NULL DEFAULT 0,
  `refund_amount` bigint NOT NULL DEFAULT 0,
  `billable_start_date` date DEFAULT NULL,
  `billable_end_date` date DEFAULT NULL,
  `occupy_start_date` date DEFAULT NULL,
  `occupy_end_date_exclusive` date DEFAULT NULL,
  `preparation_status` varchar(32) NOT NULL DEFAULT 'WAITING_RECONCILIATION',
  `preparation_reason_code` varchar(64) DEFAULT NULL,
  `preparation_updated_at` datetime DEFAULT NULL,
  `conversion_version` varchar(32) DEFAULT NULL,
  `cancel_reason` varchar(512) DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_order_order_no` (`tenant_id`, `order_no`),
  UNIQUE KEY `uk_rental_order_source` (`tenant_id`, `source_type`, `source_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `rental_order_id` bigint NOT NULL,
  `equipment_model_code` varchar(128) DEFAULT NULL,
  `source_product_id` varchar(128) DEFAULT NULL,
  `source_sku_id` varchar(128) DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT 1,
  `rent_amount` bigint NOT NULL DEFAULT 0,
  `billable_start_date` date DEFAULT NULL,
  `billable_end_date` date DEFAULT NULL,
  `occupy_start_date` date DEFAULT NULL,
  `occupy_end_date_exclusive` date DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `device_no` varchar(128) NOT NULL,
  `legacy_device_no` varchar(128) DEFAULT NULL,
  `serial_number` varchar(128) DEFAULT NULL,
  `category_code` varchar(32) DEFAULT NULL,
  `equipment_model_code` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'AVAILABLE',
  `warehouse_code` varchar(128) DEFAULT NULL,
  `purchase_amount` int DEFAULT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `source_type` varchar(32) DEFAULT NULL,
  `source_biz_id` bigint DEFAULT NULL,
  `source_item_id` bigint DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_device_no` (`tenant_id`, `device_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_device_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `rental_order_id` bigint NOT NULL,
  `rental_order_item_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `schedule_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ASSIGNED',
  `idempotency_key` varchar(128) NOT NULL,
  `assigned_at` datetime NOT NULL,
  `returned_at` datetime DEFAULT NULL,
  `return_note` varchar(512) DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_assignment_idempotency` (`tenant_id`, `idempotency_key`),
  UNIQUE KEY `uk_rental_assignment_schedule` (`tenant_id`, `schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `device_id` bigint NOT NULL,
  `rental_order_id` bigint DEFAULT NULL,
  `rental_order_item_id` bigint DEFAULT NULL,
  `schedule_type` varchar(32) NOT NULL DEFAULT 'RENTAL',
  `status` varchar(32) NOT NULL DEFAULT 'EFFECTIVE',
  `occupy_start_date` date NOT NULL,
  `occupy_end_date_exclusive` date NOT NULL,
  `idempotency_key` varchar(128) DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_schedule_idempotency` (`tenant_id`, `idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_device_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `device_id` bigint NOT NULL,
  `lock_type` varchar(32) NOT NULL,
  `reason` varchar(512) NOT NULL,
  `rental_order_id` bigint DEFAULT NULL,
  `rental_order_item_id` bigint DEFAULT NULL,
  `source_type` varchar(32) NOT NULL,
  `start_time` datetime NOT NULL,
  `planned_end_time` datetime DEFAULT NULL,
  `released_at` datetime DEFAULT NULL,
  `released_by` bigint DEFAULT NULL,
  `release_reason` varchar(512) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_device_model` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `category_id` bigint NOT NULL,
  `model_code` varchar(64) NOT NULL,
  `model_name` varchar(128) NOT NULL,
  `device_no_prefix` varchar(64) NOT NULL,
  `next_sequence` int NOT NULL DEFAULT 1,
  `sort_order` int NOT NULL DEFAULT 100,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `lock_version` int NOT NULL DEFAULT 0,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_channel_product_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `shop_id` bigint NOT NULL,
  `xianyu_item_id` varchar(128) NOT NULL,
  `xgj_product_id` varchar(128) DEFAULT NULL,
  `product_title_snapshot` varchar(512) DEFAULT NULL,
  `handling_policy` varchar(32) NOT NULL,
  `mapping_mode` varchar(32) NOT NULL DEFAULT 'SINGLE',
  `single_device_model_id` bigint DEFAULT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `rule_note` varchar(512) DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT 0,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_channel_rule_shop_item` (`tenant_id`, `shop_id`, `xianyu_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_manual_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `review_type` varchar(64) NOT NULL,
  `source_type` varchar(64) NOT NULL,
  `source_identifier` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'OPEN',
  `reason_code` varchar(64) NOT NULL,
  `reason_message` varchar(512) DEFAULT NULL,
  `resolution_note` varchar(512) DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_review_source_type`
    (`tenant_id`, `source_type`, `source_identifier`, `review_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `xianyu_order`
  (`id`, `tenant_id`, `shop_id`, `external_order_id`, `order_status`,
   `pay_amount`, `currency`, `remark_parse_status`, `rental_period_status`,
   `conversion_status`)
VALUES (1, 9, 20, 'BASE-ORDER-1', '12', 10000, 'CNY', 'PENDING', 'PENDING', 'PENDING');

INSERT INTO `rental_order`
  (`id`, `tenant_id`, `order_no`, `source_type`, `source_order_id`, `channel_order_id`,
   `status`, `rent_amount`, `refund_amount`, `occupy_end_date_exclusive`)
VALUES (1, 9, 'BASE-RENTAL-1', 'XIANYU', '20:BASE-ORDER-1', 1,
        'PENDING_ALLOCATION', 10000, 0, '2026-09-02');

INSERT INTO `rental_order_item`
  (`id`, `tenant_id`, `rental_order_id`, `quantity`, `rent_amount`,
   `occupy_end_date_exclusive`)
VALUES (1, 9, 1, 1, 10000, '2026-09-02');

INSERT INTO `rental_device`
  (`id`, `tenant_id`, `device_no`, `equipment_model_code`, `status`, `enabled`)
VALUES (1, 9, 'BASE-DEVICE-1', 'P4', 'AVAILABLE', b'1');

INSERT INTO `rental_schedule`
  (`id`, `tenant_id`, `device_id`, `rental_order_id`, `rental_order_item_id`,
   `schedule_type`, `status`, `occupy_start_date`, `occupy_end_date_exclusive`,
   `idempotency_key`)
VALUES (1, 9, 1, 1, 1, 'RENTAL', 'EFFECTIVE', '2026-08-30', '2026-09-02',
        'base-schedule-1');

INSERT INTO `rental_device_assignment`
  (`id`, `tenant_id`, `rental_order_id`, `rental_order_item_id`, `device_id`,
   `schedule_id`, `status`, `idempotency_key`, `assigned_at`, `returned_at`)
VALUES (1, 9, 1, 1, 1, 1, 'ASSIGNED', 'base-assignment-1',
        '2026-08-30 09:00:00', NULL);
