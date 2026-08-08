# Prototype Handoff: redesign-admin-device-schedule-v2

## Approved Branch Variant

- Branch: `ui-html`
- Variant: `device-search-v7` with classified persisted device locks.
- Approval: 用户明确确认原型无问题并选择设备锁定方案 1。

## Screens Or Flows

- 单仓设备排期工作台首屏。
- 14、30、90 天时间轴与数百设备搜索、分页。
- 待分配订单详情、候选推荐和事务分配。
- 冲突与异常队列。
- 设备、订单和物流右侧抽屉。
- 分类设备锁定创建、展示和解除。

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

- `ContentWrap`、`Pagination` 和 Element Plus 表单、标签、抽屉、空态、加载组件。
- `rentalLabels`、现有日期格式化和管理端主题、i18n、权限能力。
- 现有事务设备分配和 Delivery tracking detail/refresh 接口。

## Extraction Targets

- `index.vue` 仅保留查询、加载、选中和抽屉协调。
- `scheduleModel.ts` 负责时间窗口、显示裁剪和列布局，不计算权威候选或冲突。
- 后端提取工作台聚合、待分配、候选、设备详情和分类锁定 Service。

## API Contracts

- `GET /admin-api/rental/schedule/workbench`
- `GET /admin-api/rental/order/pending-allocation-page`
- `GET /admin-api/rental/order/{id}`
- `GET /admin-api/rental/order-item/{itemId}/device-candidates`
- `GET /admin-api/rental/device/{id}`
- `POST /admin-api/rental/device-lock`
- `PUT /admin-api/rental/device-lock/{id}/release`
- 复用 `POST /admin-api/rental/device/assign` 和现有物流跟踪接口。

## Data Flows

- `FLOW-SCHEDULE-WORKBENCH-QUERY`
- `FLOW-SCHEDULE-DETAIL`
- `FLOW-SCHEDULE-CANDIDATES`
- `FLOW-SCHEDULE-ASSIGN`
- `FLOW-SCHEDULE-DEVICE-LOCK`

## State Behavior

- Loading: 指标、时间轴和抽屉按独立请求显示骨架或局部加载。
- Empty: 说明当前筛选无设备、无待分配或无异常，并保留可用筛选动作。
- Error: 保留查询条件并提供安全重试，不显示第三方原始错误。
- Disabled: 陈旧候选、系统管理锁定和无可用设备时禁用不安全写操作并说明原因。
- Permission: 隐藏未授权操作，同时显式处理后端 `403`。

## Theme And Locale Policy

- Theme support: `light-dark`
- Theme modes shown in prototype: `light`
- Theme toggle: 复用管理端现有开关，原型不新增开关。
- Internationalization: `enabled`
- Locales shown in prototype: `zh-CN`
- Locale switcher: 复用管理端现有开关，生产验证覆盖 `zh-CN,en`。

## Out Of Scope Items

- 员工端扫码发货、回仓、检测和维修执行。
- 跨仓库筛选和调拨。
- 前端权威排期、候选、金额或冲突计算。
- 第一阶段持久化冲突事件表。

## Required Tests

- 后端工作台聚合、设备分页、半开区间、候选、并发分配、分类锁定和物流批量关联。
- 前端 14/30/90 天、跨月长租、搜索分页、抽屉、候选刷新、主题和中英文。
- 浏览器真实数据验证工作台主流程、异常与无权限状态。

## Open Risks

- 数百设备与 90 天窗口的数据量需要设备分页、批量查询和索引验证。
- 候选结果可能陈旧，最终分配必须事务重查。
- 生命周期自动锁定必须与回仓、检测和维修状态保持一致。
