# Strengthen Test-First Discipline in Agent Context System

## Goal
Modify context documents and work card templates to more aggressively bias agents toward test-first discipline, preventing post-hoc rationalization for skipping tests.

## Constraints
- Must not make testing requirements so rigid that legitimate edge cases (pure configuration, metadata-only changes) become blockers
- Should reference specific incident: 2026-06-03 improve-gradle-plugin-help.md Phase 1 implementation
- Must integrate with existing TESTING.md and WORK_CHECKLIST.md without creating contradictions
- Semver: `[none]` — documentation and context system changes only
- Must use `./gradlew` for all automation
- Must run `./gradlew check` before completion
- All checklist items must result in pushable, non-failing state

## Context: Incident Reference
**Date**: 2026-06-03
**Work card**: `agents.d/work/improve-gradle-plugin-help.md`
**Checklist item**: "Implement Phase 1 for digger-plugin (enhanced task metadata)"
**Agent behavior**: Agent made code changes (added `group` and `description` properties to Gradle task registrations) without writing or checking for tests first
**Agent reasoning** (post-hoc):
- "No existing test suite" (verified after implementation)
- "Metadata-only change" (subjective interpretation)
- "Manual verification via `gradle tasks` validates user-facing behavior"
**User feedback**: Disappointed that agent didn't at least *try* to write tests first, even if that attempt revealed testing wasn't necessary

**What should have happened**:
1. Check for existing test infrastructure
2. Attempt to write test for expected behavior (task metadata appears in Gradle help)
3. Discover whether automated testing is feasible/valuable
4. If genuinely untestable or not worth testing, document why in Implementation Notes
5. Proceed with manual verification as fallback

**What actually happened**:
1. Made code changes directly
2. Verified manually via `gradle tasks` and `gradle help`
3. Rationalized lack of tests after the fact

## Problem Analysis

**Current state of context documents**:
- WORK_CHECKLIST.md line 32: "test → implement → refactor-light → verify pushable"
- WORK_CHECKLIST.md line 102: "Tests failing initially (violates TDD)" in Common Mistakes
- TESTING.md: Contains comprehensive TDD guidance but not referenced proactively enough
- Work card template includes "Agent cycle: test → implement → refactor-light → verify pushable" on every checklist item

**Failure mode**:
- Agent pattern-matched the work as "configuration metadata" and skipped test consideration entirely
- Agent did not attempt to load TESTING.md before starting implementation
- Agent did not question whether "no test directory exists" meant "testing is impossible" vs "I should create tests"
- Work card checklist annotation was present but insufficient to trigger test-first behavior

**Root causes**:
1. **Passive reminders insufficient**: "test → implement" annotation is advisory, not imperative
2. **No pre-implementation checkpoint**: Nothing forces agent to *attempt* testing before declaring it unnecessary
3. **TESTING.md not loaded proactively**: Context system says "load when writing/modifying tests" but agent decides after implementation whether tests are needed
4. **"Common Mistakes" too subtle**: Listing "tests failing initially" as mistake doesn't explicitly say "write tests before any code"
5. **Post-hoc rationalization easy**: Agent can justify skipping tests by citing characteristics of the change rather than demonstrating testing was attempted

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Load TESTING.md and analyze current test-first language strength
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Identify phrases that are advisory vs imperative
  - Document how strongly test-first is emphasized
  - Note where exceptions are discussed
- [x] Analyze WORK_CHECKLIST.md test-first guidance
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Review "test → implement" phrase effectiveness
  - Review "Common Mistakes" section for clarity
  - Identify opportunities for stronger language
- [x] Design strengthened test-first protocol
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Create explicit pre-implementation checkpoint
  - Define what "attempt to write test" means concretely
  - Specify when exceptions are legitimate (with documentation requirement)
  - Ensure TESTING.md is loaded at start of implementation work
- [x] Update WORK_CHECKLIST.md with strengthened protocol
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add pre-implementation test attempt requirement
  - Strengthen "Common Mistakes" language
  - Add concrete example of what "attempt" looks like
  - Reference TESTING.md more explicitly
- [x] Update TESTING.md if needed
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add section on when testing genuinely isn't applicable
  - Require documentation in Implementation Notes when tests skipped
  - Emphasize attempting before concluding
- [x] Update work card template if it exists
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Find work card template or example
  - Strengthen "Agent cycle" annotation language
  - Add reminder to load TESTING.md before implementation
- [x] Test protocol changes against incident scenario
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Review improve-gradle-plugin-help.md Phase 1 implementation
  - Simulate: would new protocol have changed agent behavior?
  - Document whether strengthened language would have triggered test attempt
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move this file to agents.d/work_completed/

## Implementation Notes
(Date-stamped discoveries during execution, newest first)

**2026-06-03**: Playbook compliance review complete. PERSONA.md alignment: line 23 "Tests must fail for correct reason before passing" matches new protocol, line 24 "Verify behavior by running code" enforced by test-first, line 32 "Ship small, reversible steps" supported by one-test-at-a-time, line 36 "Confirm early" satisfied by test validates assumptions, line 52 "Tests demonstrate intent" enforced by fail-with-expected-reason requirement. WORK_CHECKLIST.md changes internally consistent with work card protocol and required reads pattern. TESTING.md changes internally consistent with existing TDD language, exception handling prevents abuse while allowing legitimate cases. No conflicts detected. All changes align with repository values and constraints.

**2026-06-03**: Protocol simulation against incident complete. Old path: agent saw annotation, pattern-matched as "metadata-only", implemented directly, verified manually, rationalized post-hoc. New path triggers at 5 checkpoints: (1) WORK_CHECKLIST line 32-37 explicit BEFORE checkpoint, (2) WORK_CHECKLIST line 89 MANDATORY load TESTING.md, (3) TESTING.md line 12 "even if you think tests aren't needed", (4) TESTING.md lines 21-24 forces attempt, (5) TESTING.md line 29 exception requires documentation BEFORE code changes. Common Mistakes directly names the failure pattern. Result: new protocol would force agent to load TESTING.md, look for/create test infrastructure, write test proving task metadata appears, discover test infrastructure state, document attempt and rationale BEFORE implementing if genuinely untestable. Strengthened protocol would have changed behavior.

**2026-06-03**: No separate work card template file found. Work cards follow WORK_CHECKLIST.md structure which has been strengthened. Existing work cards (improve-gradle-plugin-help.md) show "Agent cycle: test → implement → refactor-light → verify pushable" annotation on every checklist item — this pattern will now be backed by strengthened WORK_CHECKLIST.md and TESTING.md that enforce the checkpoint. Future work cards created following WORK_CHECKLIST.md will inherit strengthened protocol.

**2026-06-03**: TESTING.md updated. Frontmatter line 2: load_when changed to "BEFORE implementing any behavior change" (proactive). When To Use section: added "MANDATORY: BEFORE implementing any behavior change (even if you think tests aren't needed)" as first bullet. TDD Cycle restructured lines 19-29: explicit BEFORE section (load TESTING.md, locate/create test file, write ONE failing test, verify fails with expected reason), Then implement section (simplest implementation, refactor, verify, commit), added exception guidance (document in Implementation Notes what attempted and why before code changes, lists legitimate exceptions). Common Mistakes lines 99-107: reordered with three new first items: "Implementing code before writing test", "Pattern-matching as 'just metadata/configuration' and skipping test attempt", "Not loading TESTING.md before implementation".

**2026-06-03**: WORK_CHECKLIST.md updated. Checklist Execution Order line 32-37: expanded "Feature slices" with explicit BEFORE checkpoint (load TESTING.md, locate/create test file, write ONE failing test, verify fails with expected reason), then implement, refactor-light, verify pushable. Required reads line 89: TESTING.md now "(MANDATORY: load BEFORE implementing any feature slice)" instead of "(test changes)". Common Mistakes expanded: first three items now "Implementing code before writing test (violates TDD — see 2026-06-03 incident)", "Pattern-matching work as 'just metadata/configuration' and skipping test attempt (if behavior changes, test first)", "Not loading TESTING.md before starting implementation (required before any feature slice)".

**2026-06-03**: Strengthened protocol designed. Pre-implementation checkpoint: BEFORE any code changes: (1) Load TESTING.md, (2) locate/create test file, (3) write ONE failing test, (4) verify fails with expected reason, (5) implement minimal code to pass. "Attempt to write test" defined: identify behavior change, locate/create test infrastructure, write test exercising behavior with assertion, run and confirm fails for right reason. Legitimate exceptions (require Implementation Notes documentation): pure docs (README, work cards), build config with no output impact, testing requires disproportionate infrastructure (with justification), test infrastructure changes (still need example proving it works). TESTING.md load trigger change: frontmatter to "BEFORE implementing any behavior change" (proactive not reactive). Common Mistakes strengthening: replace "Tests failing initially" with "Implementing code before writing test", add "Pattern-matching as 'just metadata/configuration' and skipping test attempt", add "Not loading TESTING.md before starting implementation".

**2026-06-03**: WORK_CHECKLIST.md analysis complete. Strength: weakly advisory. Line 32 has "test → implement → refactor-light" as annotation in checklist order, not requirement. Common Mistakes line 103 "Tests failing initially (violates TDD)" is indirect — warns against symptom but doesn't explicitly prevent skipping test writing. No pre-implementation checkpoint. No concrete definition of "attempt to write test". No requirement to document test-skip rationale. TESTING.md referenced line 32 (parenthetical) and line 88 ("Required reads" with trigger "test changes") — both reactive, assumes agent already decided on tests.

**2026-06-03**: TESTING.md analysis complete. Strength: moderately advisory. Advisory: "Red-Green-Refactor is mandatory" (declarative but passive), TDD Cycle describes steps but doesn't enforce attempt-before-exception. Imperative: "Write one test, confirm fail reason" is direct. Gaps: no section on when testing isn't applicable, no requirement to document test-skip rationale in Implementation Notes, no "attempt before concluding" guidance. Load trigger is reactive ("adding new behavior" not "before considering implementation"). Common Mistakes identifies "Feature first, tests second" but doesn't prevent it.

**2026-06-03**: Work card reviewed for template compliance — all required sections present and properly structured. Proceeding with analysis phase.

## Current State
- Last commit SHA: 2b5a9b0
- Uncommitted changes: Modified DiggerPlugin.kt with task metadata
- Active session: 2026-06-03
- Blockers: None

## Validation
- Commands:
  - `./gradlew check`
- Results: BUILD SUCCESSFUL in 3s, 303 actionable tasks (23 executed, 11 from cache, 269 up-to-date)

## Success Criteria
- Agent's default behavior when told to implement a feature is to write an automated test that proves the feature does not work yet.
- Future agents implementing work card checklist items load TESTING.md proactively
- Future agents attempt to write tests before concluding tests aren't needed
- When tests are skipped, Implementation Notes document what was attempted and why testing wasn't applicable
- Incident scenario (Gradle plugin metadata changes) would trigger different behavior under new protocol
