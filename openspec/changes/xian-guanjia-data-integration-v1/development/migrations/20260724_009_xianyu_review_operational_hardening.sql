-- Xianyu order hot-query indexes and manual-review write permission.
-- Additive migration; do not fold into previously executed migrations.

ALTER TABLE `xianyu_order`
  ADD KEY `idx_xianyu_order_admin_page`
    (`tenant_id`, `shop_id`, `conversion_status`, `source_updated_at`, `id`),
  ADD KEY `idx_xianyu_order_detail_backfill`
    (`tenant_id`, `id`);

INSERT INTO `system_menu` (
  `id`, `name`, `permission`, `type`, `sort`, `parent_id`,
  `path`, `icon`, `component`, `component_name`, `status`,
  `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
) SELECT
  7033, '复核处理', 'rental:review:update', 3, 2, 7030,
  '', '', NULL, NULL, 0,
  b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission` = 'rental:review:update' AND `deleted` = b'0'
);
