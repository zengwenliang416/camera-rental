-- Channel order pay_amount is documented as int64 cents, so converted rental amounts must not truncate it.
ALTER TABLE `rental_order`
  MODIFY COLUMN `rent_amount` bigint NOT NULL DEFAULT 0,
  MODIFY COLUMN `refund_amount` bigint NOT NULL DEFAULT 0;

ALTER TABLE `rental_order_item`
  MODIFY COLUMN `rent_amount` bigint NOT NULL DEFAULT 0;
