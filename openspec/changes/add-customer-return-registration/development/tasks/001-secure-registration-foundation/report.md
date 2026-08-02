# Task Report: 001-secure-registration-foundation

## Status

DONE

## Files Changed

- `camera-rental-server/sql/mysql/migrations/20260801_036_customer_return_registration.sql`
- Return-registration DOs, Mappers, enums, error codes, token resolver and admin link creation service.
- Focused token and admin creation tests.

## What Changed

- Added three tenant-scoped tables for registrations, submitted devices and attachments.
- Added globally unique SHA-256 token hashes; the plaintext 256-bit token is returned once and never persisted.
- Added tenant-ignore token resolution followed by explicit tenant context restoration.
- Added expiry, revoke and safe unavailable-link states.

## TDD Evidence

- `ReturnRegistrationTokenServiceTest` covers 256-bit URL-safe entropy, stable hashing and rate-limit key privacy.
- `ReturnRegistrationAdminServiceTest` proves only the hash is persisted and the one-time plaintext token forms the share path.
- Submission and public-context tests exercise draft, terminal and unavailable states through the same persistence boundary.

## Verification Commands

- `mvn -pl yudao-module-rental/yudao-module-rental-biz -am -DskipTests=false -Dtest='ReturnRegistration*Test,FileApiImplTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- `sha256sum` and `cmp` across production migrations and SpecNav audit copies.
- `git diff --check`

## Concerns

- Production MySQL execution is owned by task 006 and remains pending until the committed SHA deploys.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Verify the applied migration checksum and table/index shape on `154.9.235.80` after deployment.

## Adjudication

Implementation and local evidence are complete; production migration evidence remains in task 006.
