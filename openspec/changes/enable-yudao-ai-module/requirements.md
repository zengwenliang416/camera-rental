# Requirements: enable-yudao-ai-module

## Summary

启用仓库中现有的完整 `yudao-module-ai` 后端实现，并通过增量、可审计的 MySQL
migration 为当前相机租赁生产库补齐 AI 表、字典、菜单和管理员角色授权。现有
管理端 AI 页面保持不变，通过后端动态菜单和权限开放。

AI API Key 必须使用项目现有 `EncryptTypeHandler` 和生产
`MYBATIS_PLUS_ENCRYPTOR_PASSWORD` 加密保存。真实 Key 不进入 Git、migration、
构建日志或普通应用日志。OpenAI 兼容中转站和 `gpt-5.6-luna` 模型在部署后通过
受权限保护的管理 API 配置。

## Users & Actors

- 租户管理员：管理 AI API Key、模型、角色、知识库、工作流和生成能力。
- 运营人员：在授权范围内使用 AI 对话、写作、思维导图等功能。
- camera-rental-server：加载完整 AI 模块、执行权限和租户隔离、加密持久化 Key。
- MySQL：保存 AI 模块业务数据、动态菜单、字典和迁移审计。
- OpenAI-compatible relay：提供 `gpt-5.6-luna` 模型调用。

## In Scope

- 在根 Maven reactor 中启用 `yudao-module-ai`。
- 在 `yudao-server` 中启用 `yudao-module-ai` 运行时依赖。
- 创建增量、幂等 MySQL migration，补齐当前源码所需全部 `ai_*` 表。
- 补齐完整 AI 字典类型、字典数据、动态菜单和菜单权限。
- 将 AI 菜单授权给生产超管角色，不扩大普通角色权限。
- 为 `AiApiKeyDO.apiKey` 接入 MyBatis AES TypeHandler，数据库不保存新写入的
  明文 Key。
- 迁移前生成结构和受影响授权数据备份；迁移后验证表、索引、菜单、权限和迁移
  记录。
- 构建完整后端 JAR，部署到 `154.9.235.80`，保留既有设备退回服务。
- 通过受保护管理 API 配置 OpenAI-compatible relay 与 `gpt-5.6-luna`，并执行
  不泄露 Key 的模型连通性验证。

## Out of Scope

- 重写现有 AI 管理端页面或引入第二套 AI 前端。
- 自动把所有租赁备注发送给 AI，或在本变更中修改备注解析业务规则。
- 将 API Key 写入 migration、源码、前端、CI 日志或普通配置文件。
- 给非管理员角色自动授予 AI 配置、密钥或模型管理权限。
- 启用未配置外部服务的绘图、音乐、向量库或 MCP 能力并伪装为可用。
- 删除、覆盖或重放现有租赁、设备退回和物流数据。

## Security & Tenant Requirements

- `AiApiKeyDO.apiKey` 使用 `EncryptTypeHandler`，并启用 `autoResultMap`。
- 生产启动前必须确认 `MYBATIS_PLUS_ENCRYPTOR_PASSWORD` 非空。
- Key 创建、更新、查询和删除继续受 `ai:api-key:*` 权限保护。
- API 响应不得向无权限用户暴露 Key；日志不得记录 Key、Authorization 或完整
  中转站请求头。
- AI 业务表遵循现有租户、审计和逻辑删除约定。
- 超管角色授权必须按菜单主键幂等写入，不能删除既有角色菜单关系。

## Architecture & Database Impact

- 影响 `camera-rental-server/pom.xml`、`camera-rental-server/yudao-server/pom.xml`、
  `camera-rental-server/yudao-module-ai`、MySQL migration 和 GitHub 部署清单。
- 数据库结构以当前源码 `@TableName` 和仓库 MySQL 基线为准；migration 只能新增
  缺失结构和种子数据，不修改已执行历史 migration。
- migration 必须由现有 `camera_rental_schema_migration` runner 管理校验和。
- 部署顺序固定为备份、应用 migration、切换 release、重启服务、健康检查、接口
  验证；失败时保持或恢复上一 release。

## Frontend-Backend Data Flow Impact

- 现有 `camera-rental-admin/src/views/ai/**` 和 `src/api/ai/**` 继续调用
  `/admin-api/ai/**`。
- 菜单由 `system_menu` 和角色授权动态返回；前端不保存 API Key。
- 模型配置通过 `AI API Key -> AI Model -> AI Chat Role/业务功能` 关联。
- 中转站配置只从受保护管理 API 进入后端，再由 TypeHandler 加密写入 MySQL。

## Verification

- Maven 编译完整 reactor，并执行 AI 模块相关单元测试。
- 在隔离 MySQL 库验证 migration 可首次执行、重复执行由 runner 跳过且校验和稳定。
- 静态验证全部 `@TableName("ai_*")` 均有对应生产表。
- 生产验证服务 `active`、健康接口成功、AI 菜单可见、受保护 AI API 不再因缺表
  失败、数据库 Key 非明文。
- 直接 relay 探测与管理端模型探测分开记录；只有端到端调用成功才声明模型可用。

## Unresolved Gaps

- 无。
