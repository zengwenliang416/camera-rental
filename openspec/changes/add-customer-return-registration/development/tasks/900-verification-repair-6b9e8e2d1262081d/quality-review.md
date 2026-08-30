# Quality Review: 900-verification-repair-6b9e8e2d1262081d

## Verdict

needs-fix

## Separation Of Concerns

- 将浏览器 API mock 提取为共享 `installReturnRegistrationMock`，减少了三个场景内
  重复的 transport stub；但是 SpecNav Kernel 的执行单元是序列化后的单个
  `scenario` 函数，不是整个 CommonJS 模块。
- `return-review-and-security` 在
  `tests/specnav/customer-return-registration.js:260-261` 调用模块外 helper，
  因而把场景的可执行性错误地耦合到不会进入 Kernel sandbox 的模块闭包。

## Component Cohesion / Coupling

- helper 内聚地保存 submit count、请求体与 unsafe write count，跨同源导航使用
  `sessionStorage` 也符合本场景的两步行为。
- 但场景与 helper 的自由变量耦合是阻断项。Kernel 的
  `serializePlaywrightScenario` 只保存 `Function.prototype.toString()` 的场景源码，
  worker 再在隔离 `vm` 中编译该源码；helper 定义不会被一同传入。

## Test Quality

- 独立执行 `node --check tests/specnav/customer-return-registration.js` 通过。
- 使用已安装 Verification Kernel 的 managed Chromium 和
  `createPlaywrightApiGuard` 直接调用模块导出的场景时，四个 vc02 断言通过，
  denied-method 列表为空。这证明新的 `page.addInitScript` 路径本身未调用
  `page.route`。
- 该 probe 保留了 Node 模块闭包，所以不能证明正式 Kernel 执行。将实际
  `scenario` 交给 Kernel serializer 后重新编译并调用，稳定得到
  `ReferenceError: installReturnRegistrationMock is not defined`，四个断言均无法
  到达。

## Error Handling

- 页面内 mock 对 REVIEW_REQUIRED 与统一错误响应保持显式分支，且状态在导航前
  持久化。
- 场景没有对缺失 helper 的 Kernel 执行错误提供任何可恢复路径；错误发生在
  `page.goto` 之前，报告中声称的浏览器断言通过不能覆盖此失败。

## Reuse / Duplication

- 提取 helper 消除了三份 `page.route` mock 的显著重复。
- 当前复用方式不符合场景序列化契约。应复用可序列化的数据或在每个场景函数内部
  定义/注入自包含 helper，而不是引用模块级函数。

## Complexity Delta

- 文件由 348 行变为 351 行，重复减少但新增了一个 210 行的共享 mock、状态持久化
  和 XHR 替身。
- 结构复杂度可接受，但隐藏的闭包依赖使运行时复杂度和诊断成本上升，并直接阻断
  正式 Verification rerun。

## Acceptance Assertions Verified

- `vc02-no-authoritative-side-effect`: managed Chromium 直接模块 probe 通过；正式
  Kernel 序列化执行未到达断言。
- `vc02-review-required-visible`: managed Chromium 直接模块 probe 通过；正式
  Kernel 序列化执行未到达断言。
- `vc02-submit-count`: managed Chromium 直接模块 probe 通过；正式 Kernel
  序列化执行未到达断言。
- `vc02-unified-error-visible`: managed Chromium 直接模块 probe 通过；正式
  Kernel 序列化执行未到达断言。

## Required Fixes

- 使 `return-review-and-security` 的序列化源码完全自包含，例如在场景函数内部定义
  安装逻辑，或使用 Kernel 明确支持且会随场景传递的输入；不得引用模块级
  `installReturnRegistrationMock`。
- 修复后通过 `serializePlaywrightScenario` 加 worker 等价的隔离 `vm` 编译路径
  重跑，再用同一 Kernel guard 的 managed Chromium 证明四个 vc02 断言均通过且
  denied-method 列表为空。
