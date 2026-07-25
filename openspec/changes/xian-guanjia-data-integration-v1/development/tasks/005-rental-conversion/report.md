# Task Report: 005-rental-conversion

## Status

DONE

## Files Changed

- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/.../dal/{dataobject,mysql}/{rental,xianyu}/`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/main/java/.../service/{SellerRemarkRentalPeriod*,RentalConversionResult,XianyuRentalConversionService*}.java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz/src/test/java/.../service/{SellerRemarkRentalPeriodParserTest,XianyuRentalConversionServiceImplTest}.java`
- `camera-rental-server/sql/mysql/migrations/20260723_003_rental_order_amount_bigint.sql`
- `docs/integrations/xianyu/field-mapping.md`
- `openspec/changes/xian-guanjia-data-integration-v1/development/{migrations,task-graph.json}`

## What Changed

- Added explicit channel product/SKU mapping, rental order/item, and manual-review persistence boundaries.
- Added versioned seller-remark parsing. Explicit `#租期M.D-M.D#` takes precedence; receipt/return dates use the documented receipt-next-day and return-day fallback.
- Added a transaction-scoped conversion service that returns an existing accepted rental order on replay, creates at most one rental order/item for a mapped valid source, and otherwise reuses one manual-review record without fabricating dates or mappings.
- Added migration 003 to widen converted rental amount columns to `BIGINT`, preserving documented `int64` cent amounts.

## TDD Evidence

- Parser tests cover explicit rent periods, receipt/return fallback, and unknown format review routing.
- Conversion tests cover a mapped high-value cent order, existing conversion replay, missing mapping review, review replay, and an unknown source rejection.

## Verification Commands

- `mvn -f camera-rental-server/pom.xml -pl yudao-module-rental/yudao-module-rental-biz -am -Dtest=SellerRemarkRentalPeriodParserTest,XianyuRentalConversionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `node -e 'JSON.parse(require("node:fs").readFileSync("openspec/changes/xian-guanjia-data-integration-v1/development/migrations/manifest.json", "utf8"))'`
- `cmp -s camera-rental-server/sql/mysql/migrations/20260723_003_rental_order_amount_bigint.sql openspec/changes/xian-guanjia-data-integration-v1/development/migrations/20260723_003_rental_order_amount_bigint.sql`
- `git diff --check`, write-path scan, and credential scan all passed.

## Concerns

- The current normalized source model has one product/SKU pair and therefore creates one rental item. Multi-item channel orders need a later line-item normalization slice.

## Scope Deviations

No scope deviations were recorded for this slice.
## Follow-up Needed

- `006-device-allocation` must add device lifecycle validation, half-open occupied schedules, conflict protection, and transactional assignment. A later operations slice must add an authorized controller/job and mapping correction workflow.

## Adjudication

No open task-level blocker remains.
