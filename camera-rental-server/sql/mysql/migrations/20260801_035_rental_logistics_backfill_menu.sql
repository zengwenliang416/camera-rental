-- Put the historical logistics backfill permission under the admin-only
-- Kuaidi100 page and grant it to roles that already own that page.

SET NAMES utf8mb4;

UPDATE `system_menu`
SET `parent_id` = 7095,
    `sort` = 4,
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'rental:logistics:backfill'
  AND `deleted` = b'0';

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  page_role.`role_id`, backfill_menu.`id`, '1', NOW(), '1', NOW(), b'0', page_role.`tenant_id`
FROM `system_role_menu` page_role
JOIN `system_menu` backfill_menu
  ON backfill_menu.`permission` = 'rental:logistics:backfill'
 AND backfill_menu.`deleted` = b'0'
WHERE page_role.`menu_id` = 7095
  AND page_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = page_role.`role_id`
      AND existing_role_menu.`menu_id` = backfill_menu.`id`
      AND existing_role_menu.`tenant_id` = page_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
