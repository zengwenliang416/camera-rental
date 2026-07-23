# Camera Rental Web Instructions

本仓库是 PC 客户租赁官网，使用 Nuxt 4、Vue 3 和 bun。

开始任务前：

1. 阅读父目录 `../AGENTS.md`、`../docs/architecture.md` 和相关领域文档。
2. 阅读本仓库 README、`package.json`、`bun.lock` 和 Nuxt 配置。
3. 客户官网与 uni-app 共用后端 `/app-api/rental/**`，不复制订单、排期或金额逻辑。
4. 保持 SSR 兼容，服务端渲染期间不得直接访问 `window`、`document` 或浏览器存储。
5. 服务端密钥只能留在服务端运行时配置，不能进入客户端 bundle。
6. 使用 bun，不无故切换 npm、yarn 或 pnpm。
7. 修改后根据本仓库真实脚本执行 `bun run build` 或目标范围内的检查。

商品详情、分类和品牌页面应优先考虑 SEO、加载性能、错误状态和无库存状态。
