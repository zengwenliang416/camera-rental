-- Add a stable category code for the current rental device model catalog.
SET NAMES utf8mb4;

SET @rental_device_category_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_device'
    AND column_name = 'category_code'
);
SET @add_rental_device_category_column_sql = IF(
  @rental_device_category_column_exists = 0,
  'ALTER TABLE `rental_device` ADD COLUMN `category_code` varchar(32) DEFAULT NULL AFTER `serial_number`',
  'SELECT 1'
);
PREPARE add_rental_device_category_column_stmt FROM @add_rental_device_category_column_sql;
EXECUTE add_rental_device_category_column_stmt;
DEALLOCATE PREPARE add_rental_device_category_column_stmt;

UPDATE `rental_device`
SET `category_code` = CASE UPPER(TRIM(`equipment_model_code`))
  WHEN '360' THEN 'DJI'
  WHEN 'NANO' THEN 'DJI'
  WHEN 'A5' THEN 'DJI'
  WHEN 'A6' THEN 'DJI'
  WHEN 'P3' THEN 'DJI'
  WHEN 'P4' THEN 'DJI'
  WHEN 'P4P' THEN 'DJI'
  WHEN 'ACE' THEN 'INSTA360'
  WHEN 'X5' THEN 'INSTA360'
  WHEN 'GT' THEN 'INSTA360'
  WHEN 'G3' THEN 'INSTA360'
  WHEN 'X300P' THEN 'PHONE'
  WHEN 'X200U' THEN 'PHONE'
  WHEN 'X300U' THEN 'PHONE'
  WHEN 'XT5' THEN 'FUJIFILM'
  WHEN 'XT50' THEN 'FUJIFILM'
  WHEN 'XS20' THEN 'FUJIFILM'
  WHEN 'X100VI' THEN 'FUJIFILM'
  WHEN 'R50' THEN 'CANON'
  WHEN 'G12' THEN 'CANON'
  WHEN 'G7X2' THEN 'CANON'
  WHEN 'GR3X' THEN 'RICOH'
  WHEN 'GR4' THEN 'RICOH'
  WHEN '支架' THEN 'STAND'
  ELSE `category_code`
END
WHERE `category_code` IS NULL;

SET @rental_device_category_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_device'
    AND index_name = 'idx_rental_device_category_model_status'
);
SET @add_rental_device_category_index_sql = IF(
  @rental_device_category_index_exists = 0,
  'ALTER TABLE `rental_device` ADD KEY `idx_rental_device_category_model_status` (`tenant_id`, `category_code`, `equipment_model_code`, `status`)',
  'SELECT 1'
);
PREPARE add_rental_device_category_index_stmt FROM @add_rental_device_category_index_sql;
EXECUTE add_rental_device_category_index_stmt;
DEALLOCATE PREPARE add_rental_device_category_index_stmt;
