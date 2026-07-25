# Data Flow Prototype

## Flow A: Bounded Order Synchronization

1. User action: a scheduler starts a due sync or an authorized admin requests a
   bounded read sync for one shop.
2. UI state transition: the admin shows a queued or running sync run. It never
   receives app credentials or raw third-party request headers.
3. Request payload: `{ shopId, upperBound, syncType }`; manual requests also
   carry the authenticated operator context.
4. Backend validation: integration is enabled, runtime credentials exist, the
   shop is authorized for the active tenant, `upperBound` is fixed, and the
   read window stays within the documented six-month limit.
5. Database or integration effect: create a sync run, read its durable cursor,
   sign and send an exact canonical JSON body to the order-list endpoint, retain
   a restricted raw payload and hash, upsert the normalized channel order by
   external order id, pull details when necessary, and advance the cursor only
   after the page is durable.
6. Response payload: `{ runId, status, receivedCount, deduplicatedCount,
   reviewRequiredCount, failedCount }`.
7. UI render result: show safe aggregate counts, redacted failures, and a link
   to manual review. No PII, secret, signature, or unrestricted raw payload is
   rendered.
8. Log, metric, or audit event: safe application/shop ids, run/page counts,
   external error category, cursor boundary, and correlation id.

## Flow B: Channel Order Conversion And Review

1. User action: a persisted channel order detail is ready for conversion, either
   from a sync page, a verified push, or a safe replay.
2. UI state transition: the order moves from `pending_conversion` to
   `converted` or `review_required`; ordinary users see the review reason but
   not the unrestricted payload.
3. Request payload: internal job input `{ channelOrderId, conversionVersion,
   idempotencyKey }`.
4. Backend validation: external order identity is present, conversion has not
   already created a rental order, product/SKU mapping is explicit, and
   `seller_remark` produces valid, non-conflicting business dates in
   `Asia/Shanghai`.
5. Database or integration effect: retain raw and normalized evidence,
   conversion version, parsed remark result, and `pay_amount` as integer cents.
   Missing mapping or invalid dates create one actionable review record and no
   rental schedule. A valid input creates at most one rental order and item.
6. Response payload: either `{ status: "converted", rentalOrderId }` or
   `{ status: "review_required", reviewId, reasonCode }`.
7. UI render result: converted orders show their source link; review items
   retain paid revenue and offer only permitted correction actions.
8. Log, metric, or audit event: channel order id, conversion version, result,
   safe reason code, and replay idempotency outcome.

## Flow C: Device Assignment And Occupied Schedule

1. User action: a permitted rental operator selects an internal order item and
   a concrete device.
2. UI state transition: confirmation is disabled while the server validates;
   no optimistic schedule lane is rendered.
3. Request payload: `{ orderItemId, deviceId, occupiedStart,
   occupiedEndExclusive, idempotencyKey, expectedVersion }`.
4. Backend validation: actor permission, order/item eligibility, device model
   mapping and lifecycle state, end-after-start range, and a fresh
   `[start, endExclusive)` conflict query.
5. Database or integration effect: in one transaction, acquire the concurrency
   guard, recheck `newStart < existingEndExclusive && newEndExclusive >
   existingStart`, create one assignment and one effective occupied schedule,
   and persist the idempotency result. Any conflict rolls back all writes.
6. Response payload: `{ assignmentId, scheduleId, deviceId,
   occupiedStart, occupiedEndExclusive }` or a typed schedule-conflict error.
7. UI render result: render only the accepted schedule; a conflict refreshes
   candidates and preserves the operator's safe draft.
8. Log, metric, or audit event: actor, device, order item, result, conflict
   category, and idempotency key hash.

## Boundary Invariants

- Channel external identifiers remain strings; documented money values remain
  integer cents.
- `pay_amount` remains rent revenue even when conversion is blocked; refunds
  are independently modeled.
- One channel order creates zero or one rental order.
- One idempotency key returns the accepted conversion or assignment rather than
  duplicating records.
- Adjacent half-open schedules are valid; overlapping effective schedules for
  the same device are rejected.
- Failed raw event or detail replay cannot advance a cursor past unprocessed
  data or create a duplicate order, assignment, or schedule.

## Review Focus

- Loading behavior: sync and assignment actions disable duplicate primary
  actions while server state remains authoritative.
- Empty behavior: no channel records, mappings, or available devices state the
  reason and expose only permitted next actions.
- Error behavior: validation, authorization, external transport, replay, and
  schedule-conflict errors are typed and redacted.
- Permission behavior: sync start, raw-payload access, conversion correction,
  and device assignment require separate backend permissions.
- Retry behavior: read transport retries are bounded; replay/assignment retries
  require the original idempotency key.
