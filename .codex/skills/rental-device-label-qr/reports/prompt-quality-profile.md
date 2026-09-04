# Prompt Quality Profile

Skill: `rental-device-label-qr`
Relevance: `prompt-heavy`
Overall quality score: `89.0/100`

## Primary Task Family

**Prompt engineering**
- Matched keywords: prompt, role, format

## Complexity

- Band: `expert`
- Score: `11`
- Reason: multiple task families plus governance, evaluation, or expert-level constraints

## Need Model

- Explicit Need: Generate repeatable, print-ready rental device QR label packages in the approved shop-label layout.
- Implicit Need: The reusable skill needs a stable role, task, and output contract rather than a one-off prompt.
- Scenario: A CSV or TSV device manifest containing device_no, serial_number, and a pre-signed QR payload.
- User Level: infer from examples and standards; ask only if it changes output depth
- Success Standard: Every generated PNG must be 945x472 at 600 DPI, every QR must decode to the exact provided payload, sequence and filenames must be deterministic, and the PDF must be valid A4.

## RTF To Skill Mapping

- Role: Use a prompt engineer role only when role design materially improves execution.
- Task: Map Role, Task, and Format into skill behavior rather than copying a large prompt template.
- Format: Return a compact prompt contract plus tests, quality matrix, and usage notes.

## Quality Matrix

### Completeness — 95/100
- Matched signals: output, example
- Repair: Name missing inputs, outputs, constraints, or success standards before deepening the package.

### Clarity — 90/100
- Matched signals: clear, specific
- Repair: Replace broad verbs with observable actions and define what done means.

### Consistency — 85/100
- Matched signals: boundary
- Repair: Check that role, task, format, exclusions, and examples do not contradict each other.

### Practicality — 95/100
- Matched signals: execute, use, workflow
- Repair: Add runnable steps, examples, or verification cues instead of abstract advice.

### Specificity — 80/100
- Matched signals: none
- Repair: Anchor wording in the user's audience, domain nouns, and target outcome.

## Matched Task Families

### Prompt engineering
- Score: `3`
- Keywords: prompt, role, format
- Role: Use a prompt engineer role only when role design materially improves execution.
- Task: Map Role, Task, and Format into skill behavior rather than copying a large prompt template.
- Format: Return a compact prompt contract plus tests, quality matrix, and usage notes.

### Creative generation
- Score: `2`
- Keywords: copy, content
- Role: Use a taste-aware creator role with clear audience, tone, and originality boundaries.
- Task: Generate variants, explain selection logic, and preserve the user's distinctive constraints.
- Format: Return options with rationale, selection criteria, and refinement paths.

### Execution operation
- Score: `2`
- Keywords: workflow, execute
- Role: Use an operator role with explicit boundaries, inputs, outputs, and failure handling.
- Task: Convert the job into ordered steps with validation checks and stop conditions.
- Format: Return a runbook-like handoff with commands, checks, owners, and next actions when relevant.

### Teaching guidance
- Score: `1`
- Keywords: tutorial
- Role: Use a teacher role that adapts to learner level and avoids overloading the first pass.
- Task: Explain through progressive steps, examples, and visible success checks.
- Format: Return learner-facing sections, worked examples, checkpoints, and common mistakes.

## Self-Repair Checks

- Check explicit need, implicit need, scenario, user level, and success standard before deepening.
- Map Role, Task, and Format into skill behavior, not decorative prompt labels.
- Ask one focused clarification only when missing information changes the package boundary.
- Add tests or examples for prompt-heavy behavior before treating it as reusable.
- Keep prompt methodology in references and reports instead of bloating SKILL.md.

## Reviewer Note

Use this profile when the package depends on prompt behavior, role design, output contracts, or conversation quality.
