# Spec Review: 004-schedule-allocation

## Verdict

approved

## Missing Requirements

- No blocking task-level requirement is missing from the actual allowed-files
  diff.
- Recommendations and manual candidates use the complete occupied interval,
  not the billable interval. Incomplete occupied ranges disable both entry
  paths and prevent candidate rendering.
- Repair and locked devices remain blocked without relying on schedule blocks,
  and list mode no longer falls back to an immediate-availability claim.
- Timeline, dialog, candidate, removal, view-switch, and device-identity
  actions now provide at least a 44 CSS pixel compact target.

## Extra Behavior

- No new API route, backend contract, persistence mechanism, scheduling
  algorithm, or production dependency was introduced.
- Keeping the allocation dialog open after the existing assignment command
  settles avoids an unsupported optimistic success claim and preserves the
  server-authoritative workflow.

## Misunderstood Requirements

- None found. Billable and occupied ranges remain separate, end-exclusive
  command semantics are preserved, frontend availability is explicitly
  provisional, and permission/detail/period/integrity gates all remain active
  before assignment submission.

## Cannot Verify From Diff

- The current local management snapshot has no registered models. Populated
  lanes, allocation interaction, backend conflict rejection, successful
  assignment refresh, and responsive populated-dialog behavior remain assigned
  to task 007/change-level verification. No fixture or production-like data was
  fabricated for this review.
- Static inspection verifies focus trapping, Escape dismissal, localized close
  controls, focus restoration, non-color timeline labels, and 44px target
  classes. Populated browser execution of those paths remains part of the final
  verification matrix rather than a blocker for this bounded task slice.

## Acceptance Assertions Verified

- A3

## Required Fixes

- No blocking fixes remain for task 004 before development handoff.
