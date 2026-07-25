# Task Brief: 008-verification

## Goal

Maintainer can validate V1 behavior, security boundaries, and migration readiness with system-executed evidence.

## Parent Artifacts

- `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- `openspec/changes/xian-guanjia-data-integration-v1/development/tasks/007-admin-operations/report.md`

## Vertical Slice

A maintainer can re-run module tests, credential/write scans, config boundary checks, and SpecNav handoff/verify packaging without inventing completed work.

## In Scope

- Package and re-run gating commands for rental tests and security scans.
- Repair SpecNav task evidence shape so handoff can pass.
- Capture migration list and admin typecheck when dependencies allow.

## Out Of Scope

- New product features beyond residual verification fixes.
- Live production deployment.

## Files Allowed

- `openspec/changes/xian-guanjia-data-integration-v1/**`
- Scratch evidence logs and existing rental/admin sources only as needed for fixes.

## Interfaces / Seams

- SpecNav development-contract handoff and verification plugin.

## Components To Create

- Task evidence packets and validation log entries.

## Components To Reuse

- Existing Surefire tests and source scanners.

## Components To Extract

- No extraction required.

## API / Data Flow Contracts

- No API contract changes in this slice.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am test`
- Security scans for write paths and secrets
- `development-contract.js --mode handoff`

## Stop Conditions

- Stop if verification requires expanding Non-goals or rewriting completed domain engines.
