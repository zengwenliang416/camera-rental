# Camera Rental Admin Instructions

本仓库是 Web 管理后台，使用 Vue 3、TypeScript、Element Plus 和 pnpm。

开始任务前：

1. 阅读父目录 `../AGENTS.md`、`../docs/architecture.md` 和相关领域文档。
2. 阅读本仓库 README、`package.json`、`pnpm-lock.yaml` 和目标页面现有模式。
3. 管理后台只调用后端 `/admin-api/rental/**`，不自行计算权威库存、排期或金额。
4. 排期页面必须同时展示计租周期和设备占用周期。
5. 复用现有 API、表格、表单、字典、权限和格式化组件。
6. 使用 pnpm，不混用 npm 或 yarn。
7. 修改后优先执行 `pnpm ts:check`，再根据风险执行真实存在的构建脚本。

不得把第三方密钥、支付凭据或客户敏感信息放入前端代码、日志或测试数据。
