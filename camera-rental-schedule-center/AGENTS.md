# Schedule Center — 独立排期中心规则

本目录是统一仓库中的 React + TypeScript + Vite 应用，使用 pnpm。
与 Vue 管理后台共用后端管理 API、登录态、租户及权限，保持独立 UI 边界。

## 按需读取

应用[根规则](../AGENTS.md)，读取 README、`package.json`、`pnpm-lock.yaml`、
`tsconfig.json` 及目标 feature 的现有代码/测试。
读[工程规范的前端章节](../docs/engineering/standards.md#3-前端职责与状态)；
涉及排期/发货再按根规则加载领域和集成文档。

## 实现与复用

- `src/app/` 负责应用集成；`src/features/` 承载业务模块；
  `src/api/` 负责认证、HTTP、分页、快照和外部 DTO 映射；`src/shared/` 提供公共 UI/hook/工具。
- 新行为优先放入所属 feature 的组件、纯 model 与现有 context；
  先检查 `src/components/`、`src/context/`、`src/lib/` 的既有调用再决定复用位置，
  不复制平行实现，也不为目录整齐在任务外批量搬迁历史文件。
- 共享层不得导入 feature；feature 通过清晰入口协作，不绕过现有命令入口直接改服务器状态。
  按用途组合 context/provider，不把所有查询、会话和命令塞进一个全局可变对象。
- 复用 `src/api/` 认证与租户处理；同源缓存、登录退出、令牌刷新和权限接口属于既有协议。
  修改集成时同时验证管理后台消费者，不新建账号系统、不复制 Vue store、不扩大凭据信任域。
- 使用现有 React UI、样式和图标体系，不引入 Element Plus 或第二套 UI 框架。
  面板、抽屉和高级筛选按需展开，保留关键冲突/待复核状态与键盘焦点行为。
- `*Model` 与 DTO mapper 只做展示/转换，权威排期和金额来自后端；
  mock/demo 数据不得作为真实 API 失败的静默兜底。
- 查询/轮询处理过期响应与卸载清理，写命令处理重复提交及失败恢复；
  不能把本地状态更新当成后端履约成功。

## 验证

在本目录执行 `pnpm lint`（实际是 `tsc --noEmit`）、相关测试或 `pnpm test`，
按影响执行 `pnpm build`，并验证目标页面。
认证/集成变化覆盖独立进入、管理端跳转、登录过期、无权限与租户切换。
遵循[验证规则](../docs/engineering/validation.md)，类型检查不能报告为 ESLint 或浏览器通过。
