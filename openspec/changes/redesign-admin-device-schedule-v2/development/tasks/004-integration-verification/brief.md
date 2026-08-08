# Task Brief: 004-integration-verification

## Goal

设备排期 V2 在真实后端数据、长租、并发分配、分类锁定和物流异常下可验证运行。

## Parent Artifacts

- `openspec/changes/redesign-admin-device-schedule-v2/requirements.md`
- `openspec/changes/redesign-admin-device-schedule-v2/acceptance.md`
- `openspec/changes/redesign-admin-device-schedule-v2/prototype/handoff.md`

## Vertical Slice

完成跨层测试、真实数据浏览器验证、查询规模检查、迁移证据和 SpecNav 验证交接，
不在此任务引入新的产品功能。

## In Scope

- 后端聚合、候选、锁定、分配和物流测试。
- 前端模型、类型、构建和浏览器主流程验证。
- migration 验证/回滚、性能风险、权限和 PII 检查。
- 更新任务报告、验证日志和交接证据。

## Out Of Scope

- 新增产品功能或扩大 API。
- 部署、提交或推送，除非用户另行明确要求。
- 修改生产数据。

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test`
- `camera-rental-admin/tests`
- `openspec/changes/redesign-admin-device-schedule-v2`

## Interfaces / Seams

- Maven 测试到后端契约。
- pnpm 测试/类型/构建到管理端。
- 浏览器真实数据结果到验收断言。

## Components To Create

- 不创建新的生产组件。

## Components To Reuse

- 复用项目测试框架、SpecNav 验证和浏览器检查。

## Components To Extract

- 重复的测试 Fixture 或断言辅助函数按现有测试模式提取。

## API / Data Flow Contracts

- 验证全部工作台、详情、候选、分配和锁定流程。

## State / Error / Empty / Loading Behavior

- Loading: 验证首屏和抽屉局部加载。
- Empty: 验证三个独立空态。
- Error: 验证网络、业务冲突和物流刷新失败。
- Disabled: 验证系统锁定和保存中禁用状态。
- Permission: 验证查询、分配、锁定和物流刷新权限。

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental -am test`
- `pnpm test:schedule`
- `pnpm ts:check`
- `pnpm build:prod`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- 不把构建成功当作行为验证。
- 不使用演示数据证明生产读模型正确。
- 不执行未经授权的生产部署、提交、推送或数据库迁移。
