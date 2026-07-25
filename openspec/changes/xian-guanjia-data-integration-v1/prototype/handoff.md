# Prototype Handoff: xian-guanjia-data-integration-v1

## Approved Branch Variant

- Approved branch: `data-flow`.
- Approved variant: read-only XianGuanJia synchronization through transactional
  device allocation, with incomplete channel data routed to manual review.

## Screens Or Flows

- Bounded shop order synchronization, raw/normalized persistence, and durable
  cursor advancement.
- Channel-order conversion, seller-remark date parsing, explicit SKU mapping,
  and manual-review routing.
- Device assignment with occupied half-open schedule conflict recheck.
- Admin sync, review, assignment, alert, replay, and report flows.

## Components To Create

- Rental API and Biz Maven modules, XianGuanJia integration client, sync
  services, rental domain services, and admin typed API/page modules.

## Components To Reuse

- Existing RuoYi common response, validation, Spring Security, tenancy,
  MyBatis, job, audit, Element Plus, theme, and vue-i18n infrastructure.

## Extraction Targets

- Canonical JSON signing, external-id normalization, redaction, window/cursor
  planning, retry classification, currency/date/status formatting, and repeated
  admin query state handling.

## API Contracts

- Backend-only third-party read requests for shops, products/SKUs, orders,
  after-sales, express companies, and documented push ingestion.
- Admin contracts under `/admin-api/xianyu/**` and `/admin-api/rental/**`.
- Assignment requests require typed occupied dates, idempotency key, and
  server-side permission and conflict validation.

## Data Flows

- Preserve raw plus normalized channel data before conversion.
- Convert one external order to zero or one rental order; keep missing mappings
  or invalid dates as review records without schedules.
- Persist an assignment and effective occupied schedule in one transaction;
  replays return the original accepted result.

## State Behavior

- Loading: sync and assignment actions disable duplicate primary actions.
- Empty: missing channel data, mappings, or device candidates explain the cause.
- Error: validation, authorization, external transport, replay, and schedule
  conflict responses are typed and redacted.
- Disabled: integration remains disabled unless runtime configuration is present.
- Permission: raw payload, sync execution, mapping correction, and assignment
  use separate backend permissions.

## Theme And Locale Policy

- Theme support: `light-dark`.
- Theme modes shown in prototype: `light`, `dark`.
- Theme toggle: intentionally omitted from this data-flow prototype; required
  in the production admin user interface.
- Internationalization: enabled.
- Locales shown in prototype: `zh-CN`, `en`; default is `zh-CN`.
- Locale switcher: intentionally omitted from this data-flow prototype; required
  in the production admin user interface.

## Out Of Scope Items

- Customer checkout, payment, deposit, real-name verification, customer/staff
  applications, Nuxt website work, fulfillment write operations, and every
  XianGuanJia write API.

## Required Tests

- MockWebServer exact signed-body, pagination window, cursor, retry,
  idempotency, redaction, alert, replay, conversion, date, money, concurrent
  assignment, reporting, permission, admin loading/empty/error/retry/theme, and
  locale tests.

## Open Risks

- External after-sale amount units and some optional source fields remain
  ambiguous; raw values and unit-confirmation state must be retained.
- Runtime shop authorization and webhook signature details require controlled
  environment verification after deployment configuration is supplied.
