# Hoist --show-commands Tests to Test Specs

## Goal
Move --show-commands tests from CLI implementation classes to test specs to ensure feature parity across all implementations.

## Constraints
- Must maintain existing test coverage (no tests lost)
- Spec-level tests must be implementation-agnostic
- Different implementations use different mechanisms (CLI flags vs properties vs automatic)
- Logging output verification strategy must abstract across stderr/Gradle logger/other
- Semver intent: `[none]` - test refactoring, no behavior changes
- Follow TestMints patterns and test hierarchy guidelines from TESTING.md

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Add command transparency verification methods to CalculateVersionTestSpec
  - `fun executeWithCommandTransparency(): TestResultWithCommands` - executes with transparency enabled
  - `fun TestResultWithCommands.assertCommandsLogged()` - verifies git commands were logged
  - `fun TestResultWithCommands.assertCommandsNotLogged()` - verifies no git commands in output
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Implement CLI test adapter methods for command transparency
  - tagger-cli: use `--show-commands` flag, verify stderr contains git commands
  - digger-cli: use `--show-commands` flag, verify stderr contains git commands
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add spec-level tests for command transparency
  - Test: transparency enabled shows git commands
  - Test: transparency disabled (default) shows no git commands
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Remove implementation-specific --show-commands tests
  - Remove from CalculateVersionCommandTest (tagger-cli)
  - Remove from CurrentContributionDataTest (digger-cli)
  - Verify tests still pass (spec tests provide coverage)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add command transparency verification to digger test specs
  - CurrentContributionTestSpec: add transparency methods
  - AllContributionTestSpec: add transparency methods
  - Implement in digger-cli test adapters
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Document test abstraction pattern in TESTING.md
  - Add section on logging/output verification abstraction
  - Reference --show-commands tests as example
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: (to be filled on start)
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Ready to start
- **Date**: 2026-07-31

## Implementation Notes
_(newest first)_

### 2026-07-31: Work card created
Context: --show-commands feature was implemented with CLI-level tests. User requested
hoisting to test specs to ensure feature parity across implementations.

Challenge: Different implementations enable transparency differently:
- CLI tools: `--show-commands` flag → stderr logging
- Gradle plugin tasks: automatic via `logger.lifecycle()` → Gradle logger
- Gradle plugin extension: no logging (programmatic API)

Solution approach:
- Add spec-level methods for transparency verification
- Implementations provide adapters that:
  1. Enable transparency for their context (flag, property, etc.)
  2. Extract logged commands from appropriate output stream
  3. Verify presence/absence of git commands

Similar to existing form-factor abstraction pattern where spec defines WHAT to verify,
implementations define HOW to verify for their context.

Key files:
- `/tools-tests/tagger-test/src/commonMain/kotlin/com/zegreatrob/tools/tagger/CalculateVersionTestSpec.kt`
- `/tools-tests/digger-test/src/commonMain/kotlin/com/zegreatrob/tools/digger/CurrentContributionTestSpec.kt`
- `/tools-tests/digger-test/src/commonMain/kotlin/com/zegreatrob/tools/digger/AllContributionTestSpec.kt`
- `/command-line-tools/tagger-cli/src/commonTest/kotlin/com/zegreatrob/tools/tagger/cli/CalculateVersionCommandTest.kt`
- `/command-line-tools/digger-cli/src/commonTest/kotlin/com/zegreatrob/tools/digger/cli/CurrentContributionDataTest.kt`

Reference: Existing deprecation warning tests use `assertHasDeprecationWarning()` pattern
where spec defines assertion, implementations handle kebab-case vs camelCase formatting.

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] Verify CLI tests removed from implementation classes
- [ ] Verify spec-level tests exist and pass for both CLI implementations
- [ ] Verify test count unchanged (no coverage lost)
- [ ] Verify digger test specs include transparency tests
- [ ] Verify TESTING.md documents the pattern
