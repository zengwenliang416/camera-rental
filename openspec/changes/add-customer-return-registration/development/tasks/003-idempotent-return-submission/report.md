# Task Report: 003-idempotent-return-submission

## Status

DONE

## Files Changed

- Public context and submission controller/service.
- Serial normalization and device matching persistence.
- Delivery channel-order compatibility changes and migration 034.
- Submission, attachment and serial tests.

## What Changed

- Added order-bound public context and a row-locked submission transaction.
- Normalizes 1-8 serials, matches only active assignments and preserves duplicate/unmatched evidence.
- Creates or reuses one local-only `RETURN` Delivery only when all devices and attachments are safe.
- Persists `REVIEW_REQUIRED` without Delivery or device/order/inspection/schedule lifecycle changes when matching or issue review is needed.
- Repeated submissions return the original receipt without duplicate writes.

## TDD Evidence

- `ReturnRegistrationSubmissionServiceTest` covers safe Delivery creation, mismatch review, duplicate submission and required-photo rejection.
- `ReturnSerialNormalizerTest` covers the physical short serial format.
- Attachment revalidation tests prevent cross-object replacement before submission.

## Verification Commands

- Focused Maven command in task 001.
- `git diff --check`

## Concerns

- Concurrent behavior is protected by row locks and unique Delivery keys; production database evidence remains part of task 006.

## Scope Deviations

- None recorded.

## Follow-up Needed

- Verify one synthetic accepted submission and one review-required submission against the deployed database.

## Adjudication

The backend transaction and no-side-effect contracts are locally complete.
