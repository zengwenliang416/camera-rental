# Quality Review: 003-xianyu-shipment-delivery

## Verdict

approved

## Separation Of Concerns

- `XianyuOrderShipService` 仍然只负责 shipment orchestration：remote ship、
  device dispatch、本地 shipment audit、Delivery 接入与响应组装都保持在既有
  业务边界内；shipment 路径只依赖 provider-neutral 的
  `RentalDeliveryService` 与 `WaybillPrivacy`，没有把 Kuaidi100 adapter、
  gateway 或 direct Provider call 拉回事务路径
  (`XianyuOrderShipService.java:130-186`)。
- 这次最新修复已经清掉前一轮阻断问题。legacy replay 分支直接复用
  `waybillPrivacy.mask(...)`，linked Delivery replay 分支通过
  `deliveryService.getResult(...)` 取得由 `RentalDeliveryServiceImpl` 用同一个
  `WaybillPrivacy` 组件生成的 `maskedWaybillNo`，不再存在私有 masking helper
  (`XianyuOrderShipService.java:131-135`,
  `XianyuOrderShipService.java:329-356`,
  `RentalDeliveryServiceImpl.java:169-180`,
  `WaybillPrivacy.java:11-29`)。

## Component Cohesion / Coupling

- `buildDeliveryCommand(...)` 把 Delivery command 组装从主流程抽出，`ship(...)`
  仍保持清晰的顺序式编排；`toShipResp(...)` 只消费 shipment + tracking read model，
  没有重新承担 Provider 状态映射职责
  (`XianyuOrderShipService.java:174-185`,
  `XianyuOrderShipService.java:317-356`)。
- `delivery_id` 仍然只作为 shipment 审计与 Delivery 聚合之间的连接点保存到
  `RentalDeviceShipmentDO`；具体的 mapping/subscribe/query 状态继续来自
  `RentalDeliveryResult`，耦合边界合理
  (`RentalDeviceShipmentDO.java:25-36`,
  `XianyuOrderShipService.java:174-177`,
  `XianyuOrderShipService.java:345-354`)。

## Test Quality

- 我独立重跑了用户指定命令：
  `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='RentalDeliveryServiceImplTest,XianyuOrderShipServiceTest,*ShipmentDelivery*' -Dsurefire.failIfNoSpecifiedTests=false test`。
  2026 年 7 月 31 日本地结果是 `24 tests run, 0 failures, 0 errors, 0 skipped`，
  不是“33 tests passed”。
- 这组通过的测试已经覆盖本次 re-review 重点：shipment -> Delivery command 字段和
  顺序、`delivery_id` 回写、legacy replay 走 `WaybillPrivacy` 后预期
  `SF5****2626`、linked Delivery replay 不重复建任务、mapping/provider degradation
  不改变 shipment success、以及 Delivery service replay 预期 `SF1****7890`
  (`XianyuOrderShipServiceTest.java:183-245`,
  `XianyuOrderShipServiceTest.java:368-427`,
  `XianyuOrderShipServiceTest.java:430-473`,
  `RentalDeliveryServiceImplTest.java:123-141`)。
- `git diff --check` 也已独立重跑并返回 0，无 whitespace/blocking format issue。

## Error Handling

- `ship(...)` 仍保持 `@Transactional(rollbackFor = Exception.class)`；当
  `deliveryService.createOrReuse(...)` 抛出 `RentalLogisticsException` 时，测试证明
  remote ship 与 dispatch 之后不会继续执行 `shipmentMapper.updateById(...)` 或
  `orderMapper.updateById(...)`，符合“本地失败阻断后续写入”的 slice 要求
  (`XianyuOrderShipService.java:129-187`,
  `XianyuOrderShipServiceTest.java:460-473`)。
- replay 路径现在显式区分“legacy shipment 无 Delivery”与“已有 Delivery 的 replay”。
  前者返回稳定 reason code `LEGACY_SHIPMENT_WITHOUT_DELIVERY`，后者读取当前
  Delivery 状态；这避免了旧数据无条件触发 Delivery lookup 的硬失败
  (`XianyuOrderShipService.java:131-135`,
  `XianyuOrderShipService.java:345-354`,
  `XianyuOrderShipServiceTest.java:368-427`)。

## Reuse / Duplication

- 阻断级重复已消除。`XianyuOrderShipService` 不再维护私有 `maskWaybill(...)`；
  legacy replay 和 Delivery replay 现在都遵守 `WaybillPrivacy` 的同一套
  normalize/mask 契约
  (`XianyuOrderShipService.java:337-338`,
  `RentalDeliveryServiceImpl.java:177-180`,
  `WaybillPrivacy.java:11-29`)。
- Delivery 创建、pending event 汇总与 degrade state 仍然统一经
  `RentalDeliveryService` / `RentalDeliveryResult` 返回，没有在 shipment service
  里复制 Provider 语义或 Outbox 细节
  (`XianyuOrderShipService.java:174-185`,
  `RentalDeliveryServiceImplTest.java:67-98`)。

## Complexity Delta

- 这次改动引入了更多 response 字段和 Delivery 接线，但主复杂度仍然集中在一个
  明确的 orchestration service 与一个 logistics service 上，没有把 task 003
  推向多处状态机分叉。
- 最新修复反而降低了长期复杂度：replay 仍然保留“有 Delivery / 无 Delivery”两条
  数据形态分支，但隐私策略已收敛为单一组件，后续 004/005 再消费这些响应时不会再
  背负双份 masking 规则。

## Required Fixes

- None recorded.
