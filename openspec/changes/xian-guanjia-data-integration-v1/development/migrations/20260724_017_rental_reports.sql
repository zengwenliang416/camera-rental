-- Read-only rental report indexes and dedicated admin menu.
-- Every DDL statement is idempotent because migration copies can be rerun safely.

SET NAMES utf8mb4;

SET @index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_order'
    AND index_name = 'idx_xianyu_order_report_time'
);
SET @ddl = IF(
  @index_exists = 0,
  'ALTER TABLE `xianyu_order` ADD KEY `idx_xianyu_order_report_time` (`tenant_id`, `order_time`, `shop_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xianyu_order'
    AND index_name = 'idx_xianyu_order_report_product'
);
SET @ddl = IF(
  @index_exists = 0,
  'ALTER TABLE `xianyu_order` ADD KEY `idx_xianyu_order_report_product` (`tenant_id`, `external_product_id`, `external_sku_id`, `order_time`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_order_item'
    AND index_name = 'idx_rental_item_report_billable'
);
SET @ddl = IF(
  @index_exists = 0,
  'ALTER TABLE `rental_order_item` ADD KEY `idx_rental_item_report_billable` (`tenant_id`, `billable_start_date`, `id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_device_assignment'
    AND index_name = 'idx_rental_assignment_report'
);
SET @ddl = IF(
  @index_exists = 0,
  'ALTER TABLE `rental_device_assignment` ADD KEY `idx_rental_assignment_report` (`tenant_id`, `status`, `rental_order_item_id`, `device_id`, `id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7060, 'router.rentalReport', '', 2, 7, 7000,
  'report', 'ep:data-analysis', 'rental/report/index', 'RentalBusinessReport', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7060
);

UPDATE `system_menu`
SET `name` = 'router.rentalReport',
    `permission` = '',
    `type` = 2,
    `sort` = 7,
    `parent_id` = 7000,
    `path` = 'report',
    `icon` = 'ep:data-analysis',
    `component` = 'rental/report/index',
    `component_name` = 'RentalBusinessReport',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7060
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 7060,
    `name` = '报表查询',
    `sort` = 1,
    `update_time` = NOW()
WHERE `id` = 7032
  AND `permission` = 'rental:report:query'
  AND `deleted` = b'0';

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT
  report_permission.`role_id`, 7060, '1', NOW(), '1', NOW(), b'0', report_permission.`tenant_id`
FROM `system_role_menu` report_permission
WHERE report_permission.`menu_id` = 7032
  AND report_permission.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` report_menu
    WHERE report_menu.`role_id` = report_permission.`role_id`
      AND report_menu.`menu_id` = 7060
      AND report_menu.`tenant_id` = report_permission.`tenant_id`
      AND report_menu.`deleted` = b'0'
  );
