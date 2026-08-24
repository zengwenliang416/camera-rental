# Task Brief: 005-verification-handoff

## Goal

Close development with traceable task evidence and hand the immutable approved
scope to SpecNav Verification 2.0 without bypassing snapshot approval.

## Parent Artifacts

- `openspec/changes/add-xianyu-dispatch-backfill/requirements.md`
- `openspec/changes/add-xianyu-dispatch-backfill/acceptance.md`
- `openspec/changes/add-xianyu-dispatch-backfill/prototype/handoff.md`

## Vertical Slice

Every baseline task has ownership, implementation evidence, independent spec
and quality review, and an explicit verification handoff; six-domain execution
starts only after the immutable case snapshot ID and SHA-256 receive human
approval.

## In Scope

- Checklist items `5.1` through `5.3`.
- Approved prototype binding, development entry, task reports/reviews/ledgers,
  migration-not-required evidence, and development handoff.

## Out Of Scope

- Claiming Verification 2.0 success before a machine-authoritative report.
- Changing approved requirements, prototype variant, production scope, or
  baseline task identities during handoff.

## Files Allowed

- `openspec/changes/add-xianyu-dispatch-backfill`

## Components To Create

- Development task evidence, reviews, ledgers, and the handoff consumed by the
  later Verification 2.0 case contract.

## Components To Reuse

- Approved `admin-dialog-v1`, foundation specifications, SpecNav development
  contracts, and SpecNav Verification 2.0 six-domain workflow.

## Components To Extract

- No production component extraction occurs in the handoff slice; unresolved
  duplication must return to the owning implementation task.

## TDD Requirement

- Verification cases must bind to approved acceptance assertions and immutable
  evidence inputs rather than ad hoc manual checks.

## Verification Commands

- `SPECNAV_CHANGE=add-xianyu-dispatch-backfill node /Users/wenliang_zeng/.codex/plugins/cache/specnav-marketplace/specnav-development/0.3.0/scripts/development-contract.js --mode entry --json`
- `SPECNAV_CHANGE=add-xianyu-dispatch-backfill node /Users/wenliang_zeng/.codex/plugins/cache/specnav-marketplace/specnav-development/0.3.0/scripts/development-contract.js --mode handoff --json`
- `openspec validate add-xianyu-dispatch-backfill --strict`

## Stop Conditions

- Scope lock mismatch.
- Any task report, review, ledger, validation log, or handoff still contains a
  scaffold marker or unsupported claim.
- The handoff attempts to claim an immutable Verification 2.0 snapshot or
  six-domain result that has not yet been produced and approved.

## Unsafe Assumptions

- Prototype approval authorizes the UI variant, not the Verification 2.0 case
  snapshot.
- Development checks and browser evidence are inputs to verification, not a
  substitute for the six-domain machine-authoritative report.
