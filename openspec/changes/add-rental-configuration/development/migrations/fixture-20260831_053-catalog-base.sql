SET NAMES utf8mb4;

CREATE TABLE `rental_device_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `category_code` varchar(32) NOT NULL,
  `category_name` varchar(64) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 100,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_device_category_code` (`tenant_id`, `category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rental_device_model` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `category_id` bigint NOT NULL,
  `model_code` varchar(64) NOT NULL,
  `model_name` varchar(128) NOT NULL,
  `device_no_prefix` varchar(64) NOT NULL,
  `next_sequence` int NOT NULL DEFAULT 1,
  `sort_order` int NOT NULL DEFAULT 100,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_device_model_code` (`tenant_id`, `model_code`),
  UNIQUE KEY `uk_rental_device_model_prefix` (`tenant_id`, `device_no_prefix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `rental_device_category` (
  `id`, `tenant_id`, `category_code`, `category_name`, `sort_order`, `enabled`
) VALUES
  (1, 77, 'FIXTURE', 'Fixture category', 10, b'1');

INSERT INTO `rental_device_model` (
  `id`, `tenant_id`, `category_id`, `model_code`, `model_name`,
  `device_no_prefix`, `next_sequence`, `sort_order`, `enabled`
) VALUES
  (1, 77, 1, 'FIXTURE-MODEL', 'Fixture model', 'FIXTURE', 1, 10, b'1');
