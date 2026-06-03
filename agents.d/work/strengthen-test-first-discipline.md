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
- [ ] Review this work card for compliance with template and update to conform
- [ ] Load TESTING.md and analyze current test-first language strength
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Identify phrases that are advisory vs imperative
  - Document how strongly test-first is emphasized
  - Note where exceptions are discussed
- [ ] Analyze WORK_CHECKLIST.md test-first guidance
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Review "test → implement" phrase effectiveness
  - Review "Common Mistakes" section for clarity
  - Identify opportunities for stronger language
- [ ] Design strengthened test-first protocol
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Create explicit pre-implementation checkpoint
  - Define what "attempt to write test" means concretely
  - Specify when exceptions are legitimate (with documentation requirement)
  - Ensure TESTING.md is loaded at start of implementation work
- [ ] Update WORK_CHECKLIST.md with strengthened protocol
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add pre-implementation test attempt requirement
  - Strengthen "Common Mistakes" language
  - Add concrete example of what "attempt" looks like
  - Reference TESTING.md more explicitly
- [ ] Update TESTING.md if needed
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add section on when testing genuinely isn't applicable
  - Require documentation in Implementation Notes when tests skipped
  - Emphasize attempting before concluding
- [ ] Update work card template if it exists
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Find work card template or example
  - Strengthen "Agent cycle" annotation language
  - Add reminder to load TESTING.md before implementation
- [ ] Test protocol changes against incident scenario
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Review improve-gradle-plugin-help.md Phase 1 implementation
  - Simulate: would new protocol have changed agent behavior?
  - Document whether strengthened language would have triggered test attempt
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
(Date-stamped discoveries during execution, newest first)

## Current State
- Last commit SHA: 2b5a9b0
- Uncommitted changes: Modified DiggerPlugin.kt with task metadata
- Active session: 2026-06-03
- Blockers: None

## Validation
- Commands:
  - `./gradlew check`
- Results: (to be filled in during implementation)

## Success Criteria
- Agent's default behavior when told to implement a feature is to write an automated test that proves the feature does not work yet.
- Future agents implementing work card checklist items load TESTING.md proactively
- Future agents attempt to write tests before concluding tests aren't needed
- When tests are skipped, Implementation Notes document what was attempted and why testing wasn't applicable
- Incident scenario (Gradle plugin metadata changes) would trigger different behavior under new protocol
