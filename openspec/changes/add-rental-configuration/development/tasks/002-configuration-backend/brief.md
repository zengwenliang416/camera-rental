# Task Brief: 002-configuration-backend

## Goal

Allow authorized administrators to maintain the device catalog and exact
shop-scoped product/SKU mapping rules with impact preview and auditability.

## Vertical Slice

An administrator can create or edit one catalog entry or channel product rule,
preview affected orders, save with optimistic versioning, and receive a bounded
reconciliation result without changing third-party data.

## In Scope

- Checklist items `2.1`, `2.2`, `2.3`, and `2.4`.
- Catalog operations, rule persistence, exact SKU children, permissions and audit.
- The 29 approved skipped items after unique internal shop resolution.

## Out Of Scope

- Admin page rendering, production backfill execution and third-party writes.
- Multi-SKU product fallback to a product default model.

## Files Allowed

- `camera-rental-server/yudao-module-rental`
- `camera-rental-server/sql/mysql`
- `openspec/changes/add-rental-configuration/development/migrations`

## Interfaces / Seams

- Rental device catalog service, configuration controller and reconciliation service.
- Shop/application ownership validation and synchronized product/SKU records.

## Components To Create

- `RentalConfigurationController`
- `RentalChannelProductRuleService`
- Product rule/SKU mapping DOs, Mappers, VOs and validation.

## Components To Reuse

- Existing rental catalog, permission, tenant, audit and pagination patterns.

## Components To Extract

- Shared exact-match validation for create, update, impact and reconcile operations.

## API / Data Flow Contracts

- Admin endpoints remain under `/admin-api/rental/configuration/**`.
- Rule keys use internal shop ID plus Xianyu item ID; multi-model children add
  the synchronized XianGuanJia SKU ID.

## State / Error / Empty / Loading Behavior

- Disabled rules remain readable and previewable but do not match new reconciliation.
- Missing or cross-shop SKU ownership is a validation error.
- Version conflicts return a conflict response without overwriting newer data.

## TDD Requirement

- Add service/controller tests for single model, multi-SKU, permissions and shop isolation.

## Verification Commands

- `cd camera-rental-server && /Users/wenliang_zeng/workspace/tool/apache-maven-3.9.10/bin/mvn -pl yudao-module-rental -am test -Dmaven.repo.local=/Volumes/zwl/maven-repository`

## Stop Conditions

- Stop if any initial skip item maps to zero or multiple internal shops.
- Stop if an API would expose credentials or raw customer payloads.
- Stop if impact preview and mutation cannot share the same rule evaluation.

## Unsafe Assumptions

- Store display names are not stable rule keys; internal shop IDs must be resolved first.
