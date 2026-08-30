# Quality Review: 900-verification-repair-f4581bff88b5cbb4

## Verdict

needs-fix

## Separation Of Concerns

- 成功场景通过共享 helper 将 API mock 与页面交互分开，测试意图比重复 route handler
  更清楚。
- 但 `return-success-idempotent` 在
  `tests/specnav/customer-return-registration.js:219-220` 调用模块外 helper。
  SpecNav Kernel 的隔离执行只接收序列化后的单个 `scenario` 函数，这种抽取越过了
  可执行边界。

## Component Cohesion / Coupling

- submit count、normalized request body 和 receipt response 集中在同一 success
  mode，内部数据关系是内聚的。
- 场景与 CommonJS 模块闭包存在未声明耦合。Kernel serializer 输出的函数源码保留
  `installReturnRegistrationMock(...)` 调用，却不会携带第 3-212 行的 helper 定义。

## Test Quality

- 独立执行 `node --check tests/specnav/customer-return-registration.js` 通过。
- 使用已安装 Verification Kernel 的 managed Chromium 和
  `createPlaywrightApiGuard` 直接调用模块导出的场景时，四个 vc01 断言通过，
  denied-method 列表为空，说明 double-click、单次提交和机器码归一化在模块环境下
  工作。
- 该 probe 没有覆盖 Kernel serialization boundary。把同一场景经
  `serializePlaywrightScenario` 后在隔离上下文编译并调用，得到
  `ReferenceError: installReturnRegistrationMock is not defined`；正式 Verification
  无法到达固定入口或提交断言。

## Error Handling

- success mock 保留 120ms 延迟，可继续检验 double-click 时的 pending gate，并返回
  明确 ACCEPTED receipt。
- 场景对缺失 helper 没有防护，且失败发生于 `page.goto` 之前。报告中的直接浏览器
  结果不能证明 Kernel worker 可执行。

## Reuse / Duplication

- helper 抽取消除了 success、review 和 upload 场景间的大量重复 response/stub
  代码。
- 这种模块级复用与 Kernel 契约不兼容。应采用场景内自包含 helper，或仅复用能通过
  `scenario_data` 等受支持边界传入的纯数据。

## Complexity Delta

- 文件从 348 行增加到 351 行，重复减少，但新增共享 mock 达 210 行并包含多 mode
  分支、持久化和 XHR 行为。
- 主要复杂度问题不是行数，而是抽取制造了 serializer 看不见的依赖，导致三个场景
  一起失效。

## Acceptance Assertions Verified

- `vc01-fixed-entry-visible`: managed Chromium 直接模块 probe 通过；正式 Kernel
  序列化执行未到达断言。
- `vc01-normalized-machine-code`: managed Chromium 直接模块 probe 通过；正式
  Kernel 序列化执行未到达断言。
- `vc01-receipt-accepted`: managed Chromium 直接模块 probe 通过；正式 Kernel
  序列化执行未到达断言。
- `vc01-submit-count`: managed Chromium 直接模块 probe 通过；正式 Kernel
  序列化执行未到达断言。

## Required Fixes

- 使 `return-success-idempotent` 的序列化源码完全自包含，例如在场景函数内部定义
  安装逻辑，或通过 Kernel 明确支持的输入传递所需数据；不得引用模块级
  `installReturnRegistrationMock`。
- 修复后先通过 `serializePlaywrightScenario` 加 worker 等价的隔离 `vm` 调用，
  再用同一 Kernel guard 的 managed Chromium 重跑四个 vc01 断言并确认
  denied-method 列表为空。
