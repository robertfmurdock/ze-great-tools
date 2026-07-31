# Add Command Transparency Tests to Test Specs

## Goal
Add command transparency verification to test specs to ensure git commands are logged when transparency is enabled across CLI implementations.

## Constraints
- Test specs already capture output via TestResult (stdout/stderr/output) - use existing abstraction
- CLI implementations support transparency via --show-commands flag
- Gradle plugin tests are separate functional tests (not spec-based)
- No new test API concepts needed - just check existing output for git commands
- Semver intent: `[none]` - test coverage improvement, no behavior changes
- Follow TestMints patterns from TESTING.md

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Add transparency tests to CalculateVersionTestSpec
  - Test: execute with transparency enabled, verify stderr contains "git"
  - Test: execute with transparency disabled, verify stderr does not contain "git"
  - Use existing TestResult abstraction - no new API
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update tagger-cli test implementation for transparency tests
  - configureWithDefaults/configureWithOverrides: add transparency parameter
  - Pass transparency setting through to CLI execution
  - Remove existing --show-commands tests from CalculateVersionCommandTest
  - Verify spec tests now provide coverage
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add transparency tests to CurrentContributionTestSpec
  - Test: execute with transparency enabled, verify stderr contains "git"
  - Test: execute with transparency disabled, verify stderr does not contain "git"
  - Use existing TestResult abstraction
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update digger-cli test implementation for transparency tests
  - setupWithDefaults/setupWithOverrides: add transparency parameter
  - Pass transparency setting through to CLI execution
  - Remove existing --show-commands tests from CurrentContributionDataTest
  - Verify spec tests now provide coverage
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add transparency tests to AllContributionTestSpec
  - Test: execute with transparency enabled, verify stderr contains "git"
  - Test: execute with transparency disabled, verify stderr does not contain "git"
  - Use existing TestResult abstraction
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add Gradle plugin functional tests for command transparency
  - Create functional test for calculateVersion task
  - Test: run with default logging, verify git commands in output (logger.lifecycle)
  - Test: run with --quiet, verify git commands suppressed
  - Separate from spec tests - Gradle plugin uses functional test pattern
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

### 2026-07-31: Final approach - use existing test infrastructure
After two revisions, arrived at the correct abstraction: specs already capture output
via TestResult. No new API needed.

**Key insight**: Test specs don't need to know about "command logs" as a special concept.
They just check the output that implementations already provide (stdout/stderr/output).

**Solution**: Add tests that check output contains "git" when transparency enabled
- Specs: write tests that check `result.stderr.contains("git")`
- CLI implementations: extend configure methods to accept transparency flag
- Gradle plugin: separate functional tests (different test pattern, not spec-based)

**What was wrong with previous approaches**:
1. First attempt: Created CLI-specific API in specs (`executeWithCommandTransparency`)
2. Second attempt: Added optional capability pattern (`supportsCommandTransparency`, `captureCommandLogs`)
3. Both polluted specs with transparency-specific concepts

**Correct approach**: Transparency is just another configuration option that affects output.
Use existing configure methods, check existing output fields. No new abstractions.

Key files:
- `/tools-tests/tagger-test/src/commonMain/kotlin/com/zegreatrob/tools/tagger/CalculateVersionTestSpec.kt`
- `/tools-tests/digger-test/src/commonMain/kotlin/com/zegreatrob/tools/digger/CurrentContributionTestSpec.kt`
- `/tools-tests/digger-test/src/commonMain/kotlin/com/zegreatrob/tools/digger/AllContributionTestSpec.kt`
- `/command-line-tools/tagger-cli/src/commonTest/kotlin/com/zegreatrob/tools/tagger/cli/CalculateVersionCommandTest.kt`
- `/command-line-tools/digger-cli/src/commonTest/kotlin/com/zegreatrob/tools/digger/cli/CurrentContributionDataTest.kt`
- `/tools-tests/tagger-plugin-test/src/functionalTest/kotlin/com/zegreatrob/tools/tagger/` (new coverage)

### 2026-07-31: Work card created
Context: --show-commands feature was implemented with CLI-level tests. User questioned
whether tests should be hoisted to specs for feature parity.

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] Verify CLI tests removed from implementation classes
- [ ] Verify spec-level tests exist and pass for both CLI implementations
- [ ] Verify test count unchanged (no coverage lost)
- [ ] Verify digger test specs include transparency tests
- [ ] Verify TESTING.md documents the pattern
