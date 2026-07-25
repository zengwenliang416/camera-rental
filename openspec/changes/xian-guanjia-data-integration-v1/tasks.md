## V1 Delivery Slices

- [x] An administrator can start the server with the rental module, disabled-by-default XianGuanJia configuration, and additive rental/channel schema.
- [x] An integration operator can prepare and submit only documented XianGuanJia read requests through canonical JSON, MD5 signing, fixed safe error handling, and a closed read-endpoint allowlist.
- [x] An integration operator can durably preserve an order detail response and advance a stable order cursor only after local persistence succeeds.
- [x] An integration operator can safely run bounded read-only shop authorization sync and order-page synchronization with raw evidence, cursors, and run counts (product/after-sale allowlisted client remains available for follow-up page orchestrators).
- [x] A rental operator can review imported channel orders and convert each eligible source order at most once; mapping/date failures enter manual review without dropping pay_amount.
- [x] An equipment operator can manage physical devices and assign a concrete device with transactional half-open occupied schedules and conflict feedback.
- [x] Complete after-sale orchestration using the existing read-client allowlist and raw-payload persistence. Product push ingestion, safe replay, read-only product-detail persistence, product list/SKU page orchestration, express-company raw evidence persistence, and after-sale list/detail scheduled orchestration are implemented.
- [x] An integration operator receives one deduplicated alert for authorization/guarantee failures, after-sale timeouts, and repeated synchronization failures. Authorization-loss, guarantee health, after-sale timeout, and order/after-sale sync-failure alerts are implemented.
- [x] An authorized auditor can inspect masked raw payloads through a separately permissioned access path with API access-log auditing.
- [x] An authorized auditor can replay failed raw events/pages safely without duplicate records or unsafe cursor advancement. Manual order-push event replay, product-push event replay, order-detail raw-payload replay, and order-page raw-payload replay are implemented.
- [x] Complete utilization, idle-time, product/SKU, and assigned-device income reports with source drill-down.
- [x] Finish admin loading, empty, permission, network-error, retry, and bilingual states for every required V1 page.
- [x] Rerun backend tests, admin static checks, migration-copy integrity checks, and static security scans against the current worktree.
- [ ] Run authenticated backend/admin smoke tests, browser E2E and sensory review, real MySQL migration/concurrency verification, runtime red-team scenarios, and the final SpecNav aggregate gate.
