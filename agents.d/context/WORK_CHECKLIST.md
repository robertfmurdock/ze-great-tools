# Task Checklist

Use this checklist for every implementation task.

## Terminology

**Work Card (or "card")**: A markdown file in `agents.d/work/` that defines a task with goals, constraints, identified issues, and an implementation checklist. This is the project's task tracking system, NOT Claude Code's built-in task tracking tool. When the user asks you to "create a work card," you should create a markdown file in `agents.d/work/` following the format seen in existing work cards in that directory.

## Intake
- Work cards live in `agents.d/work/`.
- Read:
  - `agents.d/context/PERSONA.md`
  - `agents.d/context/PLAYBOOK_CODE_STYLE.md` — when modifying source code
  - `agents.d/context/GRADLE_PLAYBOOK.md` — when modifying Gradle build logic or dependencies
  - `agents.d/context/GITHUB_ACTIONS_PLAYBOOK.md` — when adding or changing GitHub Actions workflows
- Identify impacted modules and likely test scope.
- Define test-level intent up front:
  - primary property to prove,
  - highest architecture level needed for confidence-anchor coverage,
  - candidate variations/permutations to place at lower integration levels.
- Confirm constraints and assumptions before coding.

## Implementation
- If the work card has no `## Checklist` section, create one before writing any code.
  List each planned slice as an unchecked item (`- [ ] ...`). This is the first slice.
  The second-to-last item must always be `- [ ] Review changes against applicable playbooks and verify compliance`.
  The final item must always be `- [ ] Move this file to agents.d/work_completed/`.
- Keep changes focused on impacted modules.
- Follow existing patterns and module ownership.
- Before changing something that looks wrong (especially a flag or setting overriding a default), confirm it isn't intentional. Surface the ambiguity; don't silently "fix" it.
- Prefer existing libraries and build tooling over custom implementations.
- Update all linked artifacts for cross-layer changes.
- **Each slice or step must be integration-oriented**: the repository should be in a safe,
  check-in-ready state after every slice, so work can be paused and resumed at any slice boundary.
- **End every slice by marking it complete in the work card** (`agents.d/work/<CARD>.md`).
  Do not batch work card updates to the end — update as you go.

## Validation
- Run smallest sufficient task set first for quick feedback.
- Use Gradle wrapper (`./gradlew`) only.
- Before completing any task, run `./gradlew check` to verify no cross-module surprises — let the build tooling determine impact, do not anticipate it.
- Validate the test mix:
  - confidence-anchor coverage exists at the intended boundary level,
  - variation coverage is pushed downward where possible without reducing confidence.
- Review changes against applicable playbooks to verify compliance before marking work complete.

## Completion Report
- List files changed and intent.
- List validation commands run and results.
- Confirm all slices are marked `[x]` in the work card, then move it to `agents.d/work_completed/`.
- State residual risks, skipped checks, or follow-ups.
