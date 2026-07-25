# Task Report: 003-channel-persistence

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/.../dal/{dataobject,mysql}/xianyu/`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/.../integration/xianyu/service/`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/.../integration/xianyu/service/`
- `camera-rental-server/sql/mysql/migrations/20260723_002_xianyu_order_pay_amount_bigint.sql`

## What Changed

- Parsed successful documented order-detail payloads into normalized order facts while retaining all raw JSON only in the restricted payload table.
- Added SHA-256 raw-payload deduplication, order upsert by shop and external order number, and seller-remark parse-state preservation when the remark is unchanged.
- Advanced order cursors only for newer `(source_updated_at, external_order_id)` points.
- Replaced unsafe read-then-write identity lookups with transaction-scoped `FOR UPDATE` queries for raw payloads, orders, and cursors.

## TDD Evidence

- Tests cover Shanghai timestamp parsing, large `int64` cent amounts, malformed/success checks, raw-payload deduplication, raw-before-normalized write order, preserved conversion state, and timestamp tie breaking.

## Verification Commands

- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=XianyuOrderPayloadParserTest,XianyuOrderPersistenceServiceImplTest,XianyuSyncCursorAdvancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `cmp -s camera-rental-server/sql/mysql/migrations/20260723_002_xianyu_order_pay_amount_bigint.sql openspec/changes/xian-guanjia-data-integration-v1/development/migrations/20260723_002_xianyu_order_pay_amount_bigint.sql`
- `git diff --check -- camera-rental-server/yudao-module-rental camera-rental-server/sql/mysql/migrations openspec/changes/xian-guanjia-data-integration-v1`
- All three commands have passing `system-executed` receipts in `development/validation-log.jsonl`.

## Concerns

- Unit tests verify the locking seam and ordering logic. A production-like MySQL concurrency integration test remains desirable before high-volume sync is enabled.

## Scope Deviations

No scope deviations were recorded for this slice.
## Follow-up Needed

- A later page runner must use fixed official `update_time` windows, fetch each listed order detail, and advance the cursor only after its full page commits.

## Adjudication

No open task-level blocker remains.
