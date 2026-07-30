# Deploy Plan

## Environment

- Production host group target: server `211.101.246.160`.
- Deployment root: `/opt/camera-rental`.
- Verified release: `b5968cf257b05810033b53f360f636d0535fc5dd`.
- Public surface: `https://rental.motion-cover.com/admin/`.

## Command

```bash
# GitHub Actions invokes the host runner, which executes:
RELEASE_SHA=b5968cf257b05810033b53f360f636d0535fc5dd \
  bash ops/github-deploy/server-build-deploy.sh
```

## Config / Secrets

- Tenant XianGuanJia business configuration is persisted in
  `xianyu_application` and managed through the admin UI.
- The server has no supported `XGJ_*` or
  `RENTAL_XIANYU_JOB_STARTUPSYNCENABLED` compatibility path.
- `MYBATIS_PLUS_ENCRYPTOR_PASSWORD` remains an infrastructure secret and must
  not be printed or committed.

## Migrations

- Manifest: `development/migrations/manifest.json`.
- Deployment evidence: `operations/migration-deployment.json`.
- Apply migrations 029, 030, and 031 in manifest order after taking the
  database backup and before starting the updated backend.

## Smoke Checks

- `https://rental.motion-cover.com/admin/` returns HTTP 200.
- `https://rental.motion-cover.com/admin/schedule-center/` returns HTTP 200.
- Both release-info endpoints report `b5968cf2`.
- Backend service is active and starts without encryption, unknown-column, or
  bad-padding errors.

## Owner

- Release owner: 老大.
- Implementation and verification recorder: 小G.

## Deploy Window

- Completed on July 30, 2026 after the pre-migration database backup.
