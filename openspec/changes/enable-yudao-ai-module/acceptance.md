# Acceptance Criteria: enable-yudao-ai-module

## User-Visible Criteria

- 生产超管登录管理后台后可以看到完整“AI 大模型”菜单。
- API 密钥、模型配置、聊天角色、AI 对话、知识库、工作流、写作、绘图、音乐和
  思维导图页面可以打开，不再因 AI 表缺失返回系统异常。
- OpenAI-compatible relay 和 `gpt-5.6-luna` 可通过管理端配置，不需要改源码或
  重启服务来更换 Key。
- 未授权角色不会自动获得 AI 密钥和模型管理能力。
- 原有订单、设备退回、物流和管理后台功能在部署后继续可用。

## System Criteria

- 根 Maven reactor 和 `yudao-server` 都实际包含 `yudao-module-ai`。
- 当前 AI 源码引用的全部 `ai_*` 表在生产数据库存在，字段和索引与 MySQL 基线
  兼容。
- AI 字典、菜单、按钮权限和超管角色菜单关系完整且无重复。
- 迁移通过现有 runner 记录 migration ID、SHA-256 checksum 和 release SHA。
- 重复部署不会重复建表、重复插入菜单或破坏现有 AI 数据。
- `AiApiKeyDO.apiKey` 使用 `EncryptTypeHandler`，新写入 Key 的数据库值不等于
  明文，应用读取后仍能成功调用模型。
- 生产缺少 `MYBATIS_PLUS_ENCRYPTOR_PASSWORD` 时部署不得继续配置真实 Key。

## Data Criteria

- migration 仅新增缺失 AI 结构和种子数据，不删除或覆盖租赁业务数据。
- migration 前有数据库结构与受影响菜单/角色授权备份。
- API Key 不出现在 Git diff、migration、构建日志或验证回执。
- 迁移后表数量、菜单数量、权限数量和角色授权数量有确定性查询证据。

## Verification Surfaces

- Facticity: Maven 模块、AI DO、MySQL 基线、生产 schema、角色和部署 runner。
- Static: `git diff --check`、敏感信息扫描、SQL 结构覆盖检查。
- Unit: AI API Key TypeHandler 映射和 AI 模块现有单元测试。
- Migration: 空目标库执行、生产预检、runner 首次执行与重复跳过。
- E2E: 管理员登录、动态菜单、API Key 创建、模型创建、模型测试或聊天调用。
- Regression: 后端健康检查、固定设备退回页面和核心租赁管理接口。

## Unresolved Gaps

- 无。
