# Quality Review: 900-verification-repair-b5748f717df240c3

## Verdict

needs-fix

## Separation Of Concerns

- 上传场景的 fetch mock、上传 XHR 替身和记录状态集中在
  `installReturnRegistrationMock`，比原先在场景中散布多个 `page.route` 更易读取。
- 但是 `return-private-upload` 在
  `tests/specnav/customer-return-registration.js:309-310` 引用了场景函数外的
  helper。SpecNav Kernel 只序列化单个 `scenario` 函数，模块级定义不会进入执行
  sandbox，导致职责分离跨越了不可用的运行时边界。

## Component Cohesion / Coupling

- 上传事件 `verify -> authorize -> put -> confirm -> submit` 与 attachment binding
  由一个状态对象收集，内部语义是内聚的。
- 场景对模块闭包存在强耦合。Kernel 的 `serializePlaywrightScenario` 保存的源码
  包含 helper 调用却不包含 helper 定义，worker 的隔离 `vm` 无法解析该名称。

## Test Quality

- 独立执行 `node --check tests/specnav/customer-return-registration.js` 通过。
- 使用已安装 Verification Kernel 的 managed Chromium 和
  `createPlaywrightApiGuard` 直接调用模块导出的上传场景时，三个 vc03 断言通过，
  事件顺序为 `verify, authorize, put, confirm, submit`，denied-method 列表为空。
- 上述 probe 继承了 CommonJS 模块闭包。使用 Kernel serializer 的真实场景源码
  重新编译并调用时得到
  `ReferenceError: installReturnRegistrationMock is not defined`，因此正式执行不会
  安装 fetch/XHR shim，也不会到达任何 vc03 断言。

## Error Handling

- XHR 替身对非 PUT、错误 URL 和非 Blob 请求显式走 `onerror`，成功路径提供 progress
  与 `onload`，足以覆盖当前上传实现。
- 场景本身没有处理 Kernel sandbox 中缺失 helper 的错误。该错误发生在导航和文件
  选择之前，属于确定性的场景装载缺陷而非可接受的环境波动。

## Reuse / Duplication

- 共享 helper 移除了三组重复的 API response 构造，并让上传 mock 复用同一状态
  管道。
- 复用边界选择错误。可维护的复用必须同时满足 Kernel 的可序列化约束；应将 helper
  置于场景函数内部或通过受支持的场景输入传递。

## Complexity Delta

- 文件由 348 行变为 351 行；新增 XHR class 和状态持久化后，单个共享 helper 达
  210 行，复杂度集中但尚可跟踪。
- 当前抽取减少源码重复，却引入了阻断全部场景的隐式运行时依赖，整体质量 delta
  为负。

## Acceptance Assertions Verified

- `vc03-confirmed-file-bound`: managed Chromium 直接模块 probe 通过；正式 Kernel
  序列化执行未到达断言。
- `vc03-private-upload-order`: managed Chromium 直接模块 probe 通过；正式 Kernel
  序列化执行未到达断言。
- `vc03-upload-success-visible`: managed Chromium 直接模块 probe 通过；正式
  Kernel 序列化执行未到达断言。

## Required Fixes

- 使 `return-private-upload` 的序列化源码自包含，确保 fetch mock 与 XHR 上传替身
  的定义随场景进入 Kernel sandbox；不得引用模块级
  `installReturnRegistrationMock`。
- 修复后通过 Kernel serializer 和 worker 等价的隔离 `vm` 编译路径重跑，再用
  同一 guard 的 managed Chromium 验证三个 vc03 断言、完整事件顺序和空
  denied-method 列表。
