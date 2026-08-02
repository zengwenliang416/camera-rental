-- Customer return registration: order-bound public links, submitted devices and private attachments.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `rental_return_registration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `form_no` varchar(32) NOT NULL,
  `rental_order_id` bigint NOT NULL,
  `channel_order_id` bigint DEFAULT NULL,
  `external_order_no` varchar(128) NOT NULL,
  `token_hash` char(64) NOT NULL,
  `status` varchar(32) NOT NULL,
  `carrier_code` varchar(64) DEFAULT NULL,
  `carrier_name` varchar(128) DEFAULT NULL,
  `waybill_no` varchar(128) DEFAULT NULL,
  `normalized_waybill_no` varchar(128) DEFAULT NULL,
  `shipped_date` date DEFAULT NULL,
  `issue_description` varchar(1000) DEFAULT NULL,
  `delivery_id` bigint DEFAULT NULL,
  `idempotency_key` varchar(128) DEFAULT NULL,
  `expires_at` datetime NOT NULL,
  `opened_at` datetime DEFAULT NULL,
  `submitted_at` datetime DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `review_note` varchar(1000) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_return_registration_token_hash` (`token_hash`),
  UNIQUE KEY `uk_return_registration_form_no` (`tenant_id`, `form_no`),
  KEY `idx_return_registration_order` (`tenant_id`, `rental_order_id`, `status`),
  KEY `idx_return_registration_status` (`tenant_id`, `status`, `submitted_at`),
  KEY `idx_return_registration_expiry` (`expires_at`, `status`),
  KEY `idx_return_registration_waybill` (`tenant_id`, `normalized_waybill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户设备退回登记';

CREATE TABLE IF NOT EXISTS `rental_return_registration_device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `registration_id` bigint NOT NULL,
  `device_id` bigint DEFAULT NULL,
  `assignment_id` bigint DEFAULT NULL,
  `submitted_serial` varchar(64) NOT NULL,
  `normalized_serial` varchar(64) NOT NULL,
  `match_status` varchar(32) NOT NULL,
  `match_message` varchar(255) DEFAULT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_return_registration_serial` (`tenant_id`, `registration_id`, `normalized_serial`),
  KEY `idx_return_registration_device` (`tenant_id`, `device_id`),
  KEY `idx_return_registration_device_match` (`tenant_id`, `match_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户退回登记设备';

CREATE TABLE IF NOT EXISTS `rental_return_registration_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `registration_id` bigint NOT NULL,
  `infra_file_id` bigint DEFAULT NULL,
  `file_config_id` bigint NOT NULL,
  `category` varchar(32) NOT NULL,
  `object_path` varchar(512) NOT NULL,
  `object_path_hash` char(64) NOT NULL,
  `original_name` varchar(255) NOT NULL,
  `content_type` varchar(128) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `content_sha256` char(64) DEFAULT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  `confirmed` bit(1) NOT NULL DEFAULT b'0',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_return_attachment_path` (`object_path_hash`),
  UNIQUE KEY `uk_return_attachment_file` (`infra_file_id`),
  KEY `idx_return_attachment_registration` (`tenant_id`, `registration_id`, `category`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户退回登记附件';

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
  `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
  `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7098, '客户退回登记', '', 2, 12, 7000, 'return-registration', 'ep:box',
  'rental/return-registration/index', 'RentalReturnRegistration', 0, b'1', b'1', b'1',
  '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7098);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
  `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
  `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
  (7099, '退回登记查询', 'rental:return-registration:query', 3, 1, 7098, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (7100, '创建退回链接', 'rental:return-registration:create', 3, 2, 7098, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (7101, '撤销退回链接', 'rental:return-registration:revoke', 3, 3, 7098, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
  (7102, '审核退回登记', 'rental:return-registration:review', 3, 4, 7098, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `permission` = VALUES(`permission`),
  `parent_id` = VALUES(`parent_id`), `updater` = '1', `update_time` = NOW(), `deleted` = b'0';

INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT role_menu.role_id, grant_menu.menu_id, '1', NOW(), '1', NOW(), b'0', role_menu.tenant_id
FROM `system_role_menu` role_menu
JOIN (
  SELECT 7098 menu_id UNION ALL SELECT 7099 UNION ALL SELECT 7100 UNION ALL SELECT 7101 UNION ALL SELECT 7102
) grant_menu
WHERE role_menu.menu_id = 7000 AND role_menu.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` existing
    WHERE existing.role_id = role_menu.role_id
      AND existing.menu_id = grant_menu.menu_id
      AND existing.tenant_id = role_menu.tenant_id
      AND existing.deleted = b'0'
  );
