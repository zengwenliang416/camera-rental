# Reference Synthesis

Skill: `rental-device-label-qr`
- Description: Generate and verify camera-rental device QR label packages from CSV or TSV manifests containing pre-signed payloads. Use when exporting P4P, P3, or other rental-device labels in the Changsha Jiezu Da layout with shop name, phone, device label, right-side QR, 20x40 mm PNGs, A4 PDF, preview, manifest, and ZIP. Excludes QR protocol changes, signing-secret handling, database mutation, shipping, and generic QR design.
- Intent confidence: `100/100` (`high`)

## Live GitHub Benchmarks

- No live GitHub benchmarks are attached yet.

## Curated World-Class Pattern Tracks

### Official workflow product ergonomics
- Type: `official`
- Evidence mode: `curated-pattern-track`
- Why relevant: This track matches: review.
- Borrow: Borrow a first-time operator flow that explains itself before it asks for more structure.
- Avoid: Do not mimic product polish that adds UI bulk without improving clarity.

### Human-in-the-loop verification
- Type: `research`
- Evidence mode: `curated-pattern-track`
- Why relevant: This track matches: review.
- Borrow: Borrow a review checkpoint wherever trust matters more than raw speed.
- Avoid: Do not force every skill through heavyweight review when the risk is low.

### Boundary-first design
- Type: `principles`
- Evidence mode: `curated-pattern-track`
- Why relevant: This track matches: route, exclude.
- Borrow: Borrow the discipline of defining what the skill should not own before growing the package.
- Avoid: Do not expand execution assets until route boundaries stay clean.

## Borrow Now

- Borrow a first-time operator flow that explains itself before it asks for more structure.
- Borrow a review checkpoint wherever trust matters more than raw speed.
- Borrow the discipline of defining what the skill should not own before growing the package.
- Learn what quality, tone, workflow shape, or operating standard the user wants to preserve.

## Avoid Now

- Do not mimic product polish that adds UI bulk without improving clarity.
- Do not force every skill through heavyweight review when the risk is low.
- Do not expand execution assets until route boundaries stay clean.
- Do not copy wording, confidential material, or source-specific implementation details.

## Pattern Gate

- Summary: 4 accepted, 0 deferred using threshold 3/4.
- Acceptance threshold: `3/4`
- Accepted patterns:
  - **Official workflow product ergonomics**: 4/4 (recurrence, generativity, distinctiveness, boundary)
  - **Human-in-the-loop verification**: 4/4 (recurrence, generativity, distinctiveness, boundary)
  - **Boundary-first design**: 4/4 (recurrence, generativity, distinctiveness, boundary)
  - **/var/folders/1v/8sfc4rjx1834mzlqzxqr8lr40000gn/T/codex-clipboard-78ab0014-0efd-4018-93a6-31a6b07892b5.jpg**: 4/4 (recurrence, generativity, distinctiveness, boundary)

## Default Recommendation

- Summary: Start by borrowing this pattern: Borrow a first-time operator flow that explains itself before it asks for more structure. Avoid this for the first pass: Do not mimic product polish that adds UI bulk without improving clarity.
- Why: There is a real design conflict to resolve: The stated preference leans lightweight or speed-first, while the benchmark mix leans toward governance, review, or heavier evaluation structure.
- User decision required: `True`

## Visibility Mode

- Mode: `explicit`
- Reasons: design_conflict
- User note: Surface the recommendation because intent is still settling or there is a real design conflict that needs a user call.
- Reviewer note: Keep the full benchmark and synthesis evidence visible for authors and reviewers.

## Conflict Check

- **lightweight_vs_governance**: The stated preference leans lightweight or speed-first, while the benchmark mix leans toward governance, review, or heavier evaluation structure.

## Quality Lift Thesis

- Use GitHub repositories for concrete package and workflow patterns.
- Use curated official or commercial tracks for entrypoint and operator ergonomics.
- Use research tracks to justify the smallest evaluation loop that still catches regressions.
- Use principle tracks to keep the package small, boundary-aware, and outcome-driven.

## Decision Prompt

Use the recommendation by default. Only surface the underlying benchmark tradeoffs when intent is uncertain or a real design conflict needs a deliberate call.
