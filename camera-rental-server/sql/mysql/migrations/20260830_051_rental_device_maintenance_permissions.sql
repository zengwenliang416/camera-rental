-- Device instance update/delete buttons and least-privilege role grants.
SET NAMES utf8mb4;

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7024, '设备编辑', 'rental:device:update', 3, 4, 7020,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7024);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7025, '设备删除', 'rental:device:delete', 3, 5, 7020,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7025);

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  device_creator.`role_id`, maintenance_menu.`menu_id`,
  '1', NOW(), '1', NOW(), b'0', device_creator.`tenant_id`
FROM `system_role_menu` device_creator
JOIN (
  SELECT 7024 AS `menu_id`
  UNION ALL SELECT 7025
) maintenance_menu
WHERE device_creator.`menu_id` = 7022
  AND device_creator.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = device_creator.`role_id`
      AND existing_role_menu.`menu_id` = maintenance_menu.`menu_id`
      AND existing_role_menu.`tenant_id` = device_creator.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
