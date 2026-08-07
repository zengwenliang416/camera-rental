-- XianGuanJia and Kuaidi100 configuration remain under the rental operations
-- directory, but only super administrators may see or use those pages.
-- Rental operators retain order query, sync, OCR, and shipment permissions.

UPDATE `system_menu`
SET `name` = 'router.rentalXianyu',
    `parent_id` = 7000,
    `path` = 'xianyu',
    `sort` = 1,
    `icon` = 'ep:link',
    `component` = 'rental/xianyu/index',
    `component_name` = 'RentalXianyuOps',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7001
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `name` = 'router.rentalLogisticsConfig',
    `parent_id` = 7000,
    `path` = 'logistics/config',
    `sort` = 9,
    `icon` = 'ep:setting',
    `component` = 'rental/logistics/config/index',
    `component_name` = 'RentalLogisticsConfig',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 7095
  AND `deleted` = b'0';

-- Super administrators receive every active menu through the framework and do
-- not need explicit role-menu rows. Soft-deleting these grants prevents normal
-- roles from exposing configuration, credentials, replay, raw payloads, or
-- the configuration page's historical backfill operation.
UPDATE `system_role_menu`
SET `deleted` = b'1',
    `updater` = '1',
    `update_time` = NOW()
WHERE (
    `menu_id` IN (7001, 7004, 7005, 7072, 7083, 7084, 7085, 7095)
    OR `menu_id` IN (
      SELECT restricted_menu.`id`
      FROM `system_menu` restricted_menu
      WHERE restricted_menu.`permission` = 'rental:logistics:backfill'
        AND restricted_menu.`deleted` = b'0'
    )
  )
  AND `deleted` = b'0';
