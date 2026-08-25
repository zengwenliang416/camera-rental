# Requirements: add-customer-return-registration

## Summary

为捷租达客户提供固定、独立的设备退回登记网页 `/return`。客户在一个页面填写闲鱼
订单号、收件手机号后四位、机器编码和快递单号后直接提交并获得登记回执。机器编码
和快递单号必填；闲鱼订单号与手机号后四位均为可选。机器编码无需预先存在于设备
库存，无法自动匹配时保存为未绑定登记并进入人工复核。

系统在服务端校验机器编码、可选手机号后四位、可选订单号、短时公共会话、订单、
设备、文件和幂等性。
验证通过时创建或恢复订单对应的退回登记，并创建或复用
`direction=RETURN` 的 `rental_delivery`；无法自动匹配时保留完整登记并进入人工
复核。客户提交绝不直接释放设备、完成订单、通过检测或修改排期。

## Users & Actors

- 客户：通过固定网页验证订单后登记退回物流、设备序列号、照片和异常说明。
- 客服/订单运营：复制固定退回入口、检索登记并处理异常或人工复核。
- 仓库/设备运营：查看提交详情、照片和设备匹配结果，处理人工复核。
- 租户管理员：管理菜单权限、RustFS 对应的文件配置和生产运行状态。
- camera-rental-server：通过机器编码定位订单，并校验可选手机号后四位、可选订单号、
  公共会话、文件所有权、订单关系和幂等提交。
- RustFS：作为现有基础设施文件服务背后的私有 S3 兼容对象存储。

## In Scope

### Public Order Verification and Session

- 固定客户入口为 `/return`，URL 不包含订单号、手机号、登记 ID 或访问 token。
- 固定入口只服务捷租达，公共登记始终写入配置的捷租达租户，不从请求参数选择租户。
- 闲鱼订单号与收件手机号后四位均可留空；均未填写时，服务端使用必填机器编码
  尝试定位当前仍为 `ASSIGNED` 或 `DISPATCHED` 的唯一设备分配及其渠道订单；未
  找到设备或分配时仍允许创建独立人工复核登记。同时填写订单号或手机号时必须匹配，
  不能借由未知机器编码绕过验证。服务端规范化手机号后进行后四位比对，不要求输入
  或回显完整手机号。
  后四位只能定位唯一可退回订单时直接通过；存在多笔同尾号候选时要求客户补充
  订单号，不得静默选择错误订单。机器编码映射到多个候选订单时统一验证失败，不得
  跨租户或静默选择。
- 验证失败统一返回不暴露候选数量的验证失败提示，不能区分订单不存在、手机号
  缺失、租户不匹配、订单不可退回或设备尚未分配。
- 验证接口按来源 IP 与“订单号、手机号后四位或机器编码”摘要双重限流；连续失败
  不得泄露可用于订单枚举的时间、字段或状态差异。
- 验证成功后，服务端创建或恢复当前订单的登记，并自动签发至少 256 bit 随机会话。
  会话只通过 `Secure`、`HttpOnly`、`SameSite=Lax` Cookie 传输，URL、页面状态和
  日志中不得出现明文会话值；数据库只保存 SHA-256 hash。
- 公共会话最长 24 小时。过期草稿允许客户重新验证并自动创建新草稿；已经提交或
  正在复核的订单重新验证后只返回现有回执，不重复创建登记。
- 历史分享网页统一安全重定向到固定入口；已有登记数据保留供管理端审计，旧
  token hash 在客户首次通过新验证时安全轮换为短时会话 hash，不回显旧值。旧
  token 不再作为客户访问或提交凭据，管理端也不再提供创建或重签专属链接的接口。
- 公共读取只返回表单所需的安全订单摘要，不返回租户 ID、内部订单 ID、完整地址、
  完整手机号、第三方凭据或原始渠道 JSON。

### Customer Submission

- 客户只使用一个页面和一次提交，不再经过订单确认、物流、设备、照片和复核步骤。
- 页面只显示四个输入：闲鱼订单号、收件手机号后四位、机器编码、快递单号。
- 闲鱼订单号和收件手机号后四位均可留空；机器编码承担默认订单定位职责，填写
  前两项时仅作为附加一致性校验或消歧信息。
- 机器编码和快递单号必填；机器编码统一为“型号前缀-两位序号”，例如 `P4-01`。
  输入支持小写、空格、全角/长连字符，服务端统一规范为大写 ASCII 连字符格式。
- 退回入口必须接受以下现行业务型号前缀与两位序号组合：
  `360`、`NANO`、`A5`、`A6`、`P3`、`P4`、`P4P`、`ACE`、`X5`、
  `GT`、`G3`、`X300P`、`X200U`、`X300U`、`XT5`、`XT50`、`XS20`、
  `X100VI`、`R50`、`G12`、`G7X2`、`GR3X`、`GR4` 和 `支架`，例如
  `X300U-01`、`GR3X-02`、`支架-01`。新增中文 `支架` 前缀不得放宽为任意
  中文文本；既有符合格式的 ASCII 型号前缀继续兼容。
- 既有设备按租户和型号稳定迁移为两位短码，旧设备编号保留为只读兼容别名；
  新增设备和 ERP 入库继续生成 `01` 至 `99` 的短码。
- 客户无需选择承运商、填写寄出日期、上传照片、填写异常说明或生成幂等键。
  服务端使用待识别承运商、`Asia/Shanghai` 当日和登记/运单业务键补齐内部字段。
- 机器编码无需预先录入 `rental_device`。未入库、未匹配、跨订单或跨租户机器编码
  保存原始值与规范化值，设备和分配外键保持为空，提交进入人工复核且不静默绑定。
- 当前订单中状态为 `ASSIGNED` 或 `DISPATCHED` 的设备均可登记退回，包括之前已
  发出但尚未回仓入库的设备。
- 同一登记和规范化快递单号的重复提交返回第一次成功的回执，不创建重复设备行或
  Delivery。
- 未入库机器编码不得自动创建 `rental_device`、`rental_device_assignment` 或
  `rental_delivery`，不得触发回仓、检测、排期释放或订单完成。

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

- 订单验证接口按 IP 与订单/手机号/机器编码摘要限流，后续公共接口按会话摘要限流；验证、提交和
  上传确认记录安全审计。日志不得输出完整订单验证参数、会话值、完整手机号、
  地址、完整运单号、文件签名 URL 或对象存储密钥。
- 管理接口使用独立权限：
  `rental:return-registration:query`、`create`、`revoke`、`review`。
- 所有业务表遵循租户、审计和逻辑删除约定；公共订单验证通过可选订单信息或机器
  编码唯一定位渠道订单后显式恢复租户上下文，公共会话 hash 查询也必须先恢复
  租户上下文再访问业务数据。
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
- 客户 Nuxt 页面采用已批准原型的暖色、服务型、移动优先视觉，固定入口在同一页
  显示两个可选订单信息字段和两个必填字段，支持 360-430 px 微信浏览器和桌面布局。
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
- `rental_device` 增加可空的旧编号兼容字段，并将当前设备编号迁移为型号两位短码。
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
- 上传采用“机器编码/可选订单信息验证 -> HttpOnly 会话 -> 获取授权 -> 直传私有 S3 -> 服务端确认
  -> 返回 fileId”，最终提交只接受当前会话登记已确认的 fileId。
- 管理端使用 `/admin-api/rental/return-registration/**`，客户页面使用
  `/app-api/rental/return-registration/**`。

## Component Architecture Impact

- Foundation spec: `openspec/specs/component-architecture/design.md`
- 后端提取订单验证、HttpOnly 会话、序列号规范化、订单/设备匹配、附件策略和
  提交编排服务。
- Nuxt 页面使用一个四字段表单与一个状态结果面板；页面不自行实现权威订单或设备
  匹配规则。
- 管理端复用现有表格、分页、表单、抽屉、图片预览、权限和 API 请求组件。
- 新增或重构的生产源文件原则上不超过 450 行，任何文件达到 600 行前必须拆分。

## Unresolved Gaps

- 无。
