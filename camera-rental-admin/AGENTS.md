# Admin — Web 管理后台规则

本目录使用 Vue 3、TypeScript、Element Plus 和 pnpm。

## 按需读取

应用[根规则](../AGENTS.md)，读取 README 相关部分、`package.json`、`pnpm-lock.yaml`、
格式/类型配置与目标页面模式。前端修改读[工程规范的前端章节](../docs/engineering/standards.md#3-前端职责与状态)；
跨端接口再读[API 约定](../docs/api/README.md)，领域文档按任务加载。

## 实现与复用

- 租赁页面从 `src/views/rental/`、契约从 `src/api/rental/` 定位。
  请求复用 `src/config/axios/`；认证、刷新令牌、租户与统一错误不能在页面重新实现。
- 通用 UI 优先查 `src/components/`，业务重复组件放在所属功能附近；
  Vue Composition API 组合业务过程，不把大段 Service 编排复制到多个弹窗。
- 金额/日期/标签/隐私展示先检查 `src/utils/formatter.ts`、`formatTime.ts`、
  `rentalDate.ts`、`rentalLabels.ts`、`rentalPrivacy.ts`；修改公共语义前查调用方。
- 页面负责布局与用例，API 层负责请求和类型，纯模型处理展示转换；
  Pinia 用于实际共享状态，不保存多份可推导的列表/选中项/派生结果。
- 复用 Element Plus 和项目的表格、表单、弹窗、字典、权限、图标与样式能力；
  不引入第二套 UI 框架，不把 React 排期中心代码复制进 Vue 页面。
- 租赁能力调用 `/admin-api/rental/**`，认证和公共能力使用已有模块接口；
  前端不得计算权威金额、排期或放宽后端准备状态。
- 首屏聚焦当前业务操作，次要详情按需展开；计租与设备占用明确区分。
  加载、空数据、失败、无权限、表单错误和提交结果必须可见，不能被折叠区隐藏。

## 验证

在本目录先执行 `pnpm ts:check`，再跑对应 `test:schedule`、
`test:return-registration`、`test:rental-configuration` 或真实存在的相关测试。
按风险执行 `pnpm build:local` 等适当构建并验证目标页面；
完整说明见[验证规则](../docs/engineering/validation.md)。检查使用 lint 的 `:check` 版本，
不要把带 `--fix` / `--write` 的脚本作为只读 review 命令。
