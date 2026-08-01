# Acceptance Criteria: add-customer-return-registration

## User-Visible Criteria

- 运营人员可以从管理端为可退回订单创建有效期明确的分享链接，复制给客户，并能
  撤销或重新签发。
- 客户在微信或移动浏览器打开有效链接后看到安全订单摘要和五步登记流程，不需要
  登录管理后台。
- 客户可填写承运商、运单号、寄出日期和 1-8 个设备短序列号；小写和全角连字符
  会被安全规范化，非法格式在对应字段旁提示。
- 客户必须上传设备外观和序列号铭牌照片，打包状态照片可不上传；未上传打包照片
  仍可完成提交。
- 上传过程显示进度、失败和重试，失败不会清空已填写的其他内容。
- 提交前显示完整复核摘要；重复点击或网络重试只得到同一登记回执。
- 过期、撤销、无效、已提交和需要人工复核的链接有独立、安全、可操作的页面。
- 管理端可以分页检索登记，查看完整订单授权信息、序列号匹配、照片和审核历史。
- light/dark、`zh-CN`/`en`、360-430 px 移动端和桌面均无页面级横向溢出。

## System Criteria

- 公共 token 至少 256 bit，数据库只保存 SHA-256 hash，hash 全局唯一。
- token 查询在读取任何租户业务数据前解析并恢复正确租户上下文。
- 最终提交事务锁定登记行，并校验状态、到期时间、订单绑定、运单、设备和附件。
- 序列号只能自动匹配当前订单已分配设备；跨租户、跨订单、重复或未知序列号不会
  被错误绑定。
- 自动匹配成功时创建或复用 `direction=RETURN`、来源
  `CUSTOMER_RETURN_FORM` 的 Delivery；相同业务键不会重复创建。
- 无法安全自动处理时保存为 `REVIEW_REQUIRED`，管理端接受时重新执行全部校验。
- 客户提交和审核接受都不会自动释放设备、完成检测、关闭订单或修改排期。
- 公共接口具备 token 范围限流，日志和错误不泄露 token、PII、签名 URL 或密钥。

## Data Criteria

- 增量 migration 新建三张登记表，不改写历史 migration 和现有业务数据。
- `share_token_hash` 全局唯一；表内设备序列号和附件关系分别保持幂等唯一。
- `rental_return_registration_attachment.file_id` 只引用当前 token 已确认的
  `infra_file`，不能引用其他租户或登记文件。
- RustFS Bucket 私有且禁止匿名列举/读写；对象 key 不含 PII、订单号或序列号。
- migration 在空库和当前生产结构升级场景均可执行，并有验证与回滚说明。

## Component Criteria

- Reusable components, hooks, utilities, or services named in
  `component-impact-map.json` are extracted instead of duplicated.
- Controller 不直接访问 Mapper，Nuxt 与管理端不计算权威设备匹配或状态转换。
- 客户页面、管理页面和后端新增源文件不超过 600 行；达到 450 行时优先拆分。
- 公共上传复用现有基础设施文件服务和 S3 客户端，不建立第二套文件存储元数据。

## Verification Surfaces

- Facticity: 当前订单/设备/Delivery/infra_file 模型、公共安全白名单、生产部署路径。
- Static: `git diff --check`、Maven 编译、Vue/TypeScript 检查、Nuxt 生产构建、
  migration 和敏感信息扫描。
- Unit: token、序列号规范化、状态/到期、设备匹配、附件策略、幂等提交和审核。
- Redteam: token 枚举、IDOR、跨租户 fileId、伪造上传确认、超限文件、重复提交、
  PII/签名 URL 泄露和设备自动释放尝试。
- E2E: 创建链接、公共读取、RustFS 上传、确认文件、提交、数据库落库、管理端查看
  和审核到 Return Delivery。
- Sensory: 移动/桌面、浅色/深色、中英文、上传失败、过期、撤销、已提交、复核和
  成功状态。

## Unresolved Gaps

- 无。
