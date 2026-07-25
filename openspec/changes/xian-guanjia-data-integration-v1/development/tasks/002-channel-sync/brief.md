# Task Brief: 002-channel-sync

## Goal

An integration operator can execute only documented XianGuanJia read requests
through one canonical JSON, signing, transport, redaction, and error boundary.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`

## Vertical Slice

The backend can prepare a signed read request for authorized shops, product
metadata, products, SKUs, orders, after-sales, and express companies, send the
same UTF-8 JSON bytes that were signed, and return a structured raw response or
a safe internal failure. Durable sync, replay, and admin APIs remain later
slices.

## In Scope

- Implement a canonical compact JSON serializer and MD5 signer using the
  official self-developed application contract.
- Implement an OkHttp read client whose endpoint enum contains only documented
  read paths.
- Support authorized shops, product categories and attributes, product list and
  detail, SKU list, order list and detail, after-sale list and detail, and
  express-company queries.
- Add a signature verifier for the documented order push raw body; do not add a
  callback controller before durable event storage exists.
- Add error classification and JSON redaction utilities that never include
  secrets, signatures, phone numbers, addresses, or identity fields in normal
  exception messages.
- Add MockWebServer tests for signed-body byte identity, read endpoint routing,
  disabled configuration, and remote error classification.

## Out Of Scope

- No real API invocation, credential, third-party write endpoint, product
  mutation, delivery, price change, or refund decision.
- No database persistence, cursor advancement, alert creation, replay job,
  push controller, or admin page. These require the next durable-sync slice.
- No customer, staff, or Nuxt application changes.

## Files Allowed

- `camera-rental-server/yudao-module-rental/**`
- `docs/integrations/xianyu/**`
- `openspec/changes/xian-guanjia-data-integration-v1/**`

## Interfaces / Seams

- `XianyuCanonicalJson` produces the final compact request string.
- `XianyuRequestSigner` and `XianyuWebhookSignatureVerifier` bind MD5 signing
  to that exact UTF-8 string.
- `XianyuReadClient` uses a closed `XianyuReadEndpoint` enum and cannot target
  a write path.
- Later sync services consume `XianyuReadResponse` without parsing transport
  concerns or logging raw private data.

## Components To Create

- Canonical JSON serializer, signer, webhook verifier, read endpoint enum,
  response/error model, redactor, and OkHttp read client.

## Components To Reuse

- Existing `XianyuProperties`, Spring configuration binding, managed OkHttp
  `4.12.0` dependency, MockWebServer, and Jackson ObjectMapper.

## Components To Extract

- Keep JSON/signature/transport/redaction concerns outside later persistence,
  conversion, scheduling, and controller classes.

## API / Data Flow Contracts

- Every request is `POST {baseUrl}{path}?appid={appKey}&timestamp={seconds}&sign={sign}`.
- The signed string and the transmitted UTF-8 body must be byte-for-byte
  identical, including the empty `{}` body.
- The outbound endpoint enum contains only the official read paths reviewed on
  2026-07-23. Any mutation path is absent.
- Raw order-push verification accepts only a valid signature for the received
  raw body and does not create a success response without durable persistence.

## State / Error / Empty / Loading Behavior

- Loading: later sync services own run-state tracking; this client is
  synchronous and returns a structured response only after transport completes.
- Empty: valid empty API lists remain successful raw responses for later
  persistence.
- Error: disabled configuration, missing runtime credentials, transport,
  non-2xx HTTP, malformed JSON, and remote non-zero codes are distinct safe
  failure kinds.
- Disabled: no network request is attempted until the integration is enabled
  and runtime credentials are present.
- Permission: no operator endpoint is introduced in this slice.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `git diff --check -- camera-rental-server openspec/changes/xian-guanjia-data-integration-v1`
- `rg -n "product/create|product/edit|product/delete|order/consign|order/change-price|refund/agree|refund/refuse" camera-rental-server/yudao-module-rental`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- The account uses the self-developed/third-party ERP signature mode. A
  business-integration `seller_id` signature mode requires explicit account
  confirmation before it can be enabled.
- The official documents do not define a complete shared retry or error-code
  catalog, so this slice does not retry remote business errors automatically.
