# MySQL Concurrency And Schedule Query Verification

Recorded on 2026-07-25 against local MySQL 8.4 verification container
`camera-rental-mysql-runtime-v17`, database `camera_rental_verify`.

This verification used isolated tenant `990025` and deterministic `cg-v1-20260725`
fixture prefixes. No XianGuanJia credentials, tokens, order identifiers, seller
remarks, phone numbers, addresses, or third-party writes were involved.

## Concurrent Assignment Probe

Two Python/PyMySQL worker threads attempted to allocate overlapping occupied
periods for the same physical device:

```text
period: 2026-07-22 <= occupied < 2026-07-31
device_id: 9102302
```

Worker A acquired the device row lock, inserted one effective schedule and one
assignment, held the transaction briefly, then committed. Worker B started while
A held the lock; B waited for the same device row lock, then re-ran the overlap
query and rejected the assignment as a schedule conflict.

```text
A outcome=assigned lock_wait_ms=1.2 schedule_id=9102402 assignment_id=9102502
B outcome=schedule-conflict lock_wait_ms=1356.9 conflicts=1
effective_overlap_schedules_for_device=1
assigned_rows_for_device=1
```

Assertions:

```text
exactly_one_assigned=true
exactly_one_effective_overlap=true
second_attempt_rejected_as_conflict=true
second_attempt_waited_for_device_lock_ms_ge_1000=true
```

## Schedule Query Plan

The same verification tenant was expanded to `30001` `rental_schedule` rows.
`EXPLAIN ANALYZE` was then run for the overlap check used by assignment and the
default schedule admin page.

Overlap query:

```sql
SELECT id
FROM rental_schedule
WHERE tenant_id = 990025
  AND device_id = 9102302
  AND status = 'EFFECTIVE'
  AND occupy_start_date < '2026-07-31'
  AND occupy_end_date_exclusive > '2026-07-22'
  AND deleted = b'0'
FOR UPDATE;
```

Plan excerpt:

```text
Index range scan on rental_schedule using idx_rental_schedule_device_range
over (tenant_id = 990025 AND device_id = 9102302 AND status = 'EFFECTIVE'
AND occupy_start_date < '2026-07-31')
actual time=0.582..0.587 rows=1 loops=1
```

Default schedule page query:

```sql
SELECT id, device_id, occupy_start_date
FROM rental_schedule
WHERE tenant_id = 990025
  AND deleted = b'0'
ORDER BY occupy_start_date, id
LIMIT 20;
```

Plan excerpt:

```text
Index lookup on rental_schedule using idx_rental_schedule_admin_default
(tenant_id=990025)
actual time=0.163..0.165 rows=20 loops=1
```

Assertions:

```text
overlap_plan_mentions_schedule_device_range_index=true
page_plan_mentions_admin_default_index=true
tenant_schedule_rows=30001
```

## Residual Risk

This proves the InnoDB lock/recheck pattern and representative query plans on a
local MySQL 8.4 verification database. It does not replace approved-environment
rollout, browser transition-flow tests, permission-denied click-through tests,
or production data distribution checks.
