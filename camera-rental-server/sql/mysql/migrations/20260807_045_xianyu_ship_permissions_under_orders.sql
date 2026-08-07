-- Shipment is an order operation, not an integration-configuration operation.
-- Keep XianGuanJia configuration admin-only while exposing shipment actions to
-- roles that already have access to the channel-order page.

UPDATE `system_menu`
SET `parent_id` = 7010,
    `sort` = 2,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7070
  AND `permission` = 'rental:xianyu:ship'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `parent_id` = 7010,
    `sort` = 3,
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7071
  AND `permission` = 'rental:xianyu:ship:ocr'
  AND `deleted` = b'0';

INSERT INTO `system_role_menu` (
  `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT DISTINCT
  order_role.`role_id`, shipment_menu.`menu_id`, '1', NOW(), '1', NOW(), b'0', order_role.`tenant_id`
FROM `system_role_menu` order_role
JOIN (
  SELECT 7070 AS `menu_id`
  UNION ALL SELECT 7071
) shipment_menu
WHERE order_role.`menu_id` = 7010
  AND order_role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing_role_menu
    WHERE existing_role_menu.`role_id` = order_role.`role_id`
      AND existing_role_menu.`menu_id` = shipment_menu.`menu_id`
      AND existing_role_menu.`tenant_id` = order_role.`tenant_id`
      AND existing_role_menu.`deleted` = b'0'
  );
