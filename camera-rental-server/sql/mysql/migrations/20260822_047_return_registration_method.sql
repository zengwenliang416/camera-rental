-- Support non-express return methods: customer self delivery or errand courier drop-off.
SET NAMES utf8mb4;

ALTER TABLE `rental_return_registration`
  ADD COLUMN `return_method` varchar(32) NOT NULL DEFAULT 'EXPRESS'
  COMMENT '归还方式：EXPRESS 快递寄回 / SELF_DELIVERY 本人送回 / ERRAND 跑腿送回'
  AFTER `status`;
