-- Standardize customer-facing machine codes as MODEL-01 while preserving legacy lookups.
SET NAMES utf8mb4;

SET @legacy_device_no_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_device'
    AND column_name = 'legacy_device_no'
);
SET @add_legacy_device_no_sql = IF(
  @legacy_device_no_column_exists = 0,
  'ALTER TABLE `rental_device` ADD COLUMN `legacy_device_no` varchar(128) DEFAULT NULL AFTER `device_no`',
  'SELECT 1'
);
PREPARE add_legacy_device_no_stmt FROM @add_legacy_device_no_sql;
EXECUTE add_legacy_device_no_stmt;
DEALLOCATE PREPARE add_legacy_device_no_stmt;

SET @legacy_device_no_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_device'
    AND index_name = 'uk_rental_device_legacy_no'
);
SET @add_legacy_device_no_index_sql = IF(
  @legacy_device_no_index_exists = 0,
  'ALTER TABLE `rental_device` ADD UNIQUE KEY `uk_rental_device_legacy_no` (`tenant_id`, `legacy_device_no`)',
  'SELECT 1'
);
PREPARE add_legacy_device_no_index_stmt FROM @add_legacy_device_no_index_sql;
EXECUTE add_legacy_device_no_index_stmt;
DEALLOCATE PREPARE add_legacy_device_no_index_stmt;

DROP TEMPORARY TABLE IF EXISTS `tmp_rental_device_short_code`;
CREATE TEMPORARY TABLE `tmp_rental_device_short_code` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `old_device_no` varchar(128) NOT NULL,
  `new_device_no` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tmp_rental_device_short_code` (`tenant_id`, `new_device_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tmp_rental_device_short_code` (`id`, `tenant_id`, `old_device_no`, `new_device_no`)
SELECT ranked.id,
       ranked.tenant_id,
       ranked.device_no,
       CONCAT(ranked.code_prefix, '-', LPAD(ranked.sequence_no, 2, '0'))
FROM (
  SELECT d.id,
         d.tenant_id,
         d.device_no,
         UPPER(TRIM(BOTH '-' FROM REGEXP_REPLACE(TRIM(d.equipment_model_code), '[^A-Za-z0-9]+', '-')))
           AS code_prefix,
         ROW_NUMBER() OVER (
           PARTITION BY d.tenant_id,
             UPPER(TRIM(BOTH '-' FROM REGEXP_REPLACE(TRIM(d.equipment_model_code), '[^A-Za-z0-9]+', '-')))
           ORDER BY d.id
         ) AS sequence_no
  FROM `rental_device` d
) ranked
WHERE ranked.code_prefix <> ''
  AND ranked.sequence_no <= 99;

UPDATE `rental_device` d
JOIN `tmp_rental_device_short_code` mapping ON mapping.id = d.id
SET d.legacy_device_no = mapping.old_device_no
WHERE d.legacy_device_no IS NULL
  AND BINARY d.device_no <> BINARY mapping.new_device_no;

UPDATE `rental_device` d
JOIN `tmp_rental_device_short_code` mapping ON mapping.id = d.id
SET d.device_no = CONCAT('__SHORT_CODE_041_', d.id);

UPDATE `rental_device` d
JOIN `tmp_rental_device_short_code` mapping ON mapping.id = d.id
SET d.device_no = mapping.new_device_no;

DROP TEMPORARY TABLE `tmp_rental_device_short_code`;
