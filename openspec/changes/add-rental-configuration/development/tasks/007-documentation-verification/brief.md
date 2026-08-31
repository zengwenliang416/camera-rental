# Task Brief: 007-documentation-verification

## Goal

Produce traceable documentation, automated checks and deployment/rollback
evidence without performing production deployment or historical mutation.

## Vertical Slice

A release operator can follow the documented migration order, run the full
verification set, inspect immutable evidence and prepare a reversible rollout
decision while production remains unchanged.

## In Scope

- Checklist items `7.1` through `7.5`.
- Domain/integration docs, backend/admin checks, SpecNav verification and ops evidence.

## Out Of Scope

- Production migration, deployment, third-party write or historical backfill execution.

## Files Allowed

- `docs/domain`
- `docs/integrations/xianyu`
- `openspec/changes/add-rental-configuration`
- All test and build commands may read the allowed server/admin implementation roots.

## Interfaces / Seams

- Requirements assertions `A1` through `A10`, task evidence and operations handoff.

## Components To Create

- Updated field/source/order-sync documentation and release/rollback evidence.

## Components To Reuse

- Existing project test commands, SpecNav six-domain verification and operations templates.

## Components To Extract

- Shared verification commands and evidence references remain in the change artifacts, not duplicated in product docs.

## API / Data Flow Contracts

- Documentation must match the final backend VO, database and admin types.
- Operations steps require explicit production authorization and shop/database identity checks.

## State / Error / Empty / Loading Behavior

- Failed or unavailable checks remain visible as blockers or unverified items.
- No report may convert a missing command or environment into a pass.

## TDD Requirement

- Verification is evidence-driven; every completed task must link executed checks.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental -am test -Dmaven.repo.local=/Volumes/zwl/maven-repository`
- `cd camera-rental-admin && pnpm ts:check && pnpm lint && pnpm build:prod`
- Run SpecNav facticity, static, unit, redteam, E2E and sensory contracts.

## Stop Conditions

- Stop before production writes, deployment, push or external API mutation.
- Stop if any acceptance assertion lacks reproducible evidence.

## Unsafe Assumptions

- Local green tests do not prove production migration or historical reconciliation safety.
