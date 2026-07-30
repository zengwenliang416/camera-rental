-- Tenant-managed XianGuanJia integration configuration.
-- AppSecret is encrypted/decrypted by EncryptTypeHandler using the generic
-- mybatis-plus.encryptor.password infrastructure key.

ALTER TABLE `xianyu_application`
  ADD COLUMN `base_url` varchar(512) DEFAULT 'https://open.goofish.pro' AFTER `enabled`,
  ADD COLUMN `app_key` varchar(128) DEFAULT NULL AFTER `base_url`,
  ADD COLUMN `app_secret` varchar(512) DEFAULT NULL AFTER `app_key`,
  ADD COLUMN `webhook_base_url` varchar(512) DEFAULT NULL AFTER `app_secret`,
  ADD COLUMN `write_enabled` bit(1) NOT NULL DEFAULT b'0' AFTER `webhook_base_url`,
  ADD COLUMN `job_enabled` bit(1) NOT NULL DEFAULT b'0' AFTER `write_enabled`,
  ADD COLUMN `lookback_days` int NOT NULL DEFAULT 7 AFTER `job_enabled`,
  ADD COLUMN `overlap_minutes` int NOT NULL DEFAULT 10 AFTER `lookback_days`,
  ADD COLUMN `max_pages_per_shop` int NOT NULL DEFAULT 20 AFTER `overlap_minutes`,
  ADD COLUMN `page_size` int NOT NULL DEFAULT 50 AFTER `max_pages_per_shop`,
  ADD COLUMN `push_retry_stale_seconds` int NOT NULL DEFAULT 120 AFTER `page_size`,
  ADD COLUMN `push_retry_batch_size` int NOT NULL DEFAULT 100 AFTER `push_retry_stale_seconds`;

SET @xianyu_app_key_index_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`statistics`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'xianyu_application'
    AND `index_name` = 'uk_xianyu_application_app_key'
);
SET @xianyu_app_key_index_sql = IF(
  @xianyu_app_key_index_exists = 0,
  'CREATE UNIQUE INDEX `uk_xianyu_application_app_key` ON `xianyu_application` (`app_key`)',
  'SELECT 1'
);
PREPARE xianyu_app_key_index_stmt FROM @xianyu_app_key_index_sql;
EXECUTE xianyu_app_key_index_stmt;
DEALLOCATE PREPARE xianyu_app_key_index_stmt;

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7072, '闲管家配置管理', 'rental:xianyu:config:update', 3, 7, 7001,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7072);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  xianyu_role.`role_id`, 7072, '1', NOW(), '1', NOW(), b'0', xianyu_role.`tenant_id`
FROM `system_role_menu` xianyu_role
WHERE xianyu_role.`menu_id` = 7001
  AND xianyu_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = xianyu_role.`role_id`
      AND existing_role_menu.`menu_id` = 7072
      AND existing_role_menu.`tenant_id` = xianyu_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
