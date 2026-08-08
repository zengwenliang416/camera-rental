# Prototype Question: redesign-admin-device-schedule-v2

## Question

已确认的设备排期 V2 原型是否足以作为生产管理端实现依据，覆盖单仓、数百设备搜索
与分页、14/30/90 天长周期时间轴、待分配订单、候选推荐、冲突与异常、分类设备
锁定以及订单/设备/物流右侧抽屉？

## Branch

`ui-html`

## Review Target

- Entry: `artifact/index.html`
- Required reviewer decision: 用户已明确回复“原型没问题了，开始开发这个界面”，
  并选择分类持久化设备锁定方案。

## Out of Scope

- Production implementation.
- Database writes.
- Deployment behavior.
- 员工端扫码发货、回仓、检测和维修执行。
