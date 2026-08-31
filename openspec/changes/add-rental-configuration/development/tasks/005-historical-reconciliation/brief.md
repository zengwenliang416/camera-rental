# Task Brief: 005-historical-reconciliation

## Goal

Provide a bounded, resumable and observable way to reconcile eligible historical
orders without deleting history or reversing fulfilled records.

## Vertical Slice

An operator can run a dry-run over a stable order-ID range, inspect created,
updated, skipped, conflict, failed and review counts, then resume approved local
or later production execution from a durable checkpoint.

## In Scope

- Checklist items `5.1`, `5.2`, and `5.3`.
- Stable paging, per-batch transactions, dry-run, checkpointing and failure records.

## Out Of Scope

- Production execution without separate authorization.
- Reversal of assigned, shipped, returned, inspected or settled history.

## Files Allowed

- `camera-rental-server/yudao-module-rental`
- `docs/domain`
- `docs/integrations/xianyu`

## Interfaces / Seams

- Central reconciliation service, rule service, order paging and operations status.

## Components To Create

- `RentalHistoricalOrderBackfillService`
- Backfill request/result/checkpoint models and operational runbook.

## Components To Reuse

- Stable primary-key paging, transaction templates and existing job conventions.

## Components To Extract

- Batch counters and reconciliation outcomes use the same domain result model as live replay.

## API / Data Flow Contracts

- Dry-run performs no business mutation.
- Real execution is idempotent by external/internal order identity and checkpoint.

## State / Error / Empty / Loading Behavior

- Empty ranges complete with zero counters.
- Batch failure records the boundary and stops or resumes according to explicit policy.
- Fulfilled matches are reported as conflicts or unchanged, never reversed.

## TDD Requirement

- Test replay, pause/resume, partial failure, repeated batches and fulfilled-order protection.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental -am test -Dmaven.repo.local=/Volumes/zwl/maven-repository`

## Stop Conditions

- Stop before any production database mutation, deployment or external write.
- Stop if paging can skip or duplicate equal-boundary records.

## Unsafe Assumptions

- Historical eligibility must be derived from durable current records, not old screenshots or counts.
