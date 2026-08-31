# Task Brief: 001-identifier-foundation

## Goal

Persist XianGuanJia product/SKU IDs and Xianyu item/SKU IDs as four explicit
string fields whose provenance is inspectable and never inferred by fallback.

## Vertical Slice

After product and order detail synchronization, an administrator or test can
inspect the stored channel identifiers and see the exact source field for every
product and SKU before any equipment-model rule is evaluated.

## In Scope

- Checklist items `1.1`, `1.2`, `1.3`, and `1.4`.
- Incremental schema, indexes, menu/permission seed, persistence models and tests.
- Safe legacy backfill only where the source relationship is unambiguous.

## Out Of Scope

- Product rule CRUD, reconciliation, historical order execution and admin UI.
- Guessing identifiers from titles, seller remarks or similarly named legacy fields.

## Files Allowed

- `camera-rental-server/sql/mysql`
- `camera-rental-server/yudao-module-rental`
- `openspec/changes/add-rental-configuration/development/migrations`

## Interfaces / Seams

- Existing XianGuanJia product, SKU and order-detail persistence services.
- New explicit identifier fields consumed later by product-rule reconciliation.

## Components To Create

- Incremental forward SQL and identifier-focused persistence fixtures.
- A channel identifier normalizer that preserves identifiers as strings.

## Components To Reuse

- Existing rental module DO, Mapper, service and test conventions.
- Existing raw payload and normalized order persistence boundaries.

## Components To Extract

- Centralize identifier normalization when a second persistence path needs it.

## API / Data Flow Contracts

- No public API is added in this slice.
- XianGuanJia and Xianyu identifiers remain distinct from ingestion to database.

## State / Error / Empty / Loading Behavior

- Missing identifier fields remain null and block exact mapping without fallback.
- Ambiguous legacy data remains unchanged and is reported by migration fixtures.

## TDD Requirement

- Write migration and persistence tests before or alongside implementation.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental -am test -Dmaven.repo.local=/Volumes/zwl/maven-repository`
- Run the migration fixture against an isolated test schema and record its output.

## Stop Conditions

- Stop if current online XianGuanJia docs do not prove a field's ownership.
- Stop if a backfill would require title/text inference or a cross-shop guess.
- Stop if a historical migration file would need modification.

## Unsafe Assumptions

- Similar numeric values do not prove that two external identifier types are interchangeable.
