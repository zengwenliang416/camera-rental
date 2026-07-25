# Task Brief: 007-admin-operations

## Goal

Authorized admins can operate channel and rental workflows through admin APIs and bilingual light/dark UI pages without third-party write capabilities.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`
- `openspec/changes/xian-guanjia-data-integration-v1/prototype/data-flow-map.md`

## Vertical Slice

An admin can open operational pages, view redacted integration status, sync authorized shops, run a bounded order sync page, convert eligible channel orders, manage devices, assign schedules, and inspect review/revenue surfaces. All third-party calls stay backend-only and read-only.

## In Scope

- Env-configurable XianGuanJia credentials with default disabled status.
- Admin APIs for config status, shops, orders, conversion, devices, assignment, reviews, and revenue summary.
- Admin Vue pages plus zh-CN/en locale keys and routes reusing existing theme/locale stores.
- Focused tests for AppKey masking and authorized-shop parsing.

## Out Of Scope

- Customer checkout, payment, deposit, real-name flows.
- Any XianGuanJia write operation.
- Full after-sale/product durable page orchestrators beyond the existing read allowlist.

## Files Allowed

- `camera-rental-server/yudao-module-rental/**`
- `camera-rental-server/yudao-server/src/main/resources/**`
- `camera-rental-server/sql/mysql/migrations/**`
- `camera-rental-admin/src/**`
- `openspec/changes/xian-guanjia-data-integration-v1/**`

## Interfaces / Seams

- Controllers under `controller.admin` map to `/admin-api/rental/**`.
- Domain services from 004–006 remain authoritative for sync/conversion/assignment.

## Components To Create

- Admin services/controllers/VOs, shop DO/mapper, admin UI modules.

## Components To Reuse

- Existing read client, order sync, conversion, assignment services, Element Plus admin shell, vue-i18n, dark mode store.

## Components To Extract

- No new extraction required.

## API / Data Flow Contracts

- Config response never includes AppSecret.
- Order list omits phone/address private fields.
- Sync uses fixed windows and existing cursor rules.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- Write-path and credential scans
- Admin typecheck when dependencies are available

## Stop Conditions

- Stop if a third-party write is requested or if customer checkout is required.
