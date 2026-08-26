-- Tenant-managed rental device categories, models and numbering prefixes.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `rental_device_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `category_code` varchar(32) NOT NULL,
  `category_name` varchar(64) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 100,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_device_category_code` (`tenant_id`, `category_code`),
  KEY `idx_rental_device_category_enabled` (`tenant_id`, `enabled`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户租赁设备大类';

CREATE TABLE IF NOT EXISTS `rental_device_model` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `category_id` bigint NOT NULL,
  `model_code` varchar(64) NOT NULL,
  `model_name` varchar(128) NOT NULL,
  `device_no_prefix` varchar(64) NOT NULL,
  `next_sequence` int NOT NULL DEFAULT 1,
  `sort_order` int NOT NULL DEFAULT 100,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_device_model_code` (`tenant_id`, `model_code`),
  UNIQUE KEY `uk_rental_device_model_prefix` (`tenant_id`, `device_no_prefix`),
  KEY `idx_rental_device_model_category` (`tenant_id`, `category_id`, `enabled`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户租赁设备型号';

INSERT IGNORE INTO `rental_device_category`
  (`tenant_id`, `category_code`, `category_name`, `sort_order`, `enabled`)
SELECT tenants.tenant_id, seeds.category_code, seeds.category_name, seeds.sort_order, b'1'
FROM (
  SELECT `id` AS tenant_id FROM `system_tenant` WHERE `deleted` = b'0'
  UNION
  SELECT DISTINCT `tenant_id` FROM `rental_device`
) tenants
CROSS JOIN (
  SELECT 'DJI' AS category_code, '大疆' AS category_name, 10 AS sort_order
  UNION ALL SELECT 'INSTA360', '影石', 20
  UNION ALL SELECT 'PHONE', '手机', 30
  UNION ALL SELECT 'FUJIFILM', '富士', 40
  UNION ALL SELECT 'CANON', '佳能', 50
  UNION ALL SELECT 'RICOH', '理光', 60
  UNION ALL SELECT 'STAND', '支架', 70
) seeds;

INSERT IGNORE INTO `rental_device_model`
  (`tenant_id`, `category_id`, `model_code`, `model_name`, `device_no_prefix`,
   `next_sequence`, `sort_order`, `enabled`)
SELECT categories.tenant_id, categories.id, seeds.model_code, seeds.model_name,
       seeds.device_no_prefix, 1, seeds.sort_order, b'1'
FROM `rental_device_category` categories
JOIN (
  SELECT 'DJI' AS category_code, '360' AS model_code, '360' AS model_name,
         '360' AS device_no_prefix, 10 AS sort_order
  UNION ALL SELECT 'DJI', 'NANO', 'NANO', 'NANO', 20
  UNION ALL SELECT 'DJI', 'A5', 'A5', 'A5', 30
  UNION ALL SELECT 'DJI', 'A6', 'A6', 'A6', 40
  UNION ALL SELECT 'DJI', 'P3', 'P3', 'P3', 50
  UNION ALL SELECT 'DJI', 'P4', 'P4', 'P4', 60
  UNION ALL SELECT 'DJI', 'P4P', 'P4P', 'P4P', 70
  UNION ALL SELECT 'INSTA360', 'ACE', 'ACE', 'ACE', 10
  UNION ALL SELECT 'INSTA360', 'X5', 'X5', 'X5', 20
  UNION ALL SELECT 'INSTA360', 'GT', 'GT', 'GT', 30
  UNION ALL SELECT 'INSTA360', 'G3', 'G3', 'G3', 40
  UNION ALL SELECT 'PHONE', 'X300P', 'X300P', 'X300P', 10
  UNION ALL SELECT 'PHONE', 'X200U', 'X200U', 'X200U', 20
  UNION ALL SELECT 'PHONE', 'X300U', 'X300U', 'X300U', 30
  UNION ALL SELECT 'FUJIFILM', 'XT5', 'XT5', 'XT5', 10
  UNION ALL SELECT 'FUJIFILM', 'XT50', 'XT50', 'XT50', 20
  UNION ALL SELECT 'FUJIFILM', 'XS20', 'XS20', 'XS20', 30
  UNION ALL SELECT 'FUJIFILM', 'X100VI', 'X100VI', 'X100VI', 40
  UNION ALL SELECT 'CANON', 'R50', 'R50', 'R50', 10
  UNION ALL SELECT 'CANON', 'G12', 'G12', 'G12', 20
  UNION ALL SELECT 'CANON', 'G7X2', 'G7X2', 'G7X2', 30
  UNION ALL SELECT 'RICOH', 'GR3X', 'GR3X', 'GR3X', 10
  UNION ALL SELECT 'RICOH', 'GR4', 'GR4', 'GR4', 20
  UNION ALL SELECT 'STAND', '支架', '支架', '支架', 10
) seeds ON seeds.category_code = categories.category_code
WHERE categories.deleted = b'0';

UPDATE `rental_device_model` models
LEFT JOIN (
  SELECT `tenant_id`, UPPER(TRIM(`equipment_model_code`)) AS model_code,
         MAX(CAST(SUBSTRING_INDEX(`device_no`, '-', -1) AS UNSIGNED)) AS max_sequence
  FROM `rental_device`
  WHERE `device_no` REGEXP '-[0-9]{2}$'
  GROUP BY `tenant_id`, UPPER(TRIM(`equipment_model_code`))
) devices
  ON devices.tenant_id = models.tenant_id
 AND devices.model_code = models.model_code
SET models.next_sequence = GREATEST(models.next_sequence, COALESCE(devices.max_sequence + 1, 1))
WHERE models.deleted = b'0';
