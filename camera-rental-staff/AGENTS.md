# Camera Rental Staff Instructions

本仓库是员工移动端，使用 Vue 3 + TypeScript + uni-app + pnpm。

开始任务前：

1. 阅读父目录 `../AGENTS.md`、`../docs/architecture.md`、`../docs/domain/device-scheduling.md`。
2. 阅读本仓库 README、`package.json`、`pnpm-lock.yaml` 和目标页面现有模式。
3. 员工端只调用后端管理接口，扫码、出库、回仓、检测和维修必须由后端校验设备状态。
4. 不得只修改本地状态来表示设备已经出库、回仓或维修。
5. 使用 pnpm，不混用 npm 或 yarn。
6. 修改后优先执行 `pnpm type-check`，再根据风险执行 H5 或微信小程序构建。

不得展示或记录不必要的客户隐私、支付凭据或第三方密钥。
