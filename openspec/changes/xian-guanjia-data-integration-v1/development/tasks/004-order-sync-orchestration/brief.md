# Task Brief: 004-order-sync-orchestration

## Goal

An integration operator can execute one bounded, read-only order-list page and
advance its durable cursor only after every listed order detail is persisted.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`

## Vertical Slice

The backend accepts a fixed local sync window and page request, creates a
durable run record, queries the documented order list, pulls every listed order
detail, persists it through the existing local boundary, and advances the
stable cursor only after the whole page completes.

## In Scope

- Fixed `update_time` window validation, including the official six-month
  maximum and page number/size limits.
- Documented order-list response parsing with required `order_no`,
  `update_time`, `count`, page number, and page size.
- Read-only list/detail orchestration, durable sync-run counts, and redacted
  failure status.
- Refuse a list window with more than 10,000 rows without pulling details or
  advancing the cursor.
- Advance the cursor to the newest successful detail point only after the full
  page has persisted.

## Out Of Scope

- Recursive window splitting, scheduled jobs, push ingestion, retry/replay
  endpoints, shop authorization persistence, controllers, admin UI, order
  conversion, and all third-party write operations.

## Files Allowed

- `camera-rental-server/yudao-module-rental/**`
- `docs/integrations/xianyu/**`
- `openspec/changes/xian-guanjia-data-integration-v1/**`

## Interfaces / Seams

- `XianyuOrderSyncService` coordinates one local page without exposing HTTP
  transport or persistence details.
- `XianyuOrderListPageParser` validates documented list data.
- `XianyuOrderPersistenceService` remains the only owner of raw/order/cursor
  writes.

## Components To Create

- Sync-run DO/Mapper, fixed-window value, list-page parser, and page sync
  service/result.

## Components To Reuse

- Existing `XianyuReadClient`, `XianyuReadEndpoint`, `XianyuReadResponse`,
  `XianyuOrderPersistenceService`, Jackson, tenant DOs, MyBatis Plus, and
  `xianyuClock`.

## Components To Extract

- Keep list parsing and fixed-window validation independent from HTTP and
  persistence so future recursive splitting and retries reuse them.

## API / Data Flow Contracts

- List request body contains `authorize_id`, fixed inclusive `update_time`
  epoch seconds, `page_no`, and `page_size`.
- A run starts before the read request; it is `SUCCEEDED` only after all
  details persist and the cursor decision completes.
- Any list/detail failure marks the run failed with a redacted error kind and
  leaves the cursor unchanged for the page.

## State / Error / Empty / Loading Behavior

- Loading: a durable `RUNNING` row exists before any outbound read.
- Empty: an empty list succeeds with zero counts and no cursor movement.
- Error: malformed list data, client failure, or a too-large window fails the
  run without a cursor advance.
- Disabled: existing client configuration gates prevent all outbound calls.
- Permission: no operator endpoint is introduced in this slice.

## TDD Requirement

- Write or update focused behavior tests before or alongside implementation.

## Verification Commands

- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `git diff --check -- camera-rental-server/yudao-module-rental openspec/changes/xian-guanjia-data-integration-v1`

## Stop Conditions

- Scope lock mismatch.
- Missing product, architecture, data-flow, or component decision.
- Component duplication that should be extracted.

## Unsafe Assumptions

- The official list count is authoritative for the 10,000-row guard. A later
  planner is responsible for splitting failed large windows before retry.
