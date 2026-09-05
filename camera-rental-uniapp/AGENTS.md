# Uni-app — 客户端规则

本目录使用 Vue 3 + uni-app，面向 H5、微信小程序和 App，保留现有 JavaScript 代码约定。

## 按需读取

应用[根规则](../AGENTS.md)，读取 README 相关部分、`package.json`、`pnpm-lock.yaml`、
`pages.json` / `manifest.json` 中任务相关配置及目标页面。
前端改动读[工程规范的前端章节](../docs/engineering/standards.md#3-前端职责与状态)，
涉及下单/归还再加载对应领域文档。

## 实现与复用

- 页面位于 `pages/`，复用 `sheep/components/`、`sheep/ui/` 和已有布局；
  API 使用 `sheep/api/`，统一请求走 `sheep/request/`。
- 共享业务状态/过程优先查 `sheep/store/`、`sheep/hooks/`、`sheep/helper/`；
  平台能力走 `sheep/platform/` 和 uni-app 官方能力，不能从页面直接依赖浏览器 DOM。
- 平台差异局限在适配层或必要条件编译处；不得把 Web/管理端组件、浏览器存储或
  服务端凭据直接复制到小程序。分页、图片与请求考虑移动网络。
- 租赁 API 使用 `/app-api/rental/**`，公共会员/支付等复用既有接口；
  下单结果、金额和库存以后端为准，提交时接受后端再次校验。
- 对登录失效、支付取消、库存冲突、重复提交、断网与失败恢复提供明确提示。
  基本字段和错误在当前步骤可见，低频说明与历史按需展开。
- 沿用 pnpm 锁文件，不无故更换依赖工具或借局部修改强制迁移 TypeScript。

## 验证

当前 `package.json` 只有 `prettier` 脚本且会写入文件，没有 H5/微信小程序构建脚本。
修改后使用实际可用的 HBuilderX 工具链分别验证 H5 与微信小程序；
涉及 App API 时补平台验证。工具缺失时明确未执行项和原因，不能把格式检查当构建成功。
参见[验证规则](../docs/engineering/validation.md)。
