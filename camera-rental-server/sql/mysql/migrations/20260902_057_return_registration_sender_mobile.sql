-- Store the sender contact submitted through the public return-registration form.
SET NAMES utf8mb4;

ALTER TABLE `rental_return_registration`
  ADD COLUMN `sender_mobile` varchar(32) DEFAULT NULL
  COMMENT '本次退回登记的发件人手机号'
  AFTER `return_method`;
