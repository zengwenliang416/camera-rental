# Implementation Readiness: camera-rental-full-platform-v1

## Gate

Production implementation remains blocked until the SpecNav requirements
contract passes. This document records live repository evidence and the exact
scaffolding conventions to use after the remaining product decisions are
closed.

Five product-policy decisions remain: real-name verification, deposit handling,
fulfillment buffers, pricing/exception rules, and report/export definitions.

## Verified Baseline

- Backend root: `camera-rental-server`.
- Java: 17.
- Spring Boot: 3.5.15.
- Build: Maven reactor.
- Persistence: MyBatis Plus with MySQL production semantics and H2 MySQL mode
  for module tests.
- Enabled modules today: system and infra.
- Present but disabled modules required by full V1: member, pay, mall product,
  and ERP.
- Existing root SQL is a full baseline only. No incremental migration directory
  exists.
- `BaseDO` supplies creator, updater, create/update time, and logical deletion.
- `TenantBaseDO` adds `tenant_id`.

## Rental Module Scaffold

Create the module only after the requirements gate passes:

```text
camera-rental-server/yudao-module-rental/
├── pom.xml
├── yudao-module-rental-api/
│   ├── pom.xml
│   └── src/main/java/cn/iocoder/yudao/module/rental/api/
└── yudao-module-rental-biz/
    ├── pom.xml
    ├── src/main/java/cn/iocoder/yudao/module/rental/
    ├── src/main/resources/
    └── src/test/
```

The API artifact follows the existing trade API pattern:

- packaging `jar`;
- depends on `yudao-common`;
- optional validation dependency;
- contains stable cross-module interfaces and DTOs only.

The Biz artifact:

- depends on `yudao-module-rental-api`;
- depends on system, infra, member, pay, product, and ERP at their public
  boundaries;
- depends on tenant, web, security, MyBatis, Redis, job, test, and Excel
  starters as required;
- uses OkHttp and MockWebServer for XianGuanJia transport and contract tests;
- does not depend on another module's Mapper or data object.

## Maven Integration Points

After scaffolding:

1. Add `yudao-module-rental` to the root reactor.
2. Add rental API and Biz artifacts to dependency management.
3. Add `yudao-module-rental-biz` to `yudao-server`.
4. Enable member, pay, mall product, and ERP in the root reactor.
5. Enable the corresponding runtime artifacts in `yudao-server`.
6. Do not enable mall trade as the owner of rental orders.
7. Do not enable WMS unless a later requirement proves ERP warehouse behavior
   insufficient.

## Cross-Module Boundaries

- Member: validate member account/status and read member/address DTOs. The
  baseline `name` field is editable profile data, not real-name evidence.
- Real-name verification, if approved, requires a dedicated restricted
  member-owned aggregate and status API. Rental stores only its reference and
  order-time status snapshot; raw identity numbers and document images stay out
  of rental tables, APIs, logs, and exports.
- Pay: create/read payment orders and create/read refunds through public APIs.
- Mall product: read SPU/SKU/category/brand identity through public APIs.
- ERP: own supplier, procurement, receipt, warehouse, stock movement, transfer,
  and stocktake documents.
- Rental: own rental configuration, packages, physical devices, rental orders,
  billable dates, occupied schedules, assignments, fulfillment state,
  inspection, maintenance, deposits, compensation, and channel mappings.
- If ERP lacks a public API needed by rental, add a narrow API/adapter boundary;
  never call ERP persistence internals.

## Resolved Scheduling Baseline

- Displayed business dates include both start and end.
- Persist and compare `Asia/Shanghai` intervals as `[start, endExclusive)`.
- Use `newStart < existingEndExclusive && newEndExclusive > existingStart` for
  overlap; adjacent boundaries are allowed.
- Customer order creation makes an expiring SKU quantity reservation.
- Payment confirms the quantity reservation; cancellation or timeout releases
  it.
- Concrete physical devices are assigned before picking/outbound with a
  transactional state and overlap recheck.

## Database Migration Convention

Introduce a new additive migration directory:

```text
camera-rental-server/sql/mysql/migrations/
```

The first file should be:

```text
20260723_001_rental_foundation.sql
```

Rules:

- never edit `sql/mysql/ruoyi-vue-pro.sql` to represent an incremental change;
- use `bigint` identifiers and integer-cent amount columns;
- use typed dates/timestamps, never localized date strings;
- include creator, updater, create/update time, deleted, and tenant fields where
  the project convention applies;
- add unique constraints for device numbers, order numbers, channel identity,
  payment/idempotency references, and cursor streams;
- add indexes for status/date, SKU/device/warehouse, effective schedules,
  review queues, and channel update cursors;
- retain historical financial, schedule, assignment, operation, maintenance,
  and channel evidence.

## Persistence Conventions

- Tenant-owned aggregate records extend `TenantBaseDO`.
- Global/static configuration uses `BaseDO` only when tenant isolation is
  explicitly inapplicable.
- DO classes use `@TableName`, `@TableId`, `@KeySequence`, and Lombok in the
  existing style.
- Mappers extend `BaseMapperX` and keep query construction in typed wrapper
  methods.
- Controllers use request/response VOs and converters; controllers never access
  Mappers.
- Services own transaction boundaries and concurrency rechecks.
- Reserve the currently unused rental error-code range
  `[1_024_000_000, 1_025_000_000)`, and record it in
  `ServiceErrorCodeRange` when implementation begins.

## Test Conventions

Create in `yudao-module-rental-biz/src/test/resources`:

```text
application-unit-test.yaml
sql/create_tables.sql
sql/clean.sql
logback.xml
```

Required test layers:

- `BaseDbUnitTest` service and Mapper tests using H2 in MySQL mode;
- pure unit tests for date ranges, pricing, state machines, redaction, signing,
  remark parsing, and amount conservation;
- MockWebServer tests proving signed bytes equal transmitted JSON bytes;
- concurrency tests for schedule overlap and assignment;
- API tests for permission, validation, idempotency, and safe errors;
- secret/PII and forbidden-dependency scans.

Do not copy disabled upstream tests as proof. New rental tests must run and
assert the actual V1 invariants.

## First Implementable Slice After Gate

1. Scaffold rental API/Biz modules and Maven wiring.
2. Enable required existing modules.
3. Add the first additive migration and matching H2 test schema.
4. Add rental configuration properties with XianGuanJia disabled by default.
5. Implement canonical JSON/signing/redaction primitives and mock tests.
6. Implement the core product-config, device, order, and schedule persistence
   foundation before any frontend mutation.
