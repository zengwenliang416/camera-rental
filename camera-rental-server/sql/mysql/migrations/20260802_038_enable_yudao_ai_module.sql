-- Enable the complete yudao-module-ai persistence model.
-- API keys are intentionally not seeded here. They must be written through the
-- authenticated admin API so EncryptTypeHandler stores ciphertext.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_api_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `api_key` varchar(2048) NOT NULL COMMENT '加密后的 API Key',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `url` varchar(2048) DEFAULT NULL COMMENT 'API 地址',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_api_key_tenant_platform_status` (`tenant_id`, `platform`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI API 密钥';

CREATE TABLE IF NOT EXISTS `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `key_id` bigint NOT NULL COMMENT 'API 密钥编号',
  `name` varchar(255) NOT NULL COMMENT '模型名称',
  `model` varchar(255) NOT NULL COMMENT '模型标识',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `type` tinyint NOT NULL COMMENT '模型类型',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `temperature` double DEFAULT NULL COMMENT '温度',
  `max_tokens` int DEFAULT NULL COMMENT '最大 Token 数',
  `max_contexts` int DEFAULT NULL COMMENT '最大上下文消息数',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_model_tenant_type_status_sort` (`tenant_id`, `type`, `status`, `sort`, `deleted`),
  KEY `idx_ai_model_tenant_key` (`tenant_id`, `key_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型';

CREATE TABLE IF NOT EXISTS `ai_chat_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `name` varchar(255) NOT NULL COMMENT '角色名称',
  `avatar` varchar(2048) DEFAULT NULL COMMENT '角色头像',
  `category` varchar(255) DEFAULT NULL COMMENT '角色分类',
  `description` text COMMENT '角色描述',
  `system_message` longtext COMMENT '角色设定',
  `user_id` bigint DEFAULT NULL COMMENT '用户编号',
  `model_id` bigint DEFAULT NULL COMMENT '模型编号',
  `knowledge_ids` text COMMENT '知识库编号 JSON',
  `tool_ids` text COMMENT '工具编号 JSON',
  `mcp_client_names` text COMMENT 'MCP Client 名称 JSON',
  `public_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_role_tenant_public_status_sort` (`tenant_id`, `public_status`, `status`, `sort`, `deleted`),
  KEY `idx_ai_chat_role_tenant_user` (`tenant_id`, `user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天角色';

CREATE TABLE IF NOT EXISTS `ai_chat_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `title` varchar(255) NOT NULL DEFAULT '新对话' COMMENT '对话标题',
  `pinned` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶',
  `pinned_time` datetime DEFAULT NULL COMMENT '置顶时间',
  `role_id` bigint DEFAULT NULL COMMENT '角色编号',
  `model_id` bigint NOT NULL COMMENT '模型编号',
  `model` varchar(255) NOT NULL COMMENT '模型标识',
  `system_message` longtext COMMENT '角色设定',
  `temperature` double DEFAULT NULL COMMENT '温度',
  `max_tokens` int DEFAULT NULL COMMENT '最大 Token 数',
  `max_contexts` int DEFAULT NULL COMMENT '最大上下文消息数',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_conversation_tenant_user_pinned` (`tenant_id`, `user_id`, `pinned`, `deleted`),
  KEY `idx_ai_chat_conversation_tenant_created` (`tenant_id`, `create_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天对话';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `conversation_id` bigint NOT NULL COMMENT '对话编号',
  `reply_id` bigint DEFAULT NULL COMMENT '回复消息编号',
  `type` varchar(32) NOT NULL COMMENT '消息类型',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `role_id` bigint DEFAULT NULL COMMENT '角色编号',
  `model` varchar(255) DEFAULT NULL COMMENT '模型标识',
  `model_id` bigint DEFAULT NULL COMMENT '模型编号',
  `content` longtext COMMENT '聊天内容',
  `reasoning_content` longtext COMMENT '推理内容',
  `use_context` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否携带上下文',
  `segment_ids` text COMMENT '知识库分段编号 JSON',
  `web_search_pages` longtext COMMENT '联网搜索结果 JSON',
  `attachment_urls` longtext COMMENT '附件 URL JSON',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_message_tenant_conversation` (`tenant_id`, `conversation_id`, `id`, `deleted`),
  KEY `idx_ai_chat_message_tenant_user_created` (`tenant_id`, `user_id`, `create_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天消息';

CREATE TABLE IF NOT EXISTS `ai_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `prompt` longtext NOT NULL COMMENT '提示词',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `model_id` bigint NOT NULL COMMENT '模型编号',
  `model` varchar(255) NOT NULL COMMENT '模型标识',
  `width` int DEFAULT NULL COMMENT '宽度',
  `height` int DEFAULT NULL COMMENT '高度',
  `status` tinyint NOT NULL COMMENT '生成状态',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `error_message` text COMMENT '错误信息',
  `pic_url` varchar(2048) DEFAULT NULL COMMENT '图片地址',
  `public_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `options` longtext COMMENT '绘制参数 JSON',
  `buttons` longtext COMMENT 'Midjourney 按钮 JSON',
  `task_id` varchar(255) DEFAULT NULL COMMENT '任务编号',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_image_tenant_user_created` (`tenant_id`, `user_id`, `create_time`, `deleted`),
  KEY `idx_ai_image_tenant_platform_status` (`tenant_id`, `platform`, `status`, `deleted`),
  KEY `idx_ai_image_tenant_public_created` (`tenant_id`, `public_status`, `create_time`, `deleted`),
  KEY `idx_ai_image_tenant_task` (`tenant_id`, `task_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 绘图';

CREATE TABLE IF NOT EXISTS `ai_music` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `title` varchar(512) DEFAULT NULL COMMENT '音乐名称',
  `lyric` longtext COMMENT '歌词',
  `image_url` varchar(2048) DEFAULT NULL COMMENT '图片地址',
  `audio_url` varchar(2048) DEFAULT NULL COMMENT '音频地址',
  `video_url` varchar(2048) DEFAULT NULL COMMENT '视频地址',
  `status` tinyint NOT NULL COMMENT '音乐状态',
  `generate_mode` tinyint NOT NULL COMMENT '生成模式',
  `description` longtext COMMENT '描述词',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `model` varchar(255) DEFAULT NULL COMMENT '模型',
  `tags` text COMMENT '音乐风格 JSON',
  `duration` double DEFAULT NULL COMMENT '时长',
  `public_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `task_id` varchar(255) DEFAULT NULL COMMENT '任务编号',
  `error_message` text COMMENT '错误信息',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_music_tenant_user_created` (`tenant_id`, `user_id`, `create_time`, `deleted`),
  KEY `idx_ai_music_tenant_status` (`tenant_id`, `status`, `deleted`),
  KEY `idx_ai_music_tenant_public_created` (`tenant_id`, `public_status`, `create_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 音乐';

CREATE TABLE IF NOT EXISTS `ai_write` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `type` tinyint NOT NULL COMMENT '写作类型',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `model_id` bigint NOT NULL COMMENT '模型编号',
  `model` varchar(255) NOT NULL COMMENT '模型',
  `prompt` longtext NOT NULL COMMENT '提示词',
  `generated_content` longtext COMMENT '生成内容',
  `original_content` longtext COMMENT '原文',
  `length` tinyint DEFAULT NULL COMMENT '长度',
  `format` tinyint DEFAULT NULL COMMENT '格式',
  `tone` tinyint DEFAULT NULL COMMENT '语气',
  `language` tinyint DEFAULT NULL COMMENT '语言',
  `error_message` text COMMENT '错误信息',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_write_tenant_user_created` (`tenant_id`, `user_id`, `create_time`, `deleted`),
  KEY `idx_ai_write_tenant_type_platform` (`tenant_id`, `type`, `platform`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 写作';

CREATE TABLE IF NOT EXISTS `ai_mind_map` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `model_id` bigint NOT NULL COMMENT '模型编号',
  `model` varchar(255) NOT NULL COMMENT '模型',
  `prompt` longtext NOT NULL COMMENT '提示词',
  `generated_content` longtext COMMENT '生成内容',
  `error_message` text COMMENT '错误信息',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_mind_map_tenant_user_created` (`tenant_id`, `user_id`, `create_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 思维导图';

CREATE TABLE IF NOT EXISTS `ai_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `name` varchar(255) NOT NULL COMMENT '知识库名称',
  `description` text COMMENT '知识库描述',
  `embedding_model_id` bigint NOT NULL COMMENT '向量模型编号',
  `embedding_model` varchar(255) NOT NULL COMMENT '向量模型标识',
  `top_k` int NOT NULL DEFAULT 5 COMMENT '召回数量',
  `similarity_threshold` double NOT NULL DEFAULT 0 COMMENT '相似度阈值',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_knowledge_tenant_status_created` (`tenant_id`, `status`, `create_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 知识库';

CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `knowledge_id` bigint NOT NULL COMMENT '知识库编号',
  `name` varchar(512) NOT NULL COMMENT '文档名称',
  `url` varchar(2048) NOT NULL COMMENT '文件 URL',
  `content` longtext COMMENT '文档内容',
  `content_length` int NOT NULL DEFAULT 0 COMMENT '内容长度',
  `tokens` int NOT NULL DEFAULT 0 COMMENT 'Token 数',
  `segment_max_tokens` int NOT NULL DEFAULT 0 COMMENT '分片最大 Token 数',
  `retrieval_count` int NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_knowledge_document_tenant_knowledge` (`tenant_id`, `knowledge_id`, `id`, `deleted`),
  KEY `idx_ai_knowledge_document_tenant_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 知识库文档';

CREATE TABLE IF NOT EXISTS `ai_knowledge_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `knowledge_id` bigint NOT NULL COMMENT '知识库编号',
  `document_id` bigint NOT NULL COMMENT '文档编号',
  `content` longtext NOT NULL COMMENT '切片内容',
  `content_length` int NOT NULL DEFAULT 0 COMMENT '内容长度',
  `vector_id` varchar(255) DEFAULT NULL COMMENT '向量库编号',
  `tokens` int NOT NULL DEFAULT 0 COMMENT 'Token 数',
  `retrieval_count` int NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_knowledge_segment_tenant_document` (`tenant_id`, `document_id`, `id`, `deleted`),
  KEY `idx_ai_knowledge_segment_tenant_knowledge_status` (`tenant_id`, `knowledge_id`, `status`, `deleted`),
  KEY `idx_ai_knowledge_segment_tenant_vector` (`tenant_id`, `vector_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 知识库分段';

CREATE TABLE IF NOT EXISTS `ai_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `name` varchar(255) NOT NULL COMMENT '工具名称',
  `description` text COMMENT '工具描述',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_tool_tenant_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 工具';

CREATE TABLE IF NOT EXISTS `ai_workflow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `name` varchar(255) NOT NULL COMMENT '工作流名称',
  `code` varchar(255) NOT NULL COMMENT '工作流标识',
  `graph` longtext NOT NULL COMMENT '工作流模型 JSON',
  `remark` varchar(1024) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_workflow_tenant_code` (`tenant_id`, `code`, `deleted`),
  KEY `idx_ai_workflow_tenant_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 工作流';

-- Existing production/base SQL already carries AI dictionaries and menus.
-- Grant the complete AI menu tree to tenant administrators without changing
-- ordinary roles or duplicating an existing active relation.
INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
WITH RECURSIVE `ai_menu` AS (
  SELECT `id`
  FROM `system_menu`
  WHERE `path` = '/ai' AND `deleted` = b'0'
  UNION ALL
  SELECT child.`id`
  FROM `system_menu` child
  INNER JOIN `ai_menu` parent ON child.`parent_id` = parent.`id`
  WHERE child.`deleted` = b'0'
)
SELECT role.`id`, menu.`id`, '1', NOW(), '1', NOW(), b'0', role.`tenant_id`
FROM `system_role` role
CROSS JOIN `ai_menu` menu
WHERE role.`code` = 'tenant_admin'
  AND role.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = role.`id`
      AND existing.`menu_id` = menu.`id`
      AND existing.`deleted` = b'0'
  );
