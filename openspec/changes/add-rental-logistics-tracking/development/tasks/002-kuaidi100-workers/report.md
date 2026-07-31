# Task Report: 002-kuaidi100-workers

## Status

DONE

## Files Changed

- Kuaidi100 provider adapter, signer, converter, HTTP gateway, configuration,
  callback receipt/service, and provider-neutral commands/results under
  `yudao-module-rental-biz`.
- Public form callback controller under `controller/webhook/logistics`.
- Inbox/Outbox lease, completion, worker, retry, provider registry,
  compensation services, and Quartz jobs under `service/logistics` and
  `job/logistics`.
- Focused MockWebServer, callback, worker, lease, retry, throttle, dedupe,
  payload-limit, and controller tests.
- This task report plus task validation, ledger, and drift evidence.

## What Changed

- Implemented official Kuaidi100 query signing, subscription envelopes,
  form callback ACK, provider response conversion, and platform status mapping.
- Split short transactional lease/result writes from provider network calls;
  workers themselves are non-transactional and convert unexpected runtime
  failures into bounded safe retries.
- Added `FOR UPDATE SKIP LOCKED` lease acquisition, five-minute lease recovery,
  bounded retry/backoff, `RETRY_WAIT`/`DEAD`, safe error codes, query throttling,
  monthly subscription attempt limits, and stale-delivery compensation tasks.
- Added per-Delivery callback token and salt generation. Callback URLs contain
  only the random token; callback token lookup and signature verification occur
  before encrypted Inbox persistence and ACK.
- Made callback payload persistence idempotent per Delivery with atomic MySQL
  upsert, then full-key locked lookup. Different Deliveries can persist the same
  Provider payload independently.
- Scoped callback token hash uniqueness to each tenant. Public callback lookup
  reads same-hash candidates through a non-unique lookup index, decrypts and
  constant-time matches the exact token, and rejects missing or ambiguous
  matches before entering a tenant context.
- Replaced missing-row lock/insert races in both Inbox and Outbox with
  `INSERT ... ON DUPLICATE KEY UPDATE id = id`.
- Added claim-order indexes and changed Inbox/Outbox leasing to primary-key
  order after MySQL 8.4 proved the previous filesort could lock an entire
  candidate batch and starve a second `SKIP LOCKED` worker.
- Separated credential requirements: subscription requires an API key, while
  active query additionally requires the Kuaidi100 customer authorization code.
- Kept every ordinary test on fictional credentials and local MockWebServer
  endpoints. No real provider request was issued.

## TDD Evidence

- `Kuaidi100LogisticsProviderTest`: official query form/signature, subscription
  envelope, subscription key-only credential boundary, missing query customer
  reason code, and local MockWebServer isolation.
- `Kuaidi100CallbackServiceTest` and
  `Kuaidi100TrackingWebhookControllerTest`: token lookup, signature rejection,
  tenant restoration, Inbox-before-ACK, oversized payload rejection, official
  form binding, and protocol ACK JSON.
- Inbox tests: duplicate payload reuse, concurrent duplicate insert recovery,
  cross-Delivery payload isolation, expired lease reclaim, safe worker exception
  conversion, bounded retry, and DEAD transition.
- Outbox tests: transaction boundary, query throttle, per-Delivery callback
  identity, monthly subscription limit, safe worker exception conversion,
  bounded retry, and DEAD transition.
- `RentalAsyncRetryPolicyTest`: exponential backoff with a six-hour cap and
  bounded retry count.
- `RentalLogisticsMysqlConcurrencyTest`: real MySQL 8.4 concurrent Inbox and
  Outbox upsert, cross-Delivery identical payloads, tenant-scoped callback-token
  uniqueness with cross-tenant reuse, and two-worker `FOR UPDATE SKIP LOCKED`
  behavior.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests compile`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*Kuaidi100*,*OutboxWorker*,*OutboxLease*,*OutboxCompletion*,*InboxWorker*,*InboxLease*,*InboxCompletion*,*InboxServiceImpl*,*RetryPolicy*' -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*RentalDelivery*,*RentalTracking*,*RentalLogistics*,*Kuaidi100*,WaybillPrivacyTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- `RENTAL_LOGISTICS_MYSQL_* mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='RentalLogisticsMysqlConcurrencyTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- Disposable MySQL 8.4 fresh logistics-schema execution and index assertions.
- `git diff --check`
- Static scan for real credentials and vendor SDK imports.
- Static inspection that tests construct `Kuaidi100HttpGateway` only with
  MockWebServer URLs.

## Concerns

- A real query probe is intentionally deferred because the user-provided API key
  is not persisted in source and the corresponding customer authorization code
  has not been supplied.
- The first real MySQL concurrency run exposed that leasing ordered by
  `scheduled_at` could filesort and lock the full candidate batch. That failed
  run was retained as diagnostic evidence; the claim indexes and primary-key
  ordering fix then passed all five database-backed tests.
- The SpecNav entry contract currently reports `git-baseline:tasks-not-tracked`
  because the whole active Change is still untracked. No staging, commit, push,
  deployment, or production configuration change was performed.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Independent specification and quality reviews must approve this slice before
  `tasks.md` is checked and task 003 modifies the Xianyu shipment transaction.
- Final integrated verification must exercise subscription/query/callback using
  MockWebServer plus MySQL, without contacting the real provider.

## Adjudication

The first independent reviews returned `needs-fix` for cross-Delivery Inbox
dedupe and callback-token tenant isolation. Both findings are fixed with
database constraints, atomic upsert paths, and replayable MySQL 8.4 concurrency
tests. Independent specification and quality re-reviews both approved the
current implementation.
