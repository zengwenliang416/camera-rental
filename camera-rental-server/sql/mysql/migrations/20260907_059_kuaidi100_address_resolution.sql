-- 快递100智能地址解析：Provider 能力开关与租户级加密 Secret
ALTER TABLE `rental_logistics_provider_config`
    ADD COLUMN `address_parse_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用智能地址解析' AFTER `subscribe_enabled`;

ALTER TABLE `rental_logistics_provider_credential`
    ADD COLUMN `api_secret` varchar(512) NULL COMMENT 'API Secret（应用层加密）' AFTER `api_key`;
