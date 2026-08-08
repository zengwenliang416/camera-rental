# Task Brief: 001-workbench-read-model

## Goal

运营人员可以按设备分页加载权威容量、待分配订单、有效占用和排期异常。

## Parent Artifacts

- `openspec/changes/redesign-admin-device-schedule-v2/requirements.md`
- `openspec/changes/redesign-admin-device-schedule-v2/acceptance.md`
- `openspec/changes/redesign-admin-device-schedule-v2/prototype/handoff.md`

## Vertical Slice

实现 `GET /admin-api/rental/schedule/workbench`、待分配订单读取和设备详情的完整
后端读链路，为一个设备页批量组合排期、订单、分配、物流快照和检测状态。

## In Scope

- 工作台请求/响应 VO、Controller、聚合 Service 和 Mapper 批量查询。
- 设备 lane 分页、14/30/90 天窗口、指标、条带、待分配摘要和异常摘要。
- 内部订单详情和设备排期详情读取。
- 半开区间、长租延续、回仓待检测占用和无 N+1 的单元测试。

## Out Of Scope

- 分类锁定写操作和最终设备分配。
- 管理端 Vue 界面。
- 快递100外部刷新请求。

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java`

## Interfaces / Seams

- `RentalScheduleController` 到 `RentalScheduleWorkbenchService`。
- Mapper 批量读取到工作台响应 VO。
- 现有 Delivery tracking summary 本地快照。

## Components To Create

- `RentalScheduleWorkbenchService`
- 工作台、待分配订单、设备详情和异常响应 VO。

## Components To Reuse

- 现有排期、设备、订单、分配和 Delivery Mapper/DO。
- 现有权限、分页和 CommonResult 约定。

## Components To Extract

- 设备、排期、订单明细、分配和物流关系的批量装配服务。

## API / Data Flow Contracts

- `FLOW-SCHEDULE-WORKBENCH-QUERY`
- `FLOW-SCHEDULE-DETAIL`

## State / Error / Empty / Loading Behavior

- Loading: 由前端请求状态表示，后端不返回部分伪成功。
- Empty: 返回合法空设备页、零指标和空队列。
- Error: 返回稳定业务错误，不暴露 SQL 或物流 Provider 原始异常。
- Disabled: 禁用设备仍可按筛选查看但不计入可排。
- Permission: Controller 要求 `rental:schedule:query`。

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental -am -DskipITs test`
- 工作台相关定向测试类。

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- 不假设 `XianyuOrderRespVO` 等同内部租赁订单。
- 不把预计占用结束直接视为实际检测释放。
- 不在列表加载中调用外部快递100。
