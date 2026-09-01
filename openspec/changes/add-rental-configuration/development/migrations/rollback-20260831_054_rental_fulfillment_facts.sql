-- Disposable or pre-production rollback for migration 054 only.
SET NAMES utf8mb4;

ALTER TABLE `rental_device_assignment`
  DROP COLUMN `inspection_result`,
  DROP COLUMN `inspection_completed_at`;

ALTER TABLE `rental_order_item`
  DROP COLUMN `expected_send_back_date`;

ALTER TABLE `rental_order`
  DROP COLUMN `settled_at`,
  DROP COLUMN `expected_send_back_date`;
