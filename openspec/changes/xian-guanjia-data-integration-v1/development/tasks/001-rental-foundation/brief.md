# Task Brief: 001-rental-foundation

## Goal

An administrator can start the server with the rental module enabled, safe
XianGuanJia runtime configuration defaults, and additive channel/rental tables
available for the later read-only synchronization flow.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`

## Vertical Slice

The server composition exposes the rental domain as a deployable capability
without enabling credentials or calling a third party. The data layer provides
stable identifiers and indexes needed by an operator-facing sync/review flow.

## In Scope

- Add Rental API and Biz Maven modules and wire them into the reactor and
  `yudao-server`.
- Add disabled-by-default runtime configuration placeholders sourced only from
  environment variables.
- Add the first additive MySQL migration for channel, review, rental order,
  device, schedule, assignment, cursor, alert, and raw-payload records.
- Add focused module tests that prove configuration defaults are safe and the
  foundation types compile.

## Out Of Scope

- No XianGuanJia network call, credential value, write endpoint, customer
  checkout, payment, device assignment API, or admin page implementation.

## Files Allowed

- `camera-rental-server/pom.xml`
- `camera-rental-server/yudao-server/**`
- `camera-rental-server/yudao-module-rental/**`
- `camera-rental-server/sql/mysql/migrations/**`
- `openspec/changes/xian-guanjia-data-integration-v1/**`

## Interfaces / Seams

- Maven reactor and `yudao-server` compose the new module.
- Runtime properties remain backend-only and disabled without environment
  credentials.
- The migration is additive and preserves raw-channel audit boundaries for
  subsequent services.

## Components To Create

- `yudao-module-rental-api`, `yudao-module-rental-biz`, and typed
  `XianyuProperties` configuration.

## Components To Reuse

- Existing RuoYi parent POM, module packaging, Spring Boot configuration
  binding, MyBatis tenant/audit conventions, and server YAML structure.

## Components To Extract

- Later integration clients share one configuration and redaction boundary;
  later rental services share the foundation entity names and table invariants.

## API / Data Flow Contracts

- The module must compile without `XGJ_APP_KEY` or `XGJ_APP_SECRET`.
- The generated migration creates only additive tables/indexes and no
  third-party write capability.

## State / Error / Empty / Loading Behavior

- Disabled: integration reports disabled until `rental.xianyu.enabled=true`
  and both runtime credential variables are supplied.
- Error: incomplete configuration must fail with a safe, non-secret message.
- Permission: no new operator endpoint is exposed in this slice.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `./mvnw -pl yudao-module-rental -am test`
- `./mvnw -pl yudao-server -am test -DskipTests`
- `git diff --check`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- Do not guess third-party endpoint schemas or introduce any write operation.
- Stop if the current Maven layout cannot compose an API/Biz child module.
