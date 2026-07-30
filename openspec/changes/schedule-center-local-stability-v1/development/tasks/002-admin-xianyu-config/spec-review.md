# Spec Review: 002-admin-xianyu-config

## Verdict

approved

## Missing Requirements

- None for this vertical slice.

## Extra Behavior

- The tracked `.env.xianyu.example` file is deleted, not merely ignored,
  because the user explicitly rejected compatibility.
- Infrastructure job records are still auto-created with safe default cron
  expressions; authorized operators manage later scheduling changes in the
  existing infrastructure job page.

## Misunderstood Requirements

- None. “管理端管理一切配置” is implemented for tenant business configuration;
  the generic database encryption master key remains infrastructure-only as
  explicitly allowed by the accepted requirements.

## Requirements Coverage

- The database is the only XianGuanJia business-configuration source.
- The management API and UI cover credentials, endpoints, webhook, integration,
  writes, jobs, and synchronization parameters.
- AppSecret replacement, preservation, encryption, non-return, and access-log
  exclusion are implemented.
- Dynamic read/write clients, webhook tenant lookup, tenant jobs, and shipment
  denial use persisted configuration.
- Legacy `XGJ_*`, `rental.xianyu`, local environment loading, Spring scheduling,
  and startup synchronization paths are removed without fallback.

## Compatibility Review

- Existing read, sync, webhook, and shipment routes remain unchanged.
- This task intentionally removes configuration compatibility. Deployments must
  apply migration 029 and configure each tenant in the management page.
- Existing tenants remain safely disabled until configured.

## Cannot Verify From Diff

- Production database migration execution and encryption-key availability.
- Live webhook delivery and real shipment behavior against XianGuanJia.

## Acceptance Assertions Verified

- `A3`: existing business routes, permissions, tenant isolation, authentication
  storage, and shipment state transitions remain server-authoritative while
  the configuration source changes and one protected update route is added.

## Required Fixes

- None before development handoff.
