# Staff — 员工移动端规则

本目录使用 Vue 3、TypeScript、uni-app、Wot UI 和 pnpm。

## 按需读取

应用[根规则](../AGENTS.md)，读取 README 相关部分、`package.json`、`pnpm-lock.yaml`、
目标页面与类型/格式配置。读[工程规范的前端章节](../docs/engineering/standards.md#3-前端职责与状态)；
扫码/分配/出入库再读[设备排期](../docs/domain/device-scheduling.md)，闲管家功能按根规则加载。

## 实现与复用

- 租赁页面从 `src/pages-rental/`、API 从 `src/api/rental/` 定位。
  网络调用复用 `src/http/`，UI 复用 Wot UI 和 `src/components/`。
- 状态与交互先查 `src/store/`、`src/hooks/`、`src/utils/`；
  不复制管理端 Axios/Element Plus 实现，不平行建设账号/租户/权限机制。
- 扫码只负责识别与提交；设备归属、状态、订单、排期与操作权限由管理端后端接口校验。
  不得用本地列表变化表示出库、回仓或维修成功。
- 处理连续扫码、重复点击、网络中断、已处理设备、权限不足及服务端状态改变；
  失败允许安全重试，不能把队列中的本地动作显示为已履约。
- 使用 uni-app 官方扫码、上传和平台能力，差异放适配层/条件编译；
  只展示当前操作必需的设备与订单信息，低频历史按需展开，结果与冲突保持可见。
- 移动网络下限制图片与列表负载，轮询/订阅随页面生命周期释放。
  敏感客户信息、支付凭据和第三方密钥不进入客户端日志、缓存或测试。

## 验证

在本目录执行 `pnpm type-check`。跨端共享修改至少执行
`pnpm build:h5` 与 `pnpm build:mp-weixin`，扫码/相机等能力补相应设备或平台验证。
检查使用 `lint`，`lint:fix` 会写文件。详见[验证规则](../docs/engineering/validation.md)。
