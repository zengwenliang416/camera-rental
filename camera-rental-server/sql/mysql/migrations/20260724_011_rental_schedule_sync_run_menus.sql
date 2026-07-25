-- Read-only admin entries for device schedules and XianGuanJia sync history.

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7040, '设备排期', '', 2, 5, 7000,
  'schedule', 'ep:calendar', 'rental/schedule/index', 'RentalSchedule', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7040
);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7041, '排期查询', 'rental:schedule:query', 3, 1, 7040,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'rental:schedule:query' AND `deleted` = b'0'
);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7050, '同步运行历史', '', 2, 6, 7000,
  'sync-run', 'ep:clock', 'rental/sync-run/index', 'RentalXianyuSyncRun', 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 7050
);

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
