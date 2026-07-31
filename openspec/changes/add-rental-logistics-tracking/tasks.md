# Vertical Slices

- [x] 001 Operators can create or reuse a tenant-safe physical Delivery with multiple devices, durable tracking storage, and PII-free asynchronous tasks while existing shipment behavior remains compatible.
- [x] 002 Operators can receive Kuaidi100 subscription, query, and verified callback updates through bounded Inbox and Outbox workers without holding business transactions or calling the real Provider in tests.
- [x] 003 Shipping operators can complete a Xianyu shipment and immediately see the linked Delivery and queued tracking state even when mapping or Provider configuration is unavailable.
- [x] 004 Schedule operators can view batched single-package or multi-package summaries, open complete local traces, request an asynchronous refresh, and see logistics risks in light/dark and zh-CN/en layouts.
- [x] 005 Tenant administrators can configure masked Provider credentials and carrier mappings, inspect or retry failed tasks, run reconcile or dry-run backfill, and apply safe retention cleanup with permissions and metrics.
- [x] 006 Tenant administrators can configure multiple encrypted Kuaidi100 credential pairs, while each Delivery receives a stable tenant-safe credential assignment and reselects only when its bound credential becomes unusable.
