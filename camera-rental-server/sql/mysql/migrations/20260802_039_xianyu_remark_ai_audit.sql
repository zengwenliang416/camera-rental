ALTER TABLE `xianyu_order`
  ADD COLUMN `remark_parse_source` varchar(16) DEFAULT NULL COMMENT '备注解析来源 RULE/AI' AFTER `remark_parse_status`,
  ADD COLUMN `remark_parse_confidence` decimal(5,4) DEFAULT NULL COMMENT 'AI 解析置信度' AFTER `remark_parse_source`,
  ADD COLUMN `remark_parse_model` varchar(255) DEFAULT NULL COMMENT 'AI 解析模型' AFTER `remark_parse_confidence`,
  ADD COLUMN `remark_parse_evidence_json` text COMMENT '不含原文的证据字段 JSON' AFTER `remark_parse_model`;

CREATE INDEX `idx_xianyu_order_tenant_remark_parse`
  ON `xianyu_order` (`tenant_id`, `rental_period_status`, `remark_parse_source`, `id`);
