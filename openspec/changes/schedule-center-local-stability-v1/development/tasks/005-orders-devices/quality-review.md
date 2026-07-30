# Quality Review: 005-orders-devices

## Verdict

approved

## Separation Of Concerns

- The former 320-line order page and 196-line device page are thin
  compatibility entries. Order/device filtering, status presentation, action
  derivation, page coordination, cards, and shared responsive list primitives
  are separated into bounded modules.
- Presentational cards do not import API clients. Pages consume mapped records
  and existing navigation/overlay intents through the approved `useApp()`
  compatibility seam.
- General order cards no longer import or invoke the return command, preserving
  the server-authoritative return/inspection workflow boundary.
- Mapper-created display fragments are converted into a small device-card read
  model before locale-aware rendering instead of adding locale or transport
  concerns to `DeviceCard`.

## Component Cohesion / Coupling

- `FilterToolbar`, `IdentifierText`, and `ResponsiveDataList` are cohesive
  shared components with behavior-facing inputs and no feature or transport
  dependency.
- Order and device filtering is centralized in pure models. Registered counts
  use the actual device array, and model totals are derived from the same
  management records rather than unit-number ranges.
- `OrderActionAvailability` now models assignment, shipment, and device-detail
  permission inputs together with backend flags and assigned identity.
  `DeviceCardPresentation` owns status-aware availability and normalized note
  semantics, keeping the component focused on rendering and intent emission.

## Test Quality

- Independently rerun the focused 005 command: 8 tests passed, 0 failed. It
  covers safe order filters, status/channel tones, direct-return exclusion,
  denied device-detail permission, registered identity/count behavior,
  semantic device tones, blocked-state availability, and mapper-copy
  normalization.
- Independently rerun `pnpm test`: 61 tests passed, 0 failed. Existing mapper
  tests also confirm ordinary order customer name and phone remain masked and
  assignment identifiers come from backend records.
- An additional executed populated-English assertion verified that all five
  non-idle statuses avoid immediate availability and that normalized values
  render through `Available now`, `Unavailable in current state`, and
  `Warehouse` locale entries.
- Empty `zh-CN` and `en` browser evidence is useful but cannot validate
  populated value localization. The populated matrix remains appropriately
  deferred to task 007 without requiring database fixtures.

## Error Handling

- Empty management data and empty filtered results have distinct localized
  states. Filters remain local component state and do not persist private
  values.
- Assignment and shipment actions combine server action flags with their
  existing permissions, and direct return is represented as guidance rather
  than a mutation.
- Device-detail permission is consistent across order and device cards.
  Missing availability dates now produce immediate availability only for
  `IDLE`; non-idle states fail safely to localized unavailable copy.

## Reuse / Duplication

- Order/device filters, tones, registered summaries, identifiers, date ranges,
  status badges, filter layout, empty states, and responsive list layout are
  extracted and reused.
- Device availability and warehouse presentation are centralized in
  `deviceCardPresentation`, removing the unsafe card-local fallback and
  preventing mapper-created Chinese labels from leaking into the English
  populated path.
- No new production dependency was added.

## Complexity Delta

- Independently rerun `pnpm lint` and `pnpm build`; TypeScript no-emit and the
  Vite production build both exited 0. Orders and devices remain separate lazy
  chunks.
- Repository `git diff --check` passed. No production TypeScript, TSX, or CSS
  file exceeds 600 physical lines; the largest file is `messages.ts` at 599
  lines. The 005 pages, cards, models, and shared primitives remain well below
  300 lines.
- The structural complexity delta is positive: large legacy pages are replaced
  by bounded feature modules without adding a state library, transport
  dependency, persistence layer, or duplicated business command.

## Required Fixes

- No blocking quality fixes remain for task 005 before development handoff.
