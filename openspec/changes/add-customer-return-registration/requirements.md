# Requirements: add-customer-return-registration

## Summary

为客户提供固定、独立的设备退回登记网页 `/return`。客户输入闲鱼订单号和收件
手机号后四位完成订单归属验证后，确认安全的订单摘要，填写寄回承运商、运单号、
寄出日期和一个或多个设备短序列号，上传设备外观与序列号铭牌照片，可选上传打包
或异常照片，最后提交并获得登记回执。

系统在服务端校验订单号、手机号后四位、短时公共会话、订单、设备、文件和幂等性。
验证通过时创建或恢复订单对应的退回登记，并创建或复用
`direction=RETURN` 的 `rental_delivery`；无法自动匹配时保留完整登记并进入人工
复核。客户提交绝不直接释放设备、完成订单、通过检测或修改排期。

## Users & Actors

- 客户：通过固定网页验证订单后登记退回物流、设备序列号、照片和异常说明。
- 客服/订单运营：复制固定退回入口、检索登记并处理异常或人工复核。
- 仓库/设备运营：查看提交详情、照片和设备匹配结果，处理人工复核。
- 租户管理员：管理菜单权限、RustFS 对应的文件配置和生产运行状态。
- camera-rental-server：校验订单号、手机号后四位、公共会话、文件所有权、订单
  关系和幂等提交。
- RustFS：作为现有基础设施文件服务背后的私有 S3 兼容对象存储。

## In Scope

### Public Order Verification and Session

- 固定客户入口为 `/return`，URL 不包含订单号、手机号、登记 ID 或访问 token。
- 客户提交闲鱼订单号和收件手机号后四位；服务端规范化手机号后进行后四位比对，
  不要求输入或回显完整手机号。
- 验证失败统一返回“订单号或手机号后四位不匹配”，不能区分订单不存在、手机号
  缺失、租户不匹配、订单不可退回或设备尚未分配。
- 验证接口按来源 IP 与订单号摘要双重限流；连续失败不得泄露可用于订单枚举的
  时间、字段或状态差异。
- 验证成功后，服务端创建或恢复当前订单的登记，并自动签发至少 256 bit 随机会话。
  会话只通过 `Secure`、`HttpOnly`、`SameSite=Lax` Cookie 传输，URL、页面状态和
  日志中不得出现明文会话值；数据库只保存 SHA-256 hash。
- 公共会话最长 24 小时。过期草稿允许客户重新验证并自动创建新草稿；已经提交或
  正在复核的订单重新验证后只返回现有回执，不重复创建登记。
- 历史分享网页统一安全重定向到固定入口；已有 token hash 和登记数据保留供管理端
  审计，但旧 token 不再作为客户访问或提交凭据。管理端不再提供创建或重签专属
  链接的接口。
- 公共读取只返回表单所需的安全订单摘要，不返回租户 ID、内部订单 ID、完整地址、
  完整手机号、第三方凭据或原始渠道 JSON。

### Customer Submission

- 客户流程固定为订单确认、寄回物流、设备序列号、归还照片、确认提交。
- 公共会话已经绑定验证通过的订单；提交中的订单号只用于二次确认，不允许切换订单。
- 承运商、运单号和寄出日期必填。运单号规范化后用于同订单退回包裹幂等。
- 至少填写一个设备序列号，最多 8 个；序列号支持小写、空格、全角/长连字符输入，
  服务端统一规范为大写 ASCII 连字符格式，例如 `A6-08-4L5H`。
- 每个规范化序列号在同一次登记内唯一，并必须匹配当前订单已分配的设备；未匹配、
  重复、跨订单或跨租户设备进入人工复核，不静默绑定。
- 设备外观和序列号铭牌各至少 1 张；打包状态非必拍；异常说明和异常照片可选。
- 每类最多 6 张，总数最多 20 张；单文件最大 15 MiB；允许 JPEG、PNG、WebP，
  HEIC 仅在服务端确认可安全处理时开放。
- 客户提交使用稳定幂等键。同一登记和幂等键的重复提交返回第一次成功的回执，不创建
  重复设备行、附件关系或 Delivery。

### File Upload and RustFS

- 复用 `yudao-module-infra` 的 `infra_file`、`FileService` 和 S3 客户端，不建立
  第二套文件元数据表或通用公开上传接口。
- 新增公共会话限定的上传授权和确认接口。授权只能写入当前登记的随机对象路径，
  短期有效，不能列桶、覆盖其他对象或访问其他登记文件。
- 上传确认校验公共会话、对象 key、Content-Type、实际文件大小、文件签名和登记
  归属后创建 `infra_file` 记录并返回 `fileId`。
- `rental_return_registration_attachment` 只保存 `infra_file.id`、类别、顺序和
  安全元数据；对象 key 不包含客户姓名、手机号、地址、订单号或设备序列号。
- Bucket 私有；客户和管理端预览使用短时签名 URL 或鉴权下载接口。
- RustFS 控制台不公开，数据与元数据使用持久卷、健康检查、自动重启和备份策略。

### Submission and Review

- 最终提交在事务中锁定登记行，校验状态、有效期、订单、运单、设备和附件归属。
- 全部必要信息可自动匹配时，创建或复用 `rental_delivery`，方向为 `RETURN`，
  来源为 `CUSTOMER_RETURN_FORM`，并绑定匹配到的设备。
- 已存在相同租户、订单、方向、承运商和规范运单的 Delivery 时复用，不重复创建。
- 任一设备无法安全匹配、订单关系不完整或存在需要人工判断的异常时，登记状态为
  `REVIEW_REQUIRED`，不创建错误绑定。
- 管理端展示固定公开入口，并支持按状态、订单号、运单号、序列号和提交时间分页
  查询；不要求运营人员逐单创建链接。
- 管理端详情显示完整登记、订单授权范围内的客户信息、设备匹配结果和照片预览。
- 运营人员可接受或驳回待复核登记；接受时再次执行全部服务端校验并创建/复用
  Return Delivery，驳回保留原因和审计信息。
- 任何客户提交或人工接受都不改变设备状态、assignment 状态、订单完成状态、
  回仓检测结果或排期释放；这些仍由既有仓库/检测流程负责。

### Security and Operations

- 订单验证接口按 IP 与订单摘要限流，后续公共接口按会话摘要限流；验证、提交和
  上传确认记录安全审计。日志不得输出完整订单验证参数、会话值、完整手机号、
  地址、完整运单号、文件签名 URL 或对象存储密钥。
- 管理接口使用独立权限：
  `rental:return-registration:query`、`create`、`revoke`、`review`。
- 所有业务表遵循租户、审计和逻辑删除约定；公共订单验证唯一定位渠道订单后显式
  恢复租户上下文，公共会话 hash 查询也必须先恢复租户上下文再访问业务数据。
- 数据库迁移为增量 SQL，不修改已执行的历史迁移；提供验证与回滚说明。
- GitHub `main` 推送触发 `154.9.235.80` 自动部署；部署必须执行待应用迁移、重启后端和
  Nuxt Web，并验证公开页面、上传、数据库行和管理端详情。

## Out of Scope

- 客户自行修改已经提交的登记；修改由运营人员人工处理。
- 短信验证码、微信授权登录、身份证验证或仅凭订单号直接开放登记。
- 客户查看完整订单详情、完整收货地址、手机号、支付信息或渠道原始数据。
- 物流签收后自动将设备设为可用、自动完成检测、自动关闭订单或释放排期。
- 客户页面直接访问 RustFS 管理 API、使用长期对象存储凭据或公开 Bucket。
- OCR 自动读取序列号、运单和照片内容；第一版只做人工输入与服务端校验。
- 病毒查杀、HEIC 转码和 EXIF 清理在基础设施能力不可用时不伪装为已完成。
- 修改闲管家发货写接口或快递100 Provider 配置所有权。

## UI Design Impact

- Foundation spec: `openspec/specs/ui-design/design.md`
- 客户 Nuxt 页面采用已批准原型的暖色、服务型、移动优先视觉，固定入口先显示
  订单号与手机号后四位验证区，再进入原五步登记流程，支持 360-430 px 微信浏览器
  和桌面布局。
- 复用五步进度、48-54 px 控件、照片清单、序列号技术字体、移动端固定操作区。
- 必须覆盖 verification-idle、verification-loading、verification-failed、
  loading、expired、revoked、already-submitted、validation-error、
  upload-progress、upload-failure、review-required 和 success。
- 状态使用文字加图形语义，保证键盘焦点、44 px 触控目标和无页面级横向溢出。

## Theme & Locale Capability Impact

- Theme support: `light-dark`。
- Theme toggle policy: `theme-toggle:user`，复用 Nuxt 站点统一偏好，不在表单内
  建立第二套状态源。
- Internationalization: `enabled`。
- Supported locales: `zh-CN,en`。
- Default locale: `zh-CN`，fallback 为 `zh-CN`。
- Prototype coverage: 已批准中文浅色主流程；生产验收补齐 light/dark、
  `zh-CN`/`en`、桌面/移动和全部异常状态。

## Architecture & Database Impact

- Foundation spec: `openspec/specs/system-architecture/design.md`
- 影响 `camera-rental-server/yudao-module-rental`、`yudao-module-infra` API 复用、
  `camera-rental-web`、`camera-rental-admin`、MySQL migration 和部署脚本。
- 新增 `rental_return_registration`、`rental_return_registration_device`、
  `rental_return_registration_attachment` 三张表。
- 自动签发的公共会话 hash 继续使用现有 `token_hash` 全局唯一索引；订单、状态、
  到期时间、运单、设备和附件查询建立租户范围索引。
- 后端 Controller、Service、Mapper、VO 分层；Controller 不直接访问 Mapper。
- 文件元数据继续由 `infra_file` 权威管理，RustFS 只作为 S3 存储实现。

## Frontend-Backend Data Flow Impact

- Foundation spec: `openspec/specs/frontend-backend-data-flow/design.md`
- 用 `FLOW-PUBLIC-RETURN-VERIFY` 取代人工链接签发流，并保留
  `FLOW-PUBLIC-RETURN-CONTEXT`、`FLOW-PUBLIC-RETURN-UPLOAD`、
  `FLOW-PUBLIC-RETURN-SUBMIT`、`FLOW-RETURN-REGISTRATION-REVIEW`。
- Nuxt 只保存步骤草稿、上传进度和安全响应；订单、设备匹配、文件归属、状态和
  Delivery 由后端决定。
- 上传采用“订单验证 -> HttpOnly 会话 -> 获取授权 -> 直传私有 S3 -> 服务端确认
  -> 返回 fileId”，最终提交只接受当前会话登记已确认的 fileId。
- 管理端使用 `/admin-api/rental/return-registration/**`，客户页面使用
  `/app-api/rental/return-registration/**`。

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`
- 后端提取订单验证、HttpOnly 会话、序列号规范化、订单/设备匹配、附件策略和
  提交编排服务。
- Nuxt 页面拆分步骤外壳、物流表单、设备列表、照片上传、复核与状态页面；页面
  不自行实现权威匹配规则。
- 管理端复用现有表格、分页、表单、抽屉、图片预览、权限和 API 请求组件。
- 新增或重构的生产源文件原则上不超过 450 行，任何文件达到 600 行前必须拆分。

## Unresolved Gaps

- 无。
