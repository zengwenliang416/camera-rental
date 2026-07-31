# Quality Review: 002-kuaidi100-workers

## Verdict

approved

## Separation Of Concerns

- `RentalDeliveryOutboxWorker` / `RentalDeliveryInboxWorker` 仍然把 Provider 调用
  放在非事务方法里，而领取租约和完成写回分别落在独立的
  `@Transactional` Service 中；网络隔离边界没有回退
  (`RentalDeliveryOutboxWorker.java:26-36`,
  `RentalDeliveryOutboxLeaseService.java:46-62`,
  `RentalDeliveryOutboxCompletionService.java:40-89`,
  `RentalDeliveryInboxWorker.java:25-37`,
  `RentalDeliveryInboxLeaseService.java:26-43`,
  `RentalDeliveryInboxCompletionService.java:30-59`)。
- 前次阻断的 Inbox 幂等边界已修复。`accept()` 现在按
  `tenant + provider + delivery + payload_hash` 构造落库/回查边界，
  不再把不同 Delivery 的相同 payload 混为同一条记录
  (`RentalDeliveryInboxServiceImpl.java:45-61`,
  `RentalDeliveryCallbackInboxMapper.java:17-44`,
  `20260731_032_rental_delivery_tracking.sql:106-133`)。

## Component Cohesion / Coupling

- Provider 适配器、HTTP 网关、签名器和 webhook controller 的边界仍然清晰；
  SDK/协议细节继续被限制在 `integration/logistics/kuaidi100`。
- `callback_token_hash` 现在提升为全局唯一键，匹配 callback service 的
  non-tenant lookup 方式，tenant 恢复边界与数据库约束已经一致
  (`Kuaidi100CallbackService.java:39-48`,
  `20260731_032_rental_delivery_tracking.sql:47-55`)。
- Inbox/Outbox 的“原子 upsert + 锁定回查”模型现在由 mapper 原生承载，
  service 不再依赖“先查不存在再插入”的竞态路径
  (`RentalDeliveryCallbackInboxMapper.java:17-35`,
  `RentalDeliveryOutboxMapper.java:17-33`,
  `RentalDeliveryInboxServiceImpl.java:45-61`,
  `RentalDeliveryOutboxServiceImpl.java:44-58`)。

## Test Quality

- 新增单测已经覆盖前次缺口：`scopesSamePayloadHashToDelivery`
  证明相同 payload hash 会按 Delivery 隔离
  (`RentalDeliveryInboxServiceImplTest.java:67-78`)。
- 新增 `RentalLogisticsMysqlConcurrencyTest` 直接覆盖真实数据库层面的五个关键契约：
  同 Delivery 回调用同一 Inbox、跨 Delivery 相同 payload 独立持久化、Outbox
  dedupe 原子复用、`FOR UPDATE SKIP LOCKED` 双 worker 领取，以及
  callback token hash 全局唯一
  (`RentalLogisticsMysqlConcurrencyTest.java:63-114`)。
- 我本地重跑了
  `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='RentalDeliveryInboxServiceImplTest,RentalDeliveryOutboxServiceImplTest,RentalLogisticsMysqlConcurrencyTest' -Dsurefire.failIfNoSpecifiedTests=false test`，
  结果为 `Tests run: 10, Failures: 0, Errors: 0, Skipped: 5`。
  其中 5 个跳过来自本机当前未设置 `RENTAL_LOGISTICS_MYSQL_*` 环境变量，并非断言失败。
- 当前 task 的 `validation-log.jsonl` 已补齐 system-executed MySQL 8.4 证据：
  atomic Inbox/Outbox upsert 通过、migration 唯一键断言通过、首次 `SKIP LOCKED`
  filesort 失败被保留为诊断证据、修复后 5/5 DB tests 通过
  (`validation-log.jsonl:18-23`)。

## Error Handling

- Worker 层仍把运行时异常收敛为安全 code 并交给有界退避策略处理；
  这一部分实现没有退化。
- 缺失行竞态现在由原子 upsert 消化，随后用全键 `FOR UPDATE` 回查取得持久化 id。
  这比旧的“select-then-insert + DuplicateKeyException 补救”路径更直接，也更容易
  与数据库唯一键保持一致
  (`RentalDeliveryInboxServiceImpl.java:45-61`,
  `RentalDeliveryOutboxServiceImpl.java:44-58`)。

## Reuse / Duplication

- Query/subscribe/callback 的协议转换与状态映射复用良好，没有把快递100细节扩散到
  worker 或 controller 层。
- Inbox/Outbox 都收敛到相同的“数据库唯一键定义幂等，mapper 提供
  `insertOrReuse`，service 只做安全回查”的模式，复用方向一致，没有再保留两套
  竞态处理分支。

## Complexity Delta

- 这一 slice 引入了 Provider、callback、Inbox/Outbox、lease、retry 和补偿任务，
  复杂度本来就不低；但在当前版本里，复杂度已经被压回到清晰的边界上：
  schema 约束、mapper upsert、service 协调、worker 执行、DB 并发测试互相对应。
- MySQL 8.4 首次把 `scheduled_at` 排序的 filesort 锁批量问题暴露出来，而当前修复
  通过 claim index 和 `ORDER BY id` 消除了这条隐蔽复杂度路径；这是积极的复杂度
  收敛，不是新的维护负担
  (`RentalDeliveryCallbackInboxMapper.java:46-62`,
  `RentalDeliveryOutboxMapper.java:41-57`,
  `20260731_032_rental_delivery_tracking.sql:129-160`,
  `validation-log.jsonl:21-22`)。

## Required Fixes

- 无阻断级修复项。
- 本次 re-review 结论基于当前源码、当前测试、我本地重跑的定向 Maven 用例，
  以及 `validation-log.jsonl` 中新增的 MySQL 8.4 schema/并发证据。
