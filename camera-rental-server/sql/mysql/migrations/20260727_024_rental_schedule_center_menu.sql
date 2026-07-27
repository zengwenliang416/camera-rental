-- Add independent equipment schedule center access permission.
-- The schedule center is an independent same-origin web app deployed under
-- /admin/schedule-center/. Do not register it as an admin Vue route.

SET NAMES utf8mb4;

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7081, '排期中心访问', 'rental:schedule-center:access', 3, 80, 7000,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7081
);

UPDATE `system_menu`
SET `name` = '排期中心访问',
    `permission` = 'rental:schedule-center:access',
    `type` = 3,
    `sort` = 80,
    `parent_id` = 7000,
    `path` = '',
    `icon` = '',
    `component` = NULL,
    `component_name` = NULL,
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7081
  AND `deleted` = b'0';

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  rental_role.`role_id`, 7081, '1', NOW(), '1', NOW(), b'0', rental_role.`tenant_id`
FROM `system_role_menu` rental_role
WHERE rental_role.`menu_id` = 7000
  AND rental_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = rental_role.`role_id`
      AND existing_role_menu.`menu_id` = 7081
      AND existing_role_menu.`tenant_id` = rental_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
