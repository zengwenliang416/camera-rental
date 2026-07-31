# Quality Review: 004-schedule-tracking

## Verdict

approved

## Separation Of Concerns

- Backend responsibilities are cleanly split: controller only exposes batch,
  detail, and refresh endpoints; query service assembles the local read model;
  refresh service performs only local validation plus outbox enqueue; risk
  service computes server authority
  (`camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/logistics/RentalDeliveryTrackingController.java:42-68`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingQueryService.java:94-341`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingRefreshService.java:48-95`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalLogisticsRiskService.java:27-120`).
- Frontend responsibilities are likewise split appropriately. The tracking
  provider owns shared summary/detail state and polling, while summary cards,
  device-row badges, drawer rendering, and exception-center actions stay
  presentational
  (`camera-rental-schedule-center/src/features/tracking/TrackingContext.tsx:82-325`,
  `camera-rental-schedule-center/src/features/tracking/components/DeliveryTrackingSummaryPanel.tsx:24-199`,
  `camera-rental-schedule-center/src/features/tracking/components/DeliveryTrackingDrawer.tsx:96-365`,
  `camera-rental-schedule-center/src/features/exceptions/ExceptionsPage.tsx:34-155`).

## Component Cohesion / Coupling

- `trackingByOrderId` is the single shared read model, so one order's tracking
  summary is not copied into every schedule block; both the schedule table and
  drawer read the same keyed summary and only fetch complete traces when needed
  (`camera-rental-schedule-center/src/features/tracking/trackingModel.ts:108-137`,
  `camera-rental-schedule-center/src/features/schedule/components/ScheduleDeviceTable.tsx:73-83`,
  `camera-rental-schedule-center/src/features/tracking/components/DeliveryTrackingDrawer.tsx:111-123`).
- The two concrete coupling regressions exposed by browser inspection were
  resolved in the current diff: detail loading no longer loops on changing
  callback identity, and tracking/device actions are sibling buttons rather than
  nested interactive controls
  (`camera-rental-schedule-center/src/features/tracking/TrackingContext.tsx:98-116`,
  `camera-rental-schedule-center/src/features/tracking/TrackingContext.tsx:174-227`,
  `camera-rental-schedule-center/src/features/schedule/components/ScheduleDeviceTable.tsx:202-231`,
  `openspec/changes/add-rental-logistics-tracking/development/validation-log.jsonl:43-46`).

## Test Quality

- My independent reruns passed on the current checkout:
  `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*TrackingQuery*,*TrackingRefresh*,*TrackingController*,*LogisticsRisk*,RentalLogisticsSensitiveFieldTest,WaybillPrivacyTest' -Dsurefire.failIfNoSpecifiedTests=false test`
  produced `15 tests run, 0 failures, 0 errors, 0 skipped`.
- My independent frontend rerun also passed:
  `cd camera-rental-schedule-center && pnpm exec tsx --test src/features/tracking/trackingModel.test.ts src/features/tracking/trackingPolling.test.ts src/features/exceptions/exceptionModel.test.ts src/features/providers/providerBehavior.test.tsx src/features/schedule/components/ScheduleDeviceTable.test.tsx`
  produced `21 tests, 21 passed`.
- Existing system-executed evidence remains consistent with the current source:
  focused backend validation, full `pnpm test && pnpm lint && pnpm build`,
  targeted post-fix tracking tests, and the final browser sensory matrix all
  passed
  (`openspec/changes/add-rental-logistics-tracking/development/validation-log.jsonl:31-46`).

## Error Handling

- Summary/detail flows classify safe errors, preserve the schedule shell, and
  suppress stale async responses rather than letting earlier requests overwrite
  newer session state
  (`camera-rental-schedule-center/src/features/tracking/TrackingContext.tsx:95-155`,
  `camera-rental-schedule-center/src/features/providers/providerBehavior.test.tsx:552-689`).
- Refresh handling returns stable local reason codes for not found, mapping
  required, provider disabled, query disabled, throttled, and already queued
  states without any synchronous provider query
  (`camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingRefreshService.java:52-95`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingRefreshServiceTest.java:71-123`).

## Reuse / Duplication

- The task reuses existing schedule-center shell components and keeps localized
  tracking copy centralized in one place rather than duplicating labels across
  the schedule table, summary panel, drawer, and exceptions page
  (`camera-rental-schedule-center/src/features/tracking/trackingCopy.ts:11-245`,
  `camera-rental-schedule-center/src/features/tracking/components/DeliveryTrackingSummaryPanel.tsx:24-199`,
  `camera-rental-schedule-center/src/features/tracking/components/DeliveryTrackingDrawer.tsx:96-365`,
  `camera-rental-schedule-center/src/features/exceptions/ExceptionsPage.tsx:103-130`).
- Backend masking/redaction is similarly reused through `WaybillPrivacy` and
  `SensitiveValueRedactor` instead of scattering ad hoc privacy logic across
  controller responses
  (`camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/WaybillPrivacy.java:11-29`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/SensitiveValueRedactor.java:10-19`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingQueryService.java:222-341`).

## Complexity Delta

- The slice adds meaningful UI and local-read-model complexity, but the new
  moving parts stay bounded: one shared tracking provider, one polling helper,
  one summary surface, one drawer surface, and one backend query/refresh/risk
  trio. I did not find accidental complexity that should block the slice after
  the two browser regressions were fixed.
- The remaining complexity is appropriate for the scope because the implementation
  now covers multi-package summaries, on-demand trace detail, async refresh
  states, risk rendering, locale/theme copy, and responsive rendering without
  spreading authoritative logistics logic into the browser.

## Required Fixes

- None recorded.
