# Quality Review: 001-delivery-foundation

## Verdict

approved

## Separation Of Concerns

- `RentalDeliveryServiceImpl` 现在只做 Delivery 创建、关系校验/绑定和安全
  Outbox 编排；carrier normalization 已集中到
  `RentalCarrierMappingService.resolve`
  (`RentalDeliveryServiceImpl.java:81-89`,
  `RentalCarrierMappingService.java:19-29`)。
- 这与 handoff 对“mapping service 统一归一化”的要求一致，当前没有再把 shipment、
  callback 和 query 路径拆成多套 normalization 规则。

## Component Cohesion / Coupling

- `TrackingSnapshotNormalizer` 的排序键现在覆盖 `rawTime`、`trackingStatus`、
  `providerStatus`、`traceText`、`location`、`source`，与 fingerprint 身份字段
  保持一致 (`TrackingSnapshotNormalizer.java:31-41`,
  `TrackingSnapshotNormalizer.java:51-57`)。
- 之前“相同完整快照仅因 Provider 返回顺序不同而生成新版本”的耦合点已消除；
  当前快照聚合边界自洽。

## Test Quality

- 新增测试覆盖了上次阻断的关键契约：
  `validatesEveryDuplicateDeviceRelationBeforeInserting`
  (`RentalDeliveryServiceImplTest.java:135-165`),
  `marksPhoneRequiredAndDoesNotEnqueueProviderTasks`
  (`RentalDeliveryServiceImplTest.java:167-199`),
  `createsStableHashWhenEventsShareTheSameTimeAndTraceText`
  (`TrackingSnapshotNormalizerTest.java:49-64`),
  `normalizesLookupAndFallbackCarrierCodesInOnePlace`
  (`RentalCarrierMappingServiceTest.java:29-38`)。
- 我独立重跑了
  `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*RentalDelivery*,*RentalTracking*,*RentalLogistics*,RentalCarrierMappingServiceTest,WaybillPrivacyTest' -Dsurefire.failIfNoSpecifiedTests=false test`，
  结果是 `17 tests run, 0 failures, 0 errors, 0 skipped`。
- `validation-log.jsonl` 已补上 MySQL 8.4 的 fresh-schema 与 current-upgrade
  证据，且两次 migration 032 验证都成功
  (`validation-log.jsonl:8-9`)。

## Error Handling

- `phoneRequirement=REQUIRED` 现在在 Delivery 创建阶段即被处理：缺少
  `trackingPhone` 时，subscribe/query 状态置为 `PHONE_REQUIRED`，写入明确
  error code/message，并阻止初始 `SUBSCRIBE`/`INITIAL_QUERY` 入队
  (`RentalDeliveryServiceImpl.java:92-97`,
  `RentalDeliveryServiceImpl.java:112-165`)。
- 这满足 brief 中“enqueue safe local work”的要求，错误不再延后到 worker
  阶段才暴露。

## Reuse / Duplication

- 归一化规则已收敛到 `RentalCarrierMappingService`，并通过
  `RentalCarrierResolution` 一次性返回 `sourceType/sourceCarrierCode/canonicalCarrierCode`
  (`RentalCarrierMappingService.java:19-29`)。
- `RentalDeliveryServiceImpl` 不再保留自己的 fallback canonical 规则；之前的重复与
  行为分叉已清除。

## Complexity Delta

- 任务 001 仍然引入了 7 张表、多个状态枚举和 Inbox/Outbox/快照三条内部数据流，
  但基础层关键不变量现在已经落地：稳定快照排序、`phoneRequirement` 约束、统一
  carrier normalization、重复设备关系先全量校验后去重。
- 当前复杂度与该基础 slice 的职责相匹配，没有再发现会阻塞后续 Provider/UI
  slice 的结构性问题。

## Required Fixes

- 无阻断级修复项。
- 本次 re-review 结论基于当前源码、当前测试、`validation-log.jsonl` 中新增的
  MySQL 8.4 证据，以及我独立重跑的定向 Maven 测试和 `git diff --check`。
