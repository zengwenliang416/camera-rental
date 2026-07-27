# Task 004: Verification

## Goal

A maintainer can verify the complete shipment workflow with unit, static, mock
E2E, migration, and security evidence.

## Vertical Slice

Maintainer runs the required checks and sees evidence that pending-order search,
device binding, XianGuanJia ship, local commit ordering, admin UI, staff UI, and
tenant/write safety all behave as specified.

## In Scope

- Backend test execution and focused failure triage.
- Admin type check and staff H5/WeChat builds.
- Migration copy/checksum evidence.
- Mock XianGuanJia E2E for admin and staff flows.
- Write-disabled, shop-not-authorized, and cross-tenant red-team probes.

## Files Allowed

- `openspec/changes/xian-guanjia-order-dispatch-ship-v1`
- `camera-rental-server`
- `camera-rental-admin`
- `camera-rental-staff`

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `cd camera-rental-admin && pnpm ts:check`
- `cd camera-rental-staff && pnpm build:h5`
- `cd camera-rental-staff && pnpm build:mp-weixin`
- `git diff --check`

## Stop Conditions

- Evidence is recorded for every required verification surface or exact blockers
  are documented.
- SpecNav development handoff can proceed without unsupported success claims.
