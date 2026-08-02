# Prototype Question: add-rental-delivery-foundation

## Question

供应商无关的纯逻辑能否对完整轨迹进行确定性规范化、事件 fingerprint 和
snapshot hash 计算，并在重复、乱序和终态场景下生成稳定且不回退的当前摘要？

## Branch

`logic-state`

## Review Target

- Entry: `logic/harness.js`
- Required reviewer decision: 是否批准 `deterministic-snapshot-v1` 作为 Change 1
  的快照规范化、hash、版本推进和终态保护实现基线。

## Out of Scope

- Production implementation.
- Database writes.
- Deployment behavior.
- 快递100 SDK、供应商状态映射、网络请求和回调验签。
- UI、主题、国际化和排期中心展示。
