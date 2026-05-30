# Task Checklist

Use this checklist for every implementation task.

## Terminology

**Work Card (or "card")**: A markdown file in `agents.d/work/` that defines a task with goals, constraints, identified issues, and an implementation checklist. This is the project's task tracking system, NOT Claude Code's built-in task tracking tool. When the user asks you to "create a work card," you should create a markdown file in `agents.d/work/` following the template below.

## Work Card Template

```markdown
# [Feature Name]

## Goal
One-sentence outcome.

## Constraints
- Hard boundaries from PERSONA, playbooks, or architecture
- Declare initial semver intent (`[none]`, `[patch]`, `[minor]`, `[major]`) and update it if implementation discoveries change impact.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] [Broad feature slice 1]
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] [Broad feature slice 2]
  ...
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes
[Semver intent (initial): expected level + one-line rationale; update if it changes]
[Agents log discoveries, deviations, or learned constraints here as they work]

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
```

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

### Work Card Structure
- The first checklist item must always be: `Review this work card for compliance with template and update to conform`.
- If a card intends to use subagents at any point, the second checklist item must be: `If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes`.
- Checklist items should be broad feature slices, not micro-tasks.
- The second-to-last item must always be `- [ ] Review changes against applicable playbooks and verify compliance`.
- The final item must always be `- [ ] Move this file to agents.d/work_completed/`.
- **Red flags during review**: If a work card suggests "tests may fail initially" or "test failures document baseline" or similar language implying incomplete TDD cycles, this violates the agent cycle requirement. Either adjust the work card to complete the full cycle (fix issues to make tests pass) or clarify that tests should be observation-only (not assertions that fail).
- **Red flags during review**: If a work card references subagents/delegation but does not include an explicit user-authorization prompt item, the card is non-compliant and must be updated before execution.

### Refactoring

**Light refactor** (during feature slices): Clean up what you just wrote - names, duplication, structure.

**Final refactor**: Review ALL code changes made throughout the entire work card, not just the final iteration.
See `agents.d/context/REFACTOR_AGENT.md` for the complete mandatory checklist and reporting requirements.

The final refactor agent must:
1. **Identify all commits** in the work card scope using git log
2. **List all modified files** across the full commit range (not just current state)
3. **Review each file completely** - read entire files, not just changed sections
4. **Check function evolution** - functions modified multiple times may have accumulated complexity
5. **Apply code style guidelines** - function length, naming, data flow, comment removal
6. **Verify no cruft** - intermediate refactorings may have left technical debt
7. **Run full check** to verify no cross-module impact

**Delegation pattern**: The orchestrator should spawn a specialized refactor agent by passing
the commit range and referencing REFACTOR_AGENT.md in the prompt. The agent must provide
a structured report with evidence for each check.

The final refactor must be comprehensive - incomplete review leaves quality issues that accumulate over time.

### Agent Cycle Within Each Feature Slice
Each broad checklist item follows a test-driven cycle that repeats until the feature slice is complete:

1. **Test**: Write a single test that advances the feature. Confirm it fails for the intended reason or passes for the intended reason if verifying existing behavior.
2. **Implement**: Do the simplest thing that could possibly work to make the test pass.
3. **Refactor-light**: Clean up what you just wrote - names, duplication, structure.
4. **Verify pushable**: Run validation (smallest sufficient task set) to ensure the repository is in a safe, check-in-ready state.
   - For [minor] or [patch] changes that introduce alternatives to existing APIs, explicitly verify backward compatibility — test that both old and new APIs work as expected.
   - **All tests must pass.** Do not commit failing tests. If a test fails, fix the issue to make it pass within the same cycle.
5. **Commit**: When all tests pass, commit with semver annotation:
   - `[major]` — breaking change
   - `[minor]` — new backward-compatible functionality
   - `[patch]` — bug fix, refactor, anything affecting build output (e.g., `[patch] adding tagger error message link to help resolve common issue`)
   - `[none]` — no build output impact: docs, work cards, build config (e.g., `updating a card`, `work item update`)
   
   **For CLI tools**: stdout is first-class API (parseable contract); stderr is diagnostic (flexible). Changing stdout format is `[major]`, improving stderr clarity is `[patch]`. Structured output formats (JSON, etc.) are API; text formats for humans are diagnostic.

**Subagent pattern**: Orchestrator spawns specialized subagents for each phase (testing subagent → implementation subagent → refactor subagent). Orchestrator coordinates the cycle, updates the work card, and adapts the plan as constraints are discovered.

**Subagent authorization gate (required)**:
1. Before spawning any subagent, ask the user explicitly in-thread: `Authorize subagent delegation for this card? (yes/no)`.
2. Record the user's answer in the card's Implementation Notes (with date).
3. If the user does not explicitly answer yes, continue in single-agent mode.
4. If work resumes in a new chat/thread, ask again before any new subagent spawn, even when prior authorization is recorded in the card.

### Deprecation Workflow
When replacing existing functionality:
1. **Build and test the new feature first** — it must be fully functional with behavioral tests proving feature parity before deprecating the old API.
2. **Mark the old API as deprecated** with required elements:
   - Why it's deprecated
   - What replaces it (migration pattern)
   - State "may be removed in next major version"
3. **Test both old and new APIs** to verify backward compatibility.
4. **Removal timing**: Deprecated code may be removed at any major version boundary, but never in [minor] or [patch] releases.

### Adaptation During Work
- **Project guidelines take precedence over initial work card plans.** If PERSONA, playbooks, or discovered architecture constraints conflict with the work card plan, update the plan.
- Before marking a checklist item complete, review remaining items. If project guidelines revealed constraints not reflected in the plan, update the checklist and log the discovery in Implementation Notes.
- Do not lock into the original spec — adapt as you learn.
- If new findings suggest semver impact may increase (for example `[patch]` → `[minor]` or `[major]`), pause and ask the user to confirm direction before proceeding. Record the decision in Implementation Notes with date.

### General Practices
- Keep changes focused on impacted modules.
- Follow existing patterns and module ownership.
- Before changing something that looks wrong (especially a flag or setting overriding a default), confirm it isn't intentional. Surface the ambiguity; don't silently "fix" it.
- Prefer existing libraries and build tooling over custom implementations.
- Update all linked artifacts for cross-layer changes.
- **Each checklist item must result in a pushable state**: the repository should be safe to check in after every item, so work can be paused and resumed at any boundary.
- **Mark checklist items complete as you go** (`agents.d/work/<CARD>.md`). Do not batch updates to the end.
- Log meaningful discoveries, deviations, or learned constraints in the Implementation Notes section as you work.

## Validation
- Run smallest sufficient task set first for quick feedback.
- Use Gradle wrapper (`./gradlew`) only.
- Before completing any task, run `./gradlew check` to verify no cross-module surprises — let the build tooling determine impact, do not anticipate it.
- Validate the test mix:
  - confidence-anchor coverage exists at the intended boundary level,
  - variation coverage is pushed downward where possible without reducing confidence.
- Review changes against applicable playbooks to verify compliance before marking work complete.
- Fill in the Validation section of the work card with commands run and results as work progresses.

## Completion Report
- List files changed and intent.
- List validation commands run and results (should already be in work card Validation section).
- Confirm all slices are marked `[x]` in the work card, then move it to `agents.d/work_completed/`.
- State residual risks, skipped checks, or follow-ups.
