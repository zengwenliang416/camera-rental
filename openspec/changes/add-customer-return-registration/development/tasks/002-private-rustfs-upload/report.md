# Task Report: 002-private-rustfs-upload

## Status

DONE

## Files Changed

- Infra `FileApi`/`FileService` presigned upload confirmation, preview and object deletion extensions.
- Return-registration attachment service, endpoints, DTOs and tests.
- `ops/rustfs` Compose, installation, bootstrap, private policy, backup, Nginx example and documentation.

## What Changed

- Added token-scoped upload authorization without exposing object-store credentials.
- Added MIME signature, 15 MiB size, count, category, ownership, SHA-256 and replacement checks.
- Added removal of both confirmed `infra_file` records and unconfirmed uploaded objects.
- Added loopback-only S3/console ports, private bucket bootstrap and a bucket-scoped application service account.

## TDD Evidence

- `FileApiImplTest` rejects oversized objects even when metadata already exists.
- `ReturnRegistrationAttachmentServiceTest` covers unsupported MIME, confirmed and unconfirmed removal, required categories and object replacement.
- RustFS configuration is validated through Docker Compose and shell syntax checks.

## Verification Commands

- Focused Maven command in task 001.
- `bash -n ops/rustfs/*.sh`
- `RUSTFS_ACCESS_KEY=x RUSTFS_SECRET_KEY=y RUSTFS_APP_ACCESS_KEY=a RUSTFS_APP_SECRET_KEY=b docker compose -f ops/rustfs/docker-compose.yml config --quiet`

## Concerns

- RustFS is still pre-1.0; production backup and health monitoring are required.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Verify private bucket policy, application service account, S3 endpoint TLS and that port 9001 is not publicly reachable.

## Adjudication

The local storage and upload boundary is complete; live RustFS installation and policy evidence remains in task 006.
