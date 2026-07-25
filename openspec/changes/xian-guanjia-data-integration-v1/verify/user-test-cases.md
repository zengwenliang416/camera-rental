# User-Aligned Test Cases: xian-guanjia-data-integration-v1

## User Test Case Scope

- Source requirements: `openspec/changes/xian-guanjia-data-integration-v1/requirements.md`
- Acceptance criteria: `openspec/changes/xian-guanjia-data-integration-v1/acceptance.md`
- Prototype handoff: `openspec/changes/xian-guanjia-data-integration-v1/prototype/handoff.md`
- Development handoff: `openspec/changes/xian-guanjia-data-integration-v1/development/handoff-to-verify.md`

## Aligned Test Cases

### `utc-masked-config`

- Actor: integration operator
- User goal: confirm integration status without seeing secrets
- Preconditions: runtime may be disabled; env credentials optional
- Steps: open rental xianyu config page or call config get API
- Expected result: status and masked AppKey; AppSecret never returned
- Boundary / error / permission states: disabled/missing credentials shown safely; permission rental:xianyu:query required
- Acceptance refs: acceptance security boundary

### `utc-shop-order-sync`

- Actor: integration operator
- User goal: sync authorized shops and one order page
- Preconditions: XGJ enabled with credentials; migrations applied
- Steps: sync shops; choose shop; run bounded order sync window
- Expected result: shop rows and channel orders persisted; sync run counts visible
- Boundary / error / permission states: disabled integration fails safely; missing authorize id blocked
- Acceptance refs: order sync and cursor rules

### `utc-convert-and-assign`

- Actor: rental/equipment operator
- User goal: convert channel order and assign device
- Preconditions: mapping/dates valid or review path
- Steps: convert channel order; create device; assign with occupy half-open range
- Expected result: at most one rental order; overlap conflict rejected; idempotent assign
- Boundary / error / permission states: review-required when mapping/date incomplete; conflict typed error
- Acceptance refs: conversion and device scheduling

## User Signoff

Status: `approved`

Operator signoff freezes the test-case definitions. It does not waive browser,
database, runtime, red-team, or sensory evidence.

## Domain Mapping

Each approved user test case is mapped to facticity, static, unit, redteam, e2e, and sensory domains via domain-case-matrix.json.
