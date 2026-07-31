# Task Brief: 002-kuaidi100-workers

## Goal

Operators can receive subscription, query, and verified callback updates from
Kuaidi100 through recoverable asynchronous processing.

## Parent Artifacts

- `openspec/changes/add-rental-logistics-tracking/requirements.md`
- `openspec/changes/add-rental-logistics-tracking/acceptance.md`
- `openspec/changes/add-rental-logistics-tracking/prototype/handoff.md`

## Vertical Slice

From a pending Outbox or verified public callback through Provider conversion,
network-isolated execution, Inbox processing, and local snapshot advancement,
produce a durable tracking update.

## In Scope

- Provider-neutral commands/results, Kuaidi100 gateway and converter, callback
  verifier, public webhook, Outbox/Inbox workers, leases, bounded retry, query
  throttle, subscription-attempt policy, compensation job, and fixtures.

## Out Of Scope

- No Xianyu shipment integration, schedule-center UI, credential management UI,
  historical backfill execution, deployment, or real Provider call in tests.

## Files Allowed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `openspec/changes/add-rental-logistics-tracking`

## Interfaces / Seams

- `LogisticsProvider`, `Kuaidi100Gateway`, callback verifier, worker lease
  services, snapshot service, config service, and mapping service.

## Components To Create

- Kuaidi100 adapter package, gateway, signer/verifier, converters, Outbox worker,
  Inbox worker, compensation job, query/subscription policies, and webhook VO.

## Components To Reuse

- OkHttp, Jackson, rental job starter, transaction template, Inbox/Outbox
  persistence, snapshot aggregator, carrier mappings, and masked error policy.

## Components To Extract

- Provider request signing, raw response conversion, status mapping, callback
  verification, lease policy, bounded backoff, and query throttle.

## API / Data Flow Contracts

- `SUBSCRIBE`, `INITIAL_QUERY`, `REFRESH_QUERY`, and `RECONCILE` consume local
  tasks; public callback verifies token/signature and persists Inbox before ACK.

## State / Error / Empty / Loading Behavior

- Loading: workers expose leased processing state without long transactions.
- Empty: empty Provider traces do not create an effective snapshot.
- Error: retryable and final failures use bounded categories and masked details.
- Disabled: Provider/query/subscription switches stop network calls safely.
- Permission: public callback uses token and signature, not admin authentication.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest='*Kuaidi100*,*OutboxWorker*,*InboxWorker*' test`
- `rg -n 'kuaidi100' camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.
- Official callback/signature or query contract cannot be verified.
- A test would contact a real Provider endpoint.

## Unsafe Assumptions

- Do not infer undocumented Kuaidi100 status or signature semantics.
- Do not assume callbacks are ordered, unique, or delivered once.
- Do not hold a database transaction across an HTTP call.
