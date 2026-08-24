## 1. Local-Only Operator Contract

用户结果：店铺管理员能够明确知道该能力只修复已经外部发货的本地履约事实，不会再次触发闲管家发货，也不依赖扫码设备。

- [x] 1.1 Define the local-only dispatch backfill requirements, acceptance assertions, capability delta, and technical design.
- [x] 1.2 Lock exclusions for remote XianGuanJia writes, scanner hardware, pending/refunded/closed orders, new migrations, and new configuration.
- [x] 1.3 Record accepted limitations for tenant-level authorization, one-device-per-waybill behavior, and first-order-item selection.

## 2. Transactional Local Dispatch Repair

用户结果：管理员提交实际设备和运单信息后，系统以一个事务恢复订单、设备、排期、Shipment 与 Delivery 的一致状态，失败时不留下部分写入。

- [x] 2.1 Add the validated `XianyuOrderDispatchBackfillReqVO` and `POST /rental/xianyu/order/dispatch-backfill` endpoint protected by `rental:xianyu:ship`.
- [x] 2.2 Implement status `21`/`22`, cancellation, tenant-shop, device, conversion, assignment, and occupied-schedule validation.
- [x] 2.3 Implement local assignment/dispatch, `ADMIN_BACKFILL` Shipment, outbound Delivery, and channel-order logistics updates in one transaction.
- [x] 2.4 Implement normalized idempotent replay, request-hash validation, business-key conflict handling, and typed backfill errors.
- [x] 2.5 Prove the backfill path does not read Xianyu write configuration or call `XianyuWriteClient`.

## 3. Web-Admin Backfill Experience

用户结果：有权限的管理员能在现有渠道订单页面通过键盘完成补录，看到明确校验、加载、冲突和成功反馈。

- [x] 3.1 Add the typed dispatch-backfill request and API method in the existing Xianyu admin API module.
- [x] 3.2 Add the status `21`/`22` and permission-gated order-row action.
- [x] 3.3 Add `XianyuDispatchBackfillDialog` with existing logistics defaults, device input, actual ship time, reason, validation, loading, warning, and completion events.
- [x] 3.4 Add complete `zh-CN` and `en` copy for the action, form, warning, validation, and success result.

## 4. Regression-Safe Delivery

用户结果：关键资格、租户隔离、幂等、冲突、回滚和无远端写调用都具备可重复执行的自动化证据。

- [x] 4.1 Cover successful local backfill, pending-order rejection, idempotency-key conflict, matching replay, and existing-dispatched-assignment reuse in `XianyuOrderShipServiceTest`.
- [x] 4.2 Add focused tests for refunded/closed order rejection, cross-tenant shop rejection, same-waybill different-device conflict, unmapped-order conversion, and Delivery-failure rollback.
- [x] 4.3 Run the targeted rental backend test set and record the exact system-executed result in SpecNav development evidence.
- [x] 4.4 Run admin type check, targeted ESLint, targeted Prettier, and `git diff --check`, then record exact system-executed results.

## 5. Approved Delivery Gates

用户结果：批准原型、生产实现和 Verification 2.0 之间有可追踪且不可绕过的交接证据。

- [x] 5.1 Create and verify the approved prototype branch covering the Web dialog states, themes, locales, and narrow layout.
- [x] 5.2 Obtain explicit human approval for the verified prototype and create the prototype-to-development handoff.
- [ ] 5.3 Complete SpecNav development entry, task evidence, independent reviews, and development handoff without bypassing the tracked-baseline requirement.

## Verification 2.0 Follow-On

用户结果：正式测试仍需通过不可变用例快照审批、六域执行和机器权威报告，且这些义务不阻塞开发交接本身。

Verification obligation: create the immutable six-domain case snapshot and
obtain explicit human approval for its ID and SHA-256.

Verification obligation: execute all six domains and generate the
machine-authoritative report only after snapshot approval.
