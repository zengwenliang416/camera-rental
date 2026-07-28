# Spec Review: 004-order-sync-orchestration

## Verdict

needs-fix

## Missing Requirements

- The brief and report require `XianyuOrderSyncService` to advance the durable
  cursor after every detail in the page is persisted. The current service
  never advances a cursor and always returns `cursorAdvanced=false`.

## Extra Behavior

- The page service now persists page raw evidence, records sync-failure alerts,
  supports local page replay, skips fresh details, and invokes automatic
  conversion. These are later orchestration concerns not described by this
  task packet.

## Misunderstood Requirements

- Cursor advancement was moved to `XianyuChannelSyncService`, where it occurs
  once after all pages in a fixed window succeed and the count remains stable.
  That is a defensible and safer design, but it is not the one-page contract
  claimed by the brief and report.

## Cannot Verify From Diff

- The report's claim that this page service advances the cursor is contradicted
  by both current code and `XianyuOrderSyncServiceTest`.
- A1 does not define a cursor assertion and the schedule-center browser
  artifact does not exercise cursor advancement.

## Required Fixes

- Align the approved task contract and report with the current window-level
  cursor design, or restore the documented page-level advancement behavior.
- Record a current system-executed test proving that any page failure or count
  drift prevents the single window-level cursor advance.
