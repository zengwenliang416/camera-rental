-- Add optimistic locking to the existing rental catalog tables.
SET NAMES utf8mb4;

ALTER TABLE `rental_device_category`
  ADD COLUMN `lock_version` int NOT NULL DEFAULT 0
    COMMENT 'Optimistic lock version' AFTER `enabled`;

ALTER TABLE `rental_device_model`
  ADD COLUMN `lock_version` int NOT NULL DEFAULT 0
    COMMENT 'Optimistic lock version' AFTER `enabled`;
