-- Repair rental menu visibility for existing databases.
-- Migrations 011/017 add schedule, sync-run, and report routes, but existing
-- local/approved databases may already have rental role grants without these
-- later menu ids. This migration is additive and idempotent.

SET NAMES utf8mb4;

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7040, 'router.rentalSchedule', '', 2, 5, 7000,
  'schedule', 'ep:calendar', 'rental/schedule/index', 'RentalSchedule', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7040
);

UPDATE `system_menu`
SET `name` = 'router.rentalSchedule',
    `permission` = '',
    `type` = 2,
    `sort` = 5,
    `parent_id` = 7000,
    `path` = 'schedule',
    `icon` = 'ep:calendar',
    `component` = 'rental/schedule/index',
    `component_name` = 'RentalSchedule',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7040
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7041, '排期查询', 'rental:schedule:query', 3, 1, 7040,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7041
);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7050, 'router.rentalSyncRun', '', 2, 6, 7000,
  'sync-run', 'ep:clock', 'rental/sync-run/index', 'RentalXianyuSyncRun', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7050
);

UPDATE `system_menu`
SET `name` = 'router.rentalSyncRun',
    `permission` = '',
    `type` = 2,
    `sort` = 6,
    `parent_id` = 7000,
    `path` = 'sync-run',
    `icon` = 'ep:clock',
    `component` = 'rental/sync-run/index',
    `component_name` = 'RentalXianyuSyncRun',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7050
  AND `deleted` = b'0';

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7051, '同步历史查询', 'rental:xianyu:query', 3, 1, 7050,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7051
);

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
SELECT DISTINCT
  rental_role.`role_id`, missing_menu.`menu_id`, '1', NOW(), '1', NOW(), b'0', rental_role.`tenant_id`
FROM `system_role_menu` rental_role
JOIN (
  SELECT 7040 AS `menu_id`
  UNION ALL SELECT 7041
  UNION ALL SELECT 7050
  UNION ALL SELECT 7051
  UNION ALL SELECT 7060
) missing_menu
WHERE rental_role.`menu_id` = 7000
  AND rental_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = rental_role.`role_id`
      AND existing_role_menu.`menu_id` = missing_menu.`menu_id`
      AND existing_role_menu.`tenant_id` = rental_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
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
