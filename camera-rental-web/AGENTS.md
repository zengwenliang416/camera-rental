# Web — 客户官网规则

本目录使用 Nuxt、Vue 3 和 bun。保留 SSR 能力以及现有客户归还流程。

## 按需读取

应用[根规则](../AGENTS.md)，读取 README 相关部分、`package.json`、`bun.lock`、
Nuxt 配置与目标页面。读[工程规范的前端章节](../docs/engineering/standards.md#3-前端职责与状态)；
客户归还改动再读[归还登记](../docs/domain/customer-return-registration.md)。

## 实现与复用

- 页面位于 `app/pages/`，UI 复用 `app/components/`，
  业务过程、类型与纯逻辑先查 `app/composables/`、`app/types/`、`app/utils/`。
- 请求复用已有 composable/封装；与 uniapp 共用 `/app-api/rental/**` 契约，
  不复制后端订单、排期和金额规则，不因为 SSR 而再建一套业务后端。
- 服务端渲染不得直接访问 `window`、`document`、浏览器存储；
  浏览器能力隔离到客户端生命周期。SSR 与客户端初始状态应一致，避免 hydration 差异。
- SSR 不以模块级可变单例保存用户状态；数据缓存/异步数据键必须区分用户及相关参数，
  不让登录态、订单或归还数据跨请求泄漏。
- 服务端密钥只留在私有 runtime config，不进入 public config、页面载荷或客户端 bundle。
  不为了局部组件依赖浏览器而关闭全站 SSR。
- 商品、分类、品牌内容考虑 SEO、首屏和图片加载；客户表单优先展示当前必填项，
  错误、状态与隐私提示保持可见，次要历史按需展开。
- 沿用 bun，不照搬模板 README 中的其他包管理器命令。

## 验证

在本目录按影响执行 `bun run test`、`bun run build`；页面交互使用配置匹配的
`bun run test:e2e` 或目标浏览器验证。检查 SSR 首次访问、客户端导航、
表单失败恢复与窄屏布局；构建成功不能替代页面验收。
详见[验证规则](../docs/engineering/validation.md)。
