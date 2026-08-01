# Task Brief: 002-private-rustfs-upload

## Goal

Provide registration-scoped private photo upload through the existing infra file
service and a production-ready RustFS S3 deployment.

## Vertical Slice

A customer with a valid return token can authorize, upload, confirm, preview and
remove categorized photos without receiving reusable object-store credentials or
access to another registration.

## In Scope

- Infra API extension, rental attachment policy and public attachment endpoints.
- RustFS container, private console, persistent volumes, health and backup assets.
- Required exterior/SN categories and optional packaging category.
- Extract attachment policy and upload state contracts; reuse FileService and S3 client.

## Files Allowed

- `camera-rental-server/yudao-module-infra/yudao-module-infra-api`
- `camera-rental-server/yudao-module-infra/src/main/java`
- `camera-rental-server/yudao-module-rental/yudao-module-rental-biz`
- `ops/rustfs`
- `openspec/changes/add-customer-return-registration`

## Verification Commands

- `mvn -pl yudao-module-infra,yudao-module-rental/yudao-module-rental-biz -am -Dtest='*File*Test,*ReturnRegistrationAttachment*Test' test`
- `docker compose -f ops/rustfs/docker-compose.yml config`
- `git diff --check -- camera-rental-server ops/rustfs`

## Stop Conditions

- Stop if implementation exposes generic anonymous upload or long-lived credentials.
- Stop if object ownership cannot be tied to one token and tenant.
- Stop if RustFS console or Bucket would be publicly accessible.
