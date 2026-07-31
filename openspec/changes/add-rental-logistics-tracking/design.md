# Design: Complete Rental Logistics Tracking

## Context

`rental_device_shipment` records one channel shipment operation for one device.
The complete feature needs a separate physical-package aggregate because one
order can have multiple packages and one package can contain multiple devices.
Tracking is eventually consistent and must not make a successful Xianyu
shipment fail when Kuaidi100 is unavailable.

## Architecture

```text
Xianyu shipment transaction
  -> shipment audit
  -> physical Delivery + device relation
  -> SUBSCRIBE and INITIAL_QUERY Outbox
  -> commit

Outbox worker
  -> short lease transaction
  -> Provider network call outside transaction
  -> result transaction
  -> complete tracking snapshot + Delivery summary + risk

Kuaidi100 callback
  -> callback token lookup and signature verification
  -> encrypted Inbox persistence and quick ACK
  -> Inbox worker
  -> complete tracking snapshot + Delivery summary + risk

Schedule center
  -> local batch summary every 60 seconds while visible
  -> local detail on drawer open
  -> refresh command enqueues REFRESH_QUERY
```

## Domain Decisions

### Shipment and Delivery

- Shipment remains channel-write and device-dispatch audit.
- Delivery represents one real package.
- The idempotency key is tenant, rental order, direction, source carrier code,
  and normalized waybill.
- `OUTBOUND`, `RETURN`, `EXCHANGE_OUT`, and `EXCHANGE_RETURN` never overwrite
  each other.
- Service validation ensures order item, assignment, device, and Delivery share
  tenant and order ownership before any relationship is committed.

### Complete Tracking Snapshots

- Query and callback payloads are converted into Provider-neutral events.
- Events are normalized, fingerprinted, sorted, and hashed as one complete
  snapshot.
- Identical snapshot hashes only advance synchronization metadata.
- Changed snapshots create a new immutable version and atomically update the
  Delivery summary.
- Terminal `DELIVERED` and `RETURNED` summaries cannot regress because of late
  or out-of-order events.

### Provider Boundary

- `LogisticsProvider` contains only platform commands, results, and events.
- Kuaidi100 request signing, callback verification, raw payload parsing, and
  supplier status mapping stay under `integration.logistics.kuaidi100`.
- Provider common configuration, credential rows, and carrier mappings are
  tenant-scoped and disabled by default.
- A tenant may configure multiple named `customerCode + apiKey` credential
  pairs. A Delivery binds one usable credential on its first Provider task and
  keeps that assignment until the credential is disabled, deleted, incomplete,
  or no longer belongs to the same Provider and tenant.
- Direct Provider query defaults to a minimum 1,800-second interval.

### Inbox, Outbox, and Workers

- Outbox stores only Delivery identity and safe scheduling metadata.
- Workers claim short leases in a transaction, call the network without a
  transaction, then persist success or bounded retry state in a new transaction.
- Callback Inbox stores a payload hash plus encrypted callback content.
- Duplicate callback payloads, event tasks, worker restarts, and expired leases
  are idempotent.

### Security

- Tracking phone, callback token/salt/content, and Provider credentials use
  `EncryptTypeHandler` and are excluded from `toString`.
- URLs do not expose tenant, order, or waybill identity.
- Responses and logs use masked waybills and safe Provider errors.
- Every admin action has an explicit permission and tenant boundary.

## Database

One additive migration creates:

1. `rental_delivery`
2. `rental_delivery_device_rel`
3. `rental_delivery_trace`
4. `rental_delivery_callback_inbox`
5. `rental_delivery_outbox`
6. `rental_logistics_carrier_mapping`
7. `rental_logistics_provider_config`
8. `rental_logistics_provider_credential`

It also adds nullable `provider_credential_id` to `rental_delivery` and nullable
`delivery_id` to `rental_device_shipment`. Pre-release single-credential rows
are copied into a named `default` credential without decrypting the stored
ciphertext. Historical backfill is a separate service operation and the
migration performs no network or business-data mutation.

## APIs

- `POST /admin-api/rental/delivery/tracking-summary/batch`
- `GET /admin-api/rental/delivery/{deliveryId}/tracking`
- `POST /admin-api/rental/delivery/{deliveryId}/refresh`
- Delivery, carrier mapping, Provider config, failed task, retry, reconcile,
  backfill, cleanup, and metrics admin endpoints under `/admin-api/rental`.
- Provider credential create/update, delete, and local verification endpoints
  reuse the existing Provider config permissions.
- `POST /rental/webhooks/kuaidi100/tracking/{callbackToken}` for public callback
  ingestion.

## Frontend

- The schedule page stores summaries once in `trackingByOrderId`.
- Complete traces load only when the drawer opens.
- Local summary polling runs every 60 seconds only while visible and refreshes
  immediately after visibility returns.
- Refresh submits a command and renders queued, throttled, disabled,
  mapping-required, and permission states.
- Existing shell, theme, locale, status, drawer, empty, permission, and
  responsive components are reused.

## Failure Handling

- Missing mapping or disabled Provider leaves a usable local Delivery with a
  stable reason code.
- Provider failures never roll back successful Xianyu shipment.
- Retry uses bounded exponential backoff and safe error categories.
- Delivered tracking never changes device availability or return inspection.

## Rollout

- Feature flags and Provider switches remain off until tenant configuration and
  carrier mappings are verified.
- Historical backfill supports dry-run and bounded batch size.
- Production Provider verification and deployment remain outside this Change.
