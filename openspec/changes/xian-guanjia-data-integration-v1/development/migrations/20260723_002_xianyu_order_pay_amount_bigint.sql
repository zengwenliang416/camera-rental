-- The official XianGuanJia order detail schema declares pay_amount as int64 cents.
-- This additive type widening preserves imported order revenue without truncation.
ALTER TABLE `xianyu_order`
  MODIFY COLUMN `pay_amount` bigint NOT NULL DEFAULT 0;
