# Acceptance Criteria: add-customer-return-registration

## User-Visible Criteria

- 运营人员可以复制统一入口 `https://rental.motion-cover.com/return`，不需要为
  每个客户创建、续期或复制专属链接。
- 客户在微信或移动浏览器打开固定入口，在同一个页面填写闲鱼订单号、收件手机号
  后四位、机器编码和快递单号后直接提交，不需要登录管理后台。
- 闲鱼订单号与手机号后四位均可留空；机器编码和快递单号必填。
- 页面仅用于捷租达；机器编码无需预先入库，无法自动匹配时仍保存登记并显示等待
  人工复核。
- 验证失败只显示统一错误，不暴露订单是否存在、手机号是否缺失、租户、订单状态
  或设备分配情况。
- 客户无需选择承运商、填写寄出日期、上传照片或经过步骤确认；机器编码中的小写
  和全角连字符会被安全规范化，非法格式在字段旁提示。
- 机器编码统一显示和校验为 `P4-01` 这类“型号前缀-两位序号”短码。
- 重复点击或网络重试只得到同一登记回执。
- 会话过期后客户可重新验证；已提交和需要人工复核的订单重新验证后显示原回执，
  不创建重复登记。
- 管理端可以分页检索登记，查看完整订单授权信息、序列号匹配、照片和审核历史。
- light/dark、`zh-CN`/`en`、360-430 px 移动端和桌面均无页面级横向溢出。

## System Criteria

- 固定 URL 不包含 token、订单号、手机号或登记 ID。
- 验证成功后自动签发至少 256 bit 随机会话，只通过 `Secure`、`HttpOnly`、
  `SameSite=Lax` Cookie 传输；数据库只保存 SHA-256 hash，hash 全局唯一。
- 公共订单验证固定在配置的捷租达租户内。使用机器编码、可选手机号后四位和可选
  订单号定位唯一渠道订单；仅机器编码或后四位存在多个候选时不得自动选择。
- 仅填写机器编码和快递单号且机器编码未入库时，创建租赁订单、渠道订单、设备和
  分配均为空的 `REVIEW_REQUIRED` 登记，并保留提交的机器编码和快递单号。
- 单次提交完成订单定位、短时会话签发和事务提交，并校验状态、到期时间、订单
  绑定、运单和设备。
- 序列号只能自动匹配当前订单已分配设备；跨租户、跨订单、重复或未知序列号不会
  被错误绑定。
- 已发出但尚未回仓入库的 `DISPATCHED` 设备可以通过机器编码登记退回。
- 自动匹配成功时创建或复用 `direction=RETURN`、来源
  `CUSTOMER_RETURN_FORM` 的 Delivery；相同业务键不会重复创建。
- 无法安全自动处理时保存为 `REVIEW_REQUIRED`，管理端接受时重新执行全部校验。
- 未入库机器编码不创建或修改 `rental_device`、`rental_device_assignment` 和
  `rental_delivery`。
- 客户提交和审核接受都不会自动释放设备、完成检测、关闭订单或修改排期。
- 验证接口具备 IP 与订单/手机号/机器编码摘要限流，后续接口具备会话摘要限流；日志和错误不泄露
  验证参数、会话值、PII、签名 URL 或密钥。

## Data Criteria

- 增量 migration 新建三张登记表，不改写历史 migration；设备短码迁移只更新
  `rental_device.device_no` 并将旧值完整保存在兼容字段。
- `token_hash` 作为自动会话 hash 保持全局唯一；表内设备序列号和附件关系分别
  保持幂等唯一。
- `rental_return_registration_attachment.file_id` 只引用当前会话登记已确认的
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
- Unit: 仅机器编码定位、手机号后四位独立验证、可选订单号消歧、自动会话、序列号规范化、状态/到期、设备匹配、
  附件策略、幂等提交和审核。
- Redteam: 订单枚举、手机号后四位爆破、会话固定/重放、IDOR、跨租户 fileId、
  伪造上传确认、超限文件、重复提交、PII/签名 URL 泄露和设备自动释放尝试。
- E2E: 固定入口四字段单次提交、自动创建/恢复登记、数据库落库、管理端查看和
  审核到 Return Delivery。
- Sensory: 表单空闲/加载/失败、移动/桌面、浅色/深色、中英文、已提交、复核和
  成功状态。

## Unresolved Gaps

- 无。
