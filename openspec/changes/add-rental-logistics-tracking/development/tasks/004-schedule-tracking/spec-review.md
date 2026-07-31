# Spec Review: 004-schedule-tracking

## Verdict

approved

## Missing Requirements

- None recorded. The reviewed diff implements the task brief's local batch
  summary, on-demand detail, async refresh, and logistics-risk surfaces through
  the explicit tracking controller split
  (`camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/controller/admin/logistics/RentalDeliveryTrackingController.java:42-68`),
  the shared `trackingByOrderId` read model plus on-demand detail loading
  (`camera-rental-schedule-center/src/features/tracking/TrackingContext.tsx:90-172`,
  `camera-rental-schedule-center/src/features/tracking/TrackingContext.tsx:174-289`),
  and the exception-center merge path
  (`camera-rental-schedule-center/src/features/exceptions/exceptionModel.ts:26-55`,
  `camera-rental-schedule-center/src/features/exceptions/ExceptionsPage.tsx:41-155`).

## Extra Behavior

- None recorded. The code stays inside the approved local-summary/on-demand-
  detail/async-refresh boundary and does not add browser-side provider calls or
  device lifecycle mutation
  (`camera-rental-schedule-center/src/features/tracking/TrackingContext.tsx:118-172`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingRefreshService.java:48-95`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalLogisticsRiskService.java:27-120`).

## Misunderstood Requirements

- None recorded. Manual refresh only enqueues local outbox work and returns
  stable reason codes plus `nextAllowedAt` instead of waiting for provider I/O
  (`camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingRefreshService.java:74-91`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingRefreshServiceTest.java:50-123`).
- Logistics risks remain server-derived and are only rendered by the frontend;
  the browser normalizes display state but does not recalculate provider
  throttling, risk, or lifecycle authority
  (`camera-rental-schedule-center/src/features/tracking/trackingModel.ts:152-247`,
  `camera-rental-schedule-center/src/features/exceptions/exceptionModel.ts:26-55`).

## Cannot Verify From Diff

- None. I verified the task against current source, current tests, my own
  targeted reruns, and `validation-log.jsonl` entries with
  `attestation: "system-executed"`.
- A7, A8, A9, and A10 are supported by the current controller/query/refresh/risk
  implementation plus focused backend/frontend tests
  (`camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingQueryServiceTest.java:65-137`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/RentalDeliveryTrackingRefreshServiceTest.java:50-123`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/RentalLogisticsRiskServiceTest.java:21-99`,
  `camera-rental-schedule-center/src/features/tracking/trackingPolling.test.ts:6-80`,
  `camera-rental-schedule-center/src/features/providers/providerBehavior.test.tsx:552-689`,
  `camera-rental-schedule-center/src/features/schedule/components/ScheduleDeviceTable.test.tsx:27-120`) and by the existing system-executed records
  (`openspec/changes/add-rental-logistics-tracking/development/validation-log.jsonl:31-46`).
- A11 is supported by encrypted-field and safe-surface tests in the current
  checkout, including masked config/task views and masked ordinary UI
  (`camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/RentalLogisticsSensitiveFieldTest.java:18-42`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/operations/RentalLogisticsOperationsConfigurationServiceTest.java:40-61`,
  `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/cn/iocoder/yudao/module/rental/service/logistics/operations/RentalLogisticsOperationsTaskServiceTest.java:42-65`,
  `camera-rental-schedule-center/src/features/tracking/components/DeliveryTrackingDrawer.tsx:198-260`,
  `openspec/changes/add-rental-logistics-tracking/development/validation-log.jsonl:39-40`).
- A16 is supported by bilingual copy/theming code and the final clean browser
  sensory matrix after the earlier detail-loop and nested-button regressions
  were fixed
  (`camera-rental-schedule-center/src/features/tracking/trackingCopy.ts:11-245`,
  `openspec/changes/add-rental-logistics-tracking/development/validation-log.jsonl:43-46`).

## Acceptance Assertions Verified

- A7
- A8
- A9
- A10
- A11
- A16

## Required Fixes

- None recorded.
