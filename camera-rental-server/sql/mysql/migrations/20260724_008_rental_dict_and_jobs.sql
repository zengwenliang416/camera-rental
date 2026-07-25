-- Rental / XianGuanJia dict types + data (admin dict-tag).
-- Idempotent: skip existing type/value.

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '租赁-闲鱼授权状态', 'rental_xianyu_auth_status', 0, 'VALID/INVALID', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'rental_xianyu_auth_status' AND deleted = b'0');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '租赁-闲管家集成状态', 'rental_xianyu_integration_status', 0, 'READY/DISABLED/MISSING_CREDENTIALS', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'rental_xianyu_integration_status' AND deleted = b'0');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '租赁-渠道订单转换状态', 'rental_channel_conversion_status', 0, 'PENDING/CONVERTED/REVIEW_REQUIRED', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'rental_channel_conversion_status' AND deleted = b'0');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '租赁-闲鱼订单状态', 'rental_xianyu_order_status', 0, '闲管家 order_status 数字码', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'rental_xianyu_order_status' AND deleted = b'0');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '租赁-人工复核状态', 'rental_manual_review_status', 0, 'OPEN/RESOLVED/CLOSED', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'rental_manual_review_status' AND deleted = b'0');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '租赁-设备状态', 'rental_device_status', 0, 'AVAILABLE/RENTED/...', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` WHERE `type` = 'rental_device_status' AND deleted = b'0');

-- dict data helper pattern
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '有效', 'VALID', 'rental_xianyu_auth_status', 0, 'success', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_auth_status' AND `value` = 'VALID' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '无效', 'INVALID', 'rental_xianyu_auth_status', 0, 'danger', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_auth_status' AND `value` = 'INVALID' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '未知', 'UNKNOWN', 'rental_xianyu_auth_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_auth_status' AND `value` = 'UNKNOWN' AND deleted = b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '已就绪', 'READY', 'rental_xianyu_integration_status', 0, 'success', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_integration_status' AND `value` = 'READY' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '未启用', 'DISABLED', 'rental_xianyu_integration_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_integration_status' AND `value` = 'DISABLED' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '缺少凭据', 'MISSING_CREDENTIALS', 'rental_xianyu_integration_status', 0, 'warning', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_integration_status' AND `value` = 'MISSING_CREDENTIALS' AND deleted = b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '待转换', 'PENDING', 'rental_channel_conversion_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_channel_conversion_status' AND `value` = 'PENDING' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '已转换', 'CONVERTED', 'rental_channel_conversion_status', 0, 'success', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_channel_conversion_status' AND `value` = 'CONVERTED' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '需人工复核', 'REVIEW_REQUIRED', 'rental_channel_conversion_status', 0, 'warning', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_channel_conversion_status' AND `value` = 'REVIEW_REQUIRED' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '转换失败', 'FAILED', 'rental_channel_conversion_status', 0, 'danger', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_channel_conversion_status' AND `value` = 'FAILED' AND deleted = b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 11, '待付款', '11', 'rental_xianyu_order_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_order_status' AND `value` = '11' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 12, '待发货', '12', 'rental_xianyu_order_status', 0, 'warning', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_order_status' AND `value` = '12' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 21, '已发货', '21', 'rental_xianyu_order_status', 0, 'warning', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_order_status' AND `value` = '21' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 22, '交易成功', '22', 'rental_xianyu_order_status', 0, 'success', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_order_status' AND `value` = '22' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 23, '已退款', '23', 'rental_xianyu_order_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_order_status' AND `value` = '23' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 24, '交易关闭', '24', 'rental_xianyu_order_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_xianyu_order_status' AND `value` = '24' AND deleted = b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '待处理', 'OPEN', 'rental_manual_review_status', 0, 'warning', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_manual_review_status' AND `value` = 'OPEN' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '已处理', 'RESOLVED', 'rental_manual_review_status', 0, 'success', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_manual_review_status' AND `value` = 'RESOLVED' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '已关闭', 'CLOSED', 'rental_manual_review_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_manual_review_status' AND `value` = 'CLOSED' AND deleted = b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, '可用', 'AVAILABLE', 'rental_device_status', 0, 'success', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_device_status' AND `value` = 'AVAILABLE' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 2, '在租', 'RENTED', 'rental_device_status', 0, 'warning', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_device_status' AND `value` = 'RENTED' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 3, '维修中', 'MAINTENANCE', 'rental_device_status', 0, 'danger', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_device_status' AND `value` = 'MAINTENANCE' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 4, '已退役', 'RETIRED', 'rental_device_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_device_status' AND `value` = 'RETIRED' AND deleted = b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 5, '已停用', 'DISABLED', 'rental_device_status', 0, 'info', '', NULL, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` WHERE `dict_type` = 'rental_device_status' AND `value` = 'DISABLED' AND deleted = b'0');
