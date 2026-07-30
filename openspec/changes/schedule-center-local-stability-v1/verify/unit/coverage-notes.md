# Unit Coverage Notes

- The backend rental-module suite executed 257 tests covering persisted configuration, tenant isolation, read and write clients, synchronization, webhooks, retry, order queries, remark reparsing, and shipment guards.
- The schedule-center suite executed 77 tests covering API adapters, authentication, permissions, preferences, provider lifecycle, scheduling, orders, devices, shipping, overlays, and safe errors.
- Critical asynchronous boundaries include stale request suppression, account switching, repeated 401 handling, distinct concurrent command keys, duplicate submission protection, and post-command snapshot refresh.
- Shipping coverage verifies that manual or OCR waybill input is retained independently from selected order fields and that write-disabled or incomplete gates cannot produce optimistic completion.
- Order coverage verifies that masked shared snapshots coexist with complete authorized delivery fields, receiver name/phone/address searches succeed, and page clamping never renders the full result set at once.
- No real third-party write was required for unit coverage.
