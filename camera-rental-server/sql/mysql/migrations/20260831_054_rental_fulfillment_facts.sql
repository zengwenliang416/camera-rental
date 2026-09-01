-- Add explicit expected-return, inspection, and settlement facts for remark-safe updates.
SET NAMES utf8mb4;

ALTER TABLE `rental_order`
  ADD COLUMN `expected_send_back_date` date DEFAULT NULL
    COMMENT 'Latest expected customer send-back date' AFTER `occupy_end_date_exclusive`,
  ADD COLUMN `settled_at` datetime DEFAULT NULL
    COMMENT 'Authoritative financial settlement completion time' AFTER `refund_amount`;

ALTER TABLE `rental_order_item`
  ADD COLUMN `expected_send_back_date` date DEFAULT NULL
    COMMENT 'Latest expected customer send-back date' AFTER `occupy_end_date_exclusive`;

ALTER TABLE `rental_device_assignment`
  ADD COLUMN `inspection_completed_at` datetime DEFAULT NULL
    COMMENT 'Warehouse inspection completion time' AFTER `returned_at`,
  ADD COLUMN `inspection_result` varchar(16) DEFAULT NULL
    COMMENT 'PASSED or FAILED warehouse inspection result' AFTER `inspection_completed_at`;
