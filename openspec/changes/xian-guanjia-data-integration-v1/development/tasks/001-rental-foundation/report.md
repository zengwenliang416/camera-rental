# Task Report: 001-rental-foundation

## Status

DONE

## Files Changed

- `camera-rental-server/pom.xml`
- `camera-rental-server/yudao-server/pom.xml`
- `camera-rental-server/yudao-server/src/main/resources/application.yaml`
- `camera-rental-server/yudao-module-rental/**`
- `camera-rental-server/sql/mysql/migrations/20260723_001_xianyu_rental_foundation.sql`
- `openspec/changes/xian-guanjia-data-integration-v1/development/**`

## What Changed

- Added the Rental API and Biz Maven modules to the reactor and server
  composition.
- Added backend-only XianGuanJia configuration that is disabled by default,
  reads credentials from runtime environment variables, and reports a safe
  missing-credentials state without exposing values.
- Added the first additive MySQL foundation migration for channel, raw-payload,
  review, rental-order, device, half-open schedule, assignment, cursor, sync,
  and alert records.
- Added a SHA-locked audit copy of the production migration for the SpecNav
  development handoff.

## TDD Evidence

- `XianyuPropertiesTest` verifies the default disabled state and that an
  enabled integration remains unavailable until both runtime credentials are
  supplied.

## Verification Commands

- `cd camera-rental-server && mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- `git diff --check -- camera-rental-server`
- Static migration review for additive tables, required indexes, and absence of
  supplied credentials.

## Concerns

- The migration has not been applied to a live MySQL instance in this local
  development task. Production rollout must execute the canonical SQL once
  against a backed-up target schema.

## Scope Deviations

No scope deviations were recorded for this slice.
## Follow-up Needed

- The next slice must add only documented read-only XianGuanJia transport and
  mock transport tests before any real integration is enabled.
