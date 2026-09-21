# 员工 Android App 架构

## 目标

员工端服务仓库作业：查看任务、扫码发货、回仓登记、设备检测和异常处理。所有设备状态、订单状态、排期冲突、权限和幂等规则由服务端决定，App 只负责采集输入、展示允许的动作并提交操作。

## 分层

```text
页面 pages-rental
  -> 业务组件 components/rental
  -> 用例 composable hooks/rental
  -> 类型化 API api/rental
  -> 领域/展示模型 models/rental
  -> 现有 http、token、user、tenant、theme
```

页面不得直接调用 `uni.request`，不得复制管理端 Axios 或权限实现。API DTO、领域模型和展示模型分开；金额和状态判定只来自后端响应。

## 目录约定

- `src/api/rental/`：按后端资源组织的请求和 DTO。
- `src/models/rental/`：前端需要的领域类型和状态文案映射。
- `src/hooks/rental/`：页面用例、加载、重试和提交状态。
- `src/components/rental/`：可复用的扫码卡片、状态标签和任务卡片。
- `src/services/scanner/`：扫码设备适配层。页面只消费统一 `ScanResult`。
- `src/store/rental/`：仅保存跨页面的仓库、扫码头连接等状态。
- `src/style/rental/`：员工端主题 token 和平台差异样式。

## 扫码适配

统一支持厂家广播、`uni.scanCode` 摄像头和手工输入。`src/services/scanner/android.ts` 通过 Native.js 调用 UROVO 设备系统自带的 `ScanManager`，读取设备当前的广播 Action/数据字段、打开扫描引擎并切换到广播输出。初始化完成且确认引擎和输出模式后，才显示扫码头已连接；普通设备或初始化失败时保留摄像头入口。无需把厂家 JAR 复制到业务页面。

`useStaffScanner` 只在作业页显示期间订阅，隐藏/卸载时解除订阅并停止当前解码；App 退到后台时注销广播并恢复此前输出模式，只关闭由本次适配器打开的扫描引擎。四个消费入口为设备查询、候选设备拣货、扫码发货和回仓登记。发货默认先扫运单再扫设备，点击对应步骤可切换扫描目标。重复码短时间去重，正在处理时提示稍后重扫，任何扫描都不能自动提交分配、出库或回仓。

广播 Action 与厂商 SDK 类名只能出现在平台适配层；页面不得依赖厂家协议。调试日志只记录接收事件，不记录条码、二维码签名或客户信息。运行 `node scripts/test-scanner.mjs` 可验证协议读取、初始化回滚、后台资源释放和页面消费生命周期；Mock 验证不替代真实侧键/扫描光头读码验收。

## 服务端边界

写操作提交前显示目标设备和订单，提交中禁用重复点击；服务端返回冲突、权限不足、已处理等结果时保持错误可见并允许重新加载。回仓登记和设备检测按两个动作实现，不能由前端本地改变状态。

## 第一阶段验收

登录后进入员工工作台，打开设备扫码页，完成厂家侧键或摄像头扫码，调用 `resolve-qr` 展示设备编号、型号、仓库和服务端状态。真机需验证连续扫码、重复扫码、错误码、断网恢复和页面切换后监听释放。

## Android 构建与真机运行

App 构建只包含主包、`pages-core` 登录核心页与 `pages-rental` 作业页。H5 和小程序保留各自原有分包；切换到 App 时，Vite 在配置阶段过滤生成的 `src/pages.json` 中的其他分包，避免历史路由引入 IM/LiveKit 等浏览器依赖。不能删除该文件再等待插件生成，因为 uni-app CLI 会提前读取它。

首次安装依赖或生成文件缺失时，在本目录执行 `pnpm init-baseFiles`。App 资源可用 `pnpm exec uni build -p app --mode development` 验证；这个命令只生成资源，不会生成独立 APK。

HBuilderX 5.26 支持官方 CLI 真机运行。先用 HBuilderX 的 `cli devices list` 确认设备，再执行 `cli launch app-android --project <员工端项目绝对路径> --deviceId <设备序列号> --playground standard`。等待“同步手机端程序文件成功”和应用启动后，再检查真机页面及运行日志。不要手工把资源复制进基座的 Android 数据目录，以免文件所有者与 App UID 不一致导致读取或覆盖失败。

仓务 App 固定竖屏，适配 PDA 单手操作。标准调试基座的包名和图标属于 HBuilder；独立发布 APK 仍需要单独完成打包与签名。登录使用构建配置指向的后端及所选租户中的账号，线上账号与本地开发账号不能混用。
