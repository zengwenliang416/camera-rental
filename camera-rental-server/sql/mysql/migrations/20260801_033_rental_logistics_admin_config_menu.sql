-- Move Kuaidi100 provider configuration into camera-rental-admin.
-- The schedule center remains a logistics read consumer and does not own
-- provider or credential management.

SET NAMES utf8mb4;

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7095, 'router.rentalLogisticsConfig', '', 2, 9, 7000,
  'logistics/config', 'ep:setting', 'rental/logistics/config/index', 'RentalLogisticsConfig', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7095
);

UPDATE `system_menu`
SET `name` = 'router.rentalLogisticsConfig',
    `permission` = '',
    `type` = 2,
    `sort` = 9,
    `parent_id` = 7000,
    `path` = 'logistics/config',
    `icon` = 'ep:setting',
    `component` = 'rental/logistics/config/index',
    `component_name` = 'RentalLogisticsConfig',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7095
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 7095,
    `sort` = CASE `permission`
      WHEN 'rental:logistics:config:query' THEN 1
      WHEN 'rental:logistics:config:update' THEN 2
      WHEN 'rental:logistics:config:verify' THEN 3
      ELSE `sort`
    END,
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` IN (
  'rental:logistics:config:query',
  'rental:logistics:config:update',
  'rental:logistics:config:verify'
)
  AND `deleted` = b'0';

-- Existing roles allowed to manage the XianGuanJia integration also receive
-- the logistics configuration page and its narrowly scoped permissions.
INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  config_role.`role_id`, menu_to_grant.`menu_id`, '1', NOW(), '1', NOW(), b'0', config_role.`tenant_id`
FROM `system_role_menu` config_role
JOIN (
  SELECT 7095 AS `menu_id`
  UNION ALL SELECT 7083
  UNION ALL SELECT 7084
  UNION ALL SELECT 7085
) menu_to_grant
WHERE config_role.`menu_id` = 7072
  AND config_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = config_role.`role_id`
      AND existing_role_menu.`menu_id` = menu_to_grant.`menu_id`
      AND existing_role_menu.`tenant_id` = config_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
