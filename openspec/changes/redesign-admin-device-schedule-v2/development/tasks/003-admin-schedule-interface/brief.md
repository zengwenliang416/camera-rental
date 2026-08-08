# Task Brief: 003-admin-schedule-interface

## Goal

运营人员可以在现有管理端使用设备搜索、14/30/90 天时间轴、待分配、候选、异常
和右侧详情抽屉。

## Parent Artifacts

- `openspec/changes/redesign-admin-device-schedule-v2/requirements.md`
- `openspec/changes/redesign-admin-device-schedule-v2/acceptance.md`
- `openspec/changes/redesign-admin-device-schedule-v2/prototype/handoff.md`

## Vertical Slice

将批准原型按 Vue 3 + Element Plus 重新实现到现有 `RentalSchedule` 路由，并连接
工作台、候选、锁定、分配和物流 API。

## In Scope

- 类型化 API、纯时间轴模型、指标、筛选、时间轴、待分配、推荐和异常组件。
- 订单、设备和物流抽屉。
- 搜索、设备分页、14/30/90 天导航、锁定和事务分配交互。
- 中英文、浅色/深色、加载、空、错误、无权限和窄屏。

## Out Of Scope

- 后端权威可用性、冲突或指标算法。
- 员工扫码、回仓、检测和维修页面。
- 其他租赁页面重设计。

## Files Allowed

- `camera-rental-admin/src/api/rental`
- `camera-rental-admin/src/views/rental/schedule`
- `camera-rental-admin/src/locales/zh-CN.ts`
- `camera-rental-admin/src/locales/en.ts`
- `camera-rental-admin/tests/scheduleModel.test.ts`

## Interfaces / Seams

- 页面协调器到类型化工作台 API。
- 时间轴纯模型到 `ScheduleTimeline`。
- 待分配选择到候选、锁定和分配动作。

## Components To Create

- `ScheduleMetrics`
- `ScheduleFilters`
- `ScheduleTimeline`
- `PendingAllocationPanel`
- `AllocationRecommendationPanel`
- `ScheduleExceptionQueue`
- `ScheduleOrderDrawer`
- `ScheduleDeviceDrawer`
- `ScheduleLogisticsDrawer`

## Components To Reuse

- `ContentWrap`、`Pagination`、Element Plus、`rentalLabels`。
- 现有管理端主题、i18n、权限和请求封装。

## Components To Extract

- `useScheduleWorkbench`
- `useScheduleDrawerState`
- `scheduleModel` 日期窗口、裁剪和布局函数。

## API / Data Flow Contracts

- 全部五个 Schedule V2 data flow。
- 后端响应是设备可用性、候选、异常和指标的唯一来源。

## State / Error / Empty / Loading Behavior

- Loading: 首屏骨架与抽屉局部加载分离。
- Empty: 无设备、无待分配和无异常分别显示可操作空态。
- Error: 保留筛选和选中状态，允许安全重试。
- Disabled: 陈旧候选、系统锁定和保存中禁用写操作并说明原因。
- Permission: 隐藏未授权操作并处理后端 `403`。

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `pnpm test:schedule`
- `pnpm ts:check`
- `pnpm build:prod`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- 不复制原型 mock 数据或内联 JavaScript。
- 不把排期行分页继续伪装成设备分页。
- 不在普通时间轴展示完整客户手机号、地址或原始渠道 JSON。
