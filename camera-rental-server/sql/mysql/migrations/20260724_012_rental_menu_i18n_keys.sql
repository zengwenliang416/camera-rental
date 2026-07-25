-- Dynamic menu titles are passed through the admin i18n function.
-- Store locale keys for rental page menus so light/dark zh-CN and en modes stay consistent.

UPDATE `system_menu` SET `name` = 'router.rental', `updater` = '1', `update_time` = NOW()
WHERE `id` = 7000 AND `deleted` = b'0';

UPDATE `system_menu` SET `name` = 'router.rentalXianyu', `updater` = '1', `update_time` = NOW()
WHERE `id` = 7001 AND `deleted` = b'0';

UPDATE `system_menu` SET `name` = 'router.rentalOrder', `updater` = '1', `update_time` = NOW()
WHERE `id` = 7010 AND `deleted` = b'0';

UPDATE `system_menu` SET `name` = 'router.rentalDevice', `updater` = '1', `update_time` = NOW()
WHERE `id` = 7020 AND `deleted` = b'0';

UPDATE `system_menu` SET `name` = 'router.rentalReview', `updater` = '1', `update_time` = NOW()
WHERE `id` = 7030 AND `deleted` = b'0';

UPDATE `system_menu` SET `name` = 'router.rentalSchedule', `updater` = '1', `update_time` = NOW()
WHERE `id` = 7040 AND `deleted` = b'0';

UPDATE `system_menu` SET `name` = 'router.rentalSyncRun', `updater` = '1', `update_time` = NOW()
WHERE `id` = 7050 AND `deleted` = b'0';
