-- Allow a device assignment to exist before the rental occupied plan is known.
-- The assignment is still tied to the order item and device; the schedule is
-- created later by the fulfillment reconciliation guard.
SET NAMES utf8mb4;

ALTER TABLE `rental_device_assignment`
  MODIFY COLUMN `schedule_id` bigint DEFAULT NULL
    COMMENT '设备占用排期，租期待补的已出库分配可暂为空';
