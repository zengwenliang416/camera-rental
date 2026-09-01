-- Disposable or pre-production rollback for migration 053 only.
SET NAMES utf8mb4;

ALTER TABLE `rental_device_model`
  DROP COLUMN `lock_version`;

ALTER TABLE `rental_device_category`
  DROP COLUMN `lock_version`;
