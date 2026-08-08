# Task Brief: 002-lock-and-allocation

## Goal

主管可以分类锁定设备，运营人员可以看到安全候选并通过现有事务确认分配。

## Parent Artifacts

- `openspec/changes/redesign-admin-device-schedule-v2/requirements.md`
- `openspec/changes/redesign-admin-device-schedule-v2/acceptance.md`
- `openspec/changes/redesign-admin-device-schedule-v2/prototype/handoff.md`

## Vertical Slice

从 `rental_device_lock` 增量表、锁定生命周期、候选资格到最终事务分配，完整实现
订单预留、回仓待检测、维修隔离和主管人工保留。

## In Scope

- 锁定 migration、DO、Mapper、枚举、权限、Service 和 Controller。
- 候选查询、排除原因、锁定创建/解除/过期和生命周期自动锁定。
- 最终分配重查活动锁定。
- 并发、租户、权限、审计和不可人工解除系统锁定测试。

## Out Of Scope

- 工作台前端组件。
- 持久化排期冲突事件表。
- 修改员工端扫描界面。

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java`
- `camera-rental-server/sql/mysql/migrations`
- `openspec/changes/redesign-admin-device-schedule-v2/development/migrations`

## Interfaces / Seams

- `RentalDeviceLockService` 与设备回仓、检测、维修和分配 Service。
- 候选查询到现有 `RentalDeviceAssignmentServiceImpl`。

## Components To Create

- `RentalDeviceLockService`
- 锁定 VO、枚举和稳定候选排除原因。

## Components To Reuse

- 现有设备行锁、订单明细锁、幂等分配和排期冲突检查。
- 现有租户和权限框架。

## Components To Extract

- 所有人工和系统锁定写入共用一个事务 Service。

## API / Data Flow Contracts

- `FLOW-SCHEDULE-CANDIDATES`
- `FLOW-SCHEDULE-ASSIGN`
- `FLOW-SCHEDULE-DEVICE-LOCK`

## State / Error / Empty / Loading Behavior

- Loading: 候选和锁定写请求不返回未落库成功。
- Empty: 无候选时返回稳定排除统计和安全说明。
- Error: 并发占用或锁定返回可刷新候选的稳定错误码。
- Disabled: 系统管理锁定禁止普通人工解除。
- Permission: 查询使用排期权限，创建/解除使用 `rental:device-lock:update`。

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental -am -DskipITs test`
- migration 语法、升级、验证和回滚检查。

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- 不用数据库 `FOR UPDATE` 临时锁代替业务锁定记录。
- 不允许人工解除回仓待检测或维修隔离来绕过生命周期。
- 不相信候选查询结果在最终确认时仍然有效。
