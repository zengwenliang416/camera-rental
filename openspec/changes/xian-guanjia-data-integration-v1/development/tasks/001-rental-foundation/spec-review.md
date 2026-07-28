# Spec Review: 001-rental-foundation

## Verdict

needs-fix

## Missing Requirements

- The task requires safe runtime defaults. `XianyuProperties.writeEnabled`
  currently defaults to `true`, and `XianyuPropertiesTest` explicitly locks in
  that unsafe object-level default. The YAML override to `false` does not make
  the configuration type safe when it is instantiated or bound outside those
  files.

## Extra Behavior

- A third-party write switch is now present in the foundation configuration
  even though this task and the parent V1 requirements exclude all
  XianGuanJia write capabilities.

## Misunderstood Requirements

- Defaulting the read integration to disabled is not sufficient once a write
  client exists in the same module. The independent write gate must also be
  fail-closed at the configuration-object boundary.

## Cannot Verify From Diff

- The historical system-executed Maven receipt proves the original
  default-disabled read configuration, but it predates the current
  `writeEnabled=true` behavior.
- A1 remains an unresolved placeholder statement and therefore does not define
  a foundation assertion that can be independently verified.

## Required Fixes

- Change the Java default for `writeEnabled` to `false` and update the focused
  configuration test to require the fail-closed default.
- Re-run the focused configuration test and record current system-executed
  evidence before requesting approval again.
