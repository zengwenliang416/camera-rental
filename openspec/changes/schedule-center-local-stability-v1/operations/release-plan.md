# Release Plan: schedule-center-local-stability-v1

## Release Target

`project-deploy`

## Required Artifacts

- `operations/readiness.md`
- `operations/readiness.json`
- `operations/release-checklist.json`
- `operations/changelog.md`
- `operations/release-notes.md`
- `operations/deploy-plan.md`
- `operations/rollback-plan.md`
- `operations/monitor-plan.md`
- `operations/migration-deployment.json`
- `operations/production-deployment-evidence.json`
- `operations/update-spec.json`

## Release Decision

Release `b5968cf257b05810033b53f360f636d0535fc5dd` is deployed. The
operations evidence commit is documentation-only and should use `[skip ci]` so
it does not replace the verified production release.
