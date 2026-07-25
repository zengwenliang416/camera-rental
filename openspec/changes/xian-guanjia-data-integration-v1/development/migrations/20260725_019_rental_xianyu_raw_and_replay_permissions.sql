-- Separate permissions for audited, masked raw payload access and safe replay.
-- Raw JSON is never returned without server-side redaction; replay remains read-only.

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7004, '闲管家原始载荷查看', 'rental:xianyu:raw', 3, 3, 7001,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7004);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7005, '闲管家安全重放', 'rental:xianyu:replay', 3, 4, 7001,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 7005);
