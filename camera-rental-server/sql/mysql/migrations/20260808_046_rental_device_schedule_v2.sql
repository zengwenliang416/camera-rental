-- Device schedule V2: persistent classified locks and supervisor permission.

CREATE TABLE IF NOT EXISTS `rental_device_lock` (
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
  PRIMARY KEY (`id`),
  KEY `idx_rental_device_lock_active` (`tenant_id`, `device_id`, `status`, `planned_end_time`),
  KEY `idx_rental_device_lock_order` (`tenant_id`, `rental_order_id`, `rental_order_item_id`),
  KEY `idx_rental_device_lock_type` (`tenant_id`, `lock_type`, `status`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁设备分类锁定';

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7042, '设备锁定管理', 'rental:device-lock:update', 3, 2, 7040,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'rental:device-lock:update' AND `deleted` = b'0'
);
