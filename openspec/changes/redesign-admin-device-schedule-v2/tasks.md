# Vertical Slices

## 1. Schedule workbench read contract

User outcome: An operator can load authoritative capacity, device lanes and
effective occupied segments for a selected 14, 30 or 90 day window.

- [x] 1.1 Add workbench request/response VOs for window, device pagination, filters, metrics, device lanes, segments and exception summaries.
- [x] 1.2 Add indexed batch queries and a tenant-aware workbench service without per-device database or external logistics calls.
- [x] 1.3 Add controller permission checks and unit tests for device pagination, half-open overlap, long-rental continuation and returned-pending-inspection occupancy.

## 2. Pending allocation and candidate decisions

User outcome: An operator can inspect pending or partially allocated items,
review safe candidates and confirm a device without bypassing transaction rules.

- [x] 2.1 Add internal rental order pending-allocation page and detail contracts with required, assigned and remaining quantities.
- [x] 2.2 Add candidate query service and reason codes for model, state, effective schedule, logistics, inspection and confirmed lock policy.
- [x] 2.3 Reuse the existing transactional assignment write path and add tests for stale candidates, concurrent conflict, idempotency and partial allocation.

## 3. Device, logistics and exception details

User outcome: An operator can understand why a device is unavailable and when it
may be released without entering warehouse execution screens.

- [x] 3.1 Add device schedule-detail contract combining active assignment, effective schedules, delivery relations, logistics snapshots, inspection and maintenance state.
- [x] 3.2 Add window-scoped scheduling exception projection for candidate conflicts, return delay, logistics risk, pending inspection and schedule-affecting review.
- [x] 3.3 Reuse tracking detail and refresh APIs, with permission, tenant, redaction and no-external-call-on-list tests.

## 4. Classified device locks

User outcome: A supervisor can reserve or hold a device with an audited reason,
while inspection and maintenance automatically isolate unsafe equipment.

- [x] 4.1 Add the incremental `rental_device_lock` migration, enums, DO, Mapper, indexes and `rental:device-lock:update` permission.
- [x] 4.2 Implement transactional create, automatic expiry and permitted release for order holds and supervisor manual holds.
- [x] 4.3 Integrate return-inspection and maintenance locks with their owning lifecycle transitions without allowing manual bypass.
- [x] 4.4 Exclude active locks from candidates and final assignment, and add concurrency, tenant, permission and audit tests.

## 5. Admin schedule V2 shell

User outcome: An operator can navigate a management-style workbench with stable
filters, metrics and hundreds-device pagination.

- [x] 5.1 Extend typed schedule APIs and pure timeline models for 14/30/90 windows, cross-month columns, clipping, continuation markers and query mapping.
- [x] 5.2 Refactor `RentalSchedule` into a coordinator and extract metrics, filters and device timeline components using existing Element Plus patterns.
- [x] 5.3 Add device keyword search, model/device/logistics filters, 25/50/100 pagination, loading, empty, error and permission states.

## 6. Admin allocation and detail flows

User outcome: An operator can open pending orders, compare candidates, confirm
assignment and inspect device and logistics details in right-side drawers.

- [x] 6.1 Add pending-allocation and recommendation panels with order detail drawer and authoritative remaining quantities.
- [x] 6.2 Add device and logistics drawers with effective schedules, tracking process, risk and expected-release information.
- [x] 6.3 Wire transactional assignment, stale-candidate refresh and scheduling exception navigation without warehouse execution actions.
- [x] 6.4 Add complete zh-CN/en copy and verify existing light/dark theme integration.

## 7. Verification and handoff

User outcome: The production workbench is demonstrably safe, scalable and
consistent with the approved prototype.

- [x] 7.1 Run focused Maven tests for workbench, candidate, assignment and logistics behavior.
- [x] 7.2 Run `pnpm test:schedule`, `pnpm ts:check`, production build and `git diff --check`.
- [ ] 7.3 Verify browser flows with real backend data for search, pagination, 14/30/90 ranges, long rental, drawers, pending allocation and conflict refresh.
- [x] 7.4 Record verification evidence, remaining operational configuration and any migration or deployment risk.
