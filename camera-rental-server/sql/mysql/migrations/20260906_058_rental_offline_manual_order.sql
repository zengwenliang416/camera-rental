-- Offline manual rental order entry: customer master data, per-order delivery facts,
-- order-level customer/deposit columns, and the admin menu/button for manual creation.
-- Encrypted columns (mobile / receiver_mobile / receiver_address) are AES-encrypted by
-- MyBatis EncryptTypeHandler; equality lookup on mobile relies on deterministic encryption.
SET NAMES utf8mb4;

ALTER TABLE `rental_order`
  ADD COLUMN `customer_id` bigint DEFAULT NULL
    COMMENT '线下客户主档 ID（rental_customer.id），渠道订单为空' AFTER `channel_order_id`,
  ADD COLUMN `deposit_amount` bigint DEFAULT NULL
    COMMENT '押金，单位分' AFTER `rent_amount`,
  ADD KEY `idx_rental_order_customer` (`tenant_id`, `customer_id`);

CREATE TABLE IF NOT EXISTS `rental_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `name` varchar(64) NOT NULL COMMENT '客户姓名',
  `mobile` varchar(255) NOT NULL COMMENT '手机号（AES 加密存储，仅支持完整号码等值反查）',
  `wechat_id` varchar(64) DEFAULT NULL COMMENT '微信号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_rental_customer_mobile` (`tenant_id`, `mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下租赁客户';

CREATE TABLE IF NOT EXISTS `rental_order_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `rental_order_id` bigint NOT NULL COMMENT '租赁订单 ID',
  `delivery_method` varchar(16) NOT NULL COMMENT 'EXPRESS 快递 / ERRAND 跑腿 / SELF_DELIVERY 自送',
  `receiver_name` varchar(64) DEFAULT NULL COMMENT '收货人姓名',
  `receiver_mobile` varchar(255) DEFAULT NULL COMMENT '收货人手机号（AES 加密存储）',
  `receiver_address` varchar(512) DEFAULT NULL COMMENT '收货地址（AES 加密存储）',
  `delivery_remark` varchar(255) DEFAULT NULL COMMENT '配送备注',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_order_delivery_order` (`tenant_id`, `rental_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁订单配送信息（线下录单）';

-- 菜单：线下录单页面（挂目录 7000 租赁运营）+ 录单按钮权限
INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7113, '线下录单', '', 2, 6, 7000,
  'order-create', 'ep:edit-pen', 'rental/order-create/index', 'RentalOrderCreate', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7113);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7114, '线下录单提交', 'rental:order:create', 3, 1, 7113,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7114);

-- 角色授权：已拥有「渠道订单」页面（7010）的角色获得线下录单页面与按钮
INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  order_role.`role_id`, manual_menu.`menu_id`, '1', NOW(), '1', NOW(), b'0', order_role.`tenant_id`
FROM `system_role_menu` order_role
JOIN (
  SELECT 7113 AS `menu_id`
  UNION ALL SELECT 7114
) manual_menu
WHERE order_role.`menu_id` = 7010
  AND order_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = order_role.`role_id`
      AND existing_role_menu.`menu_id` = manual_menu.`menu_id`
      AND existing_role_menu.`tenant_id` = order_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
