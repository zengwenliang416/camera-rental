-- XianGuanJia order shipment workflow: local shipment evidence + permissions.
-- No credentials, signatures, phone numbers, or addresses are stored here.

CREATE TABLE IF NOT EXISTS `rental_device_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `channel_order_id` bigint NOT NULL,
  `assignment_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `waybill_no` varchar(128) NOT NULL,
  `express_code` varchar(64) NOT NULL,
  `express_name` varchar(128) NOT NULL,
  `ship_request_hash` char(32) NOT NULL,
  `ship_response_code` int DEFAULT NULL,
  `ship_response_msg` varchar(512) DEFAULT NULL,
  `ocr_confirmed` bit(1) NOT NULL DEFAULT b'0',
  `source` varchar(16) NOT NULL,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_shipment_idempotency` (`tenant_id`, `idempotency_key`),
  UNIQUE KEY `uk_rental_shipment_waybill` (`tenant_id`, `channel_order_id`, `waybill_no`, `express_code`),
  KEY `idx_rental_shipment_assignment` (`tenant_id`, `assignment_id`),
  KEY `idx_rental_shipment_device` (`tenant_id`, `device_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁设备发货记录';

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7070, '闲管家订单发货', 'rental:xianyu:ship', 3, 5, 7001,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7070);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7071, '闲管家发货识别', 'rental:xianyu:ship:ocr', 3, 6, 7001,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7071);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  xianyu_role.`role_id`, missing_menu.`menu_id`, '1', NOW(), '1', NOW(), b'0', xianyu_role.`tenant_id`
FROM `system_role_menu` xianyu_role
JOIN (
  SELECT 7070 AS `menu_id`
  UNION ALL SELECT 7071
) missing_menu
WHERE xianyu_role.`menu_id` = 7001
  AND xianyu_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = xianyu_role.`role_id`
      AND existing_role_menu.`menu_id` = missing_menu.`menu_id`
      AND existing_role_menu.`tenant_id` = xianyu_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
