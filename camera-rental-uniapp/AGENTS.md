# Camera Rental Uni-app Instructions

本仓库是客户商城端，使用 Vue 3 + uni-app，面向微信小程序、H5 和 App。

开始任务前：

1. 阅读父目录 `../AGENTS.md`、`../docs/architecture.md` 和租赁领域文档。
2. 阅读本仓库 README、`package.json` 和目标页面现有模式。
3. 客户端只调用后端 `/app-api/rental/**`，后端结果是库存、排期和金额的权威来源。
4. 下单提交时必须允许后端再次校验排期，处理库存冲突、支付取消、登录失效和重复提交。
5. 使用 uni-app 官方跨端能力，不依赖浏览器专有 DOM API。
6. 不保存支付密钥、闲管家密钥或其他服务端凭据。
7. 修改后根据本仓库真实可用脚本验证 H5 和微信小程序构建。
