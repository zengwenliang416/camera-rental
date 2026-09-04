# Reference Scan

Skill: `rental-device-label-qr`

## Why This Step Exists

Use a short benchmark pass before authoring the package in depth. External benchmark objects should define the pattern ceiling. Local files are used afterward only to calibrate fit, privacy, naming, and compatibility.

## Current Skill Anchor

- Title: `Rental Device Label QR`
- Description: Generate and verify camera-rental device QR label packages from CSV or TSV manifests containing pre-signed payloads. Use when exporting P4P, P3, or other rental-device labels in the Changsha Jiezu Da layout with shop name, phone, device label, right-side QR, 20x40 mm PNGs, A4 PDF, preview, manifest, and ZIP. Excludes QR protocol changes, signing-secret handling, database mutation, shipping, and generic QR design.

## Scan Focus

- **Execution pattern**: There is deterministic logic in scripts, so compare how strong references separate prose from executable steps.
- **Portability pattern**: The package carries neutral metadata, so scan how good references preserve semantics across targets without forking source.
- **Method pattern**: Use the core job description as the anchor for comparison: Generate and verify camera-rental device QR label packages from CSV or TSV manifests containing pre-signed payloads. Use when exporting P4P, P3, or other rental-device labels in the Changsha Jiezu Da layout with shop name, phone, device label, right-side QR, 20x40 mm PNGs, A4 PDF, preview, manifest, and ZIP. Excludes QR protocol changes, signing-secret handling, database mutation, shipping, and generic QR design.

## Priority Rule

- External benchmark objects set the pattern ceiling. User references refine taste and standards. Local files only calibrate fit, risk, and compatibility.

## External Benchmark Objects

- No explicit external benchmark objects recorded yet.
- Recommended: capture 2 to 5 external references at most.
- Suggested mix: one method reference, one structure reference, one execution or portability reference.

## User-Supplied References

### /var/folders/1v/8sfc4rjx1834mzlqzxqr8lr40000gn/T/codex-clipboard-78ab0014-0efd-4018-93a6-31a6b07892b5.jpg
- Category: `taste`
- Learn from: Learn what quality, tone, workflow shape, or operating standard the user wants to preserve.
- Do not copy: Do not copy wording, confidential material, or source-specific implementation details.

## Local Fit Check

### Current project QR protocol is CRD1|deviceNo|equipmentModelCode|sig16 and payload signing is owned by the backend/runtime, not this skill.
- Category: `general`
- Keep in mind: Keep the new skill compatible with real local constraints that matter.
- Do not inherit: Do not inherit private or outdated patterns just because they already exist locally.

### Default visible layout is left-side shop name, phone, device number plus serial suffix, and right-side QR.
- Category: `general`
- Keep in mind: Keep the new skill compatible with real local constraints that matter.
- Do not inherit: Do not inherit private or outdated patterns just because they already exist locally.

## Borrow Plan

- External benchmark first: let high-quality public references define the upper bound for method, structure, execution, or portability.
- User references second: use them to understand taste, standards, and directional preferences without copying source phrasing.
- Local fit third: use local assets only to detect naming conflicts, private dependencies, or compatibility constraints.
- Borrow patterns, not prose: extract loops, boundaries, metadata, and operator flow without copying source-specific language.
- Keep the package light: reject any borrowed pattern that increases context cost faster than it increases reliability.

## Non-Goals

- Do not copy source prose or branding into the new skill.
- Do not import gates that cost more context than they save.
- Do not use benchmark scanning to justify scope creep.
- Do not let local historical habits outrank stronger public benchmarks.
