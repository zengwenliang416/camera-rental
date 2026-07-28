# Independent Sensory Review

## Decision

`green` for the 2026-07-28 local device-schedule-center regression.

## Reviewed Behaviors

| Behavior | Result | Evidence |
|---|---|---|
| Direct shipping entry does not select the first order | Pass | Browser showed `未选择`; `QuickBindingView.tsx:102-115` only honors explicit handoff |
| Waybill survives order selection | Pass | `SENSORY-LOCAL-20260728` remained after selecting `5126403888047003436` |
| Waybill survives carrier selection | Pass | Value remained after switching to `京东快递 (京东)` |
| Write-disabled safety state | Pass | Visible warning, disabled `确认发货`, context-level write gate, no ship call |
| Shipped order is not shown as exception | Pass | `5126359104930006425` rendered `租赁中`; mapper status precedence and tests agree |
| Rental period uses backend authority | Pass | `2026-07-29 至 2026-08-05`; no editable start/end inputs |
| Recipient contact is masked | Pass | `杨*`, `188****0175`; full phone absent |
| Runtime interaction stability | Pass | No browser console errors; 8/8 mapper tests and TypeScript check passed |

## Non-Blocking Findings

1. Device and order rows use clickable `div` elements without keyboard role,
   focus, or key handlers.
2. The active shipping component is 853 lines, while an unused 644-line legacy
   modal remains and can drift from the active behavior.
3. Orders without receiver snapshots can fall back to a seller/buyer nickname
   outside the recipient masking helper.
4. The orders page renders a large data set without pagination or
   virtualization; no formal performance measurement was supplied.

## Required Fixes

None for this sensory gate.

## Safety Statement

No production code was edited. No real shipment, deployment, push, credential
inspection, or external side effect was performed.
