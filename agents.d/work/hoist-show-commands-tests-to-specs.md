# Add Command Transparency Tests to Test Specs

## Goal
Add command transparency verification to test specs to ensure git commands are logged correctly across all implementations that support transparency.

## Constraints
- Behavior invariant: implementations that support command transparency must log git commands before execution
- Different transparency mechanisms: CLI flags, automatic Gradle logging, or N/A for programmatic APIs
- Output destinations vary: stderr (CLI), Gradle logger (plugin tasks), or none (extension)
- Default implementations: specs provide no-op defaults, supporting implementations override
- Must verify both transparency-enabled and default (no logging) cases
- Semver intent: `[none]` - test coverage improvement, no behavior changes
- Follow TestMints patterns and optional feature testing from TESTING.md

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Add optional transparency hooks to CalculateVersionTestSpec
  - `fun supportsCommandTransparency(): Boolean = false` - implementations override to true if they support it
  - `fun captureCommandLogs(block: () -> TestResult): List<String>` - default returns emptyList, implementations capture logs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add spec-level command transparency tests to CalculateVersionTestSpec
  - Test: when supportsCommandTransparency, verify git commands logged during execution
  - Test: when supportsCommandTransparency, verify default execution produces no command logs
  - Tests skip if !supportsCommandTransparency (no-op for implementations without transparency)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Implement transparency hooks in tagger-cli test adapter
  - Override supportsCommandTransparency() = true
  - Implement captureCommandLogs: run with/without --show-commands, extract stderr
  - Remove existing --show-commands tests from CalculateVersionCommandTest
  - Verify spec tests now provide coverage
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add Gradle plugin transparency tests (new coverage)
  - Create functional test for calculateVersion task with --info flag
  - Verify git commands appear in Gradle output via logger.lifecycle
  - Verify --quiet suppresses command logging
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add optional transparency hooks to digger test specs
  - CurrentContributionTestSpec: add supportsCommandTransparency and captureCommandLogs
  - AllContributionTestSpec: add supportsCommandTransparency and captureCommandLogs
  - Add spec-level transparency tests (skip if not supported)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Implement transparency hooks in digger-cli test adapters
  - Override supportsCommandTransparency() = true in both specs
  - Implement captureCommandLogs: run with/without --show-commands, extract stderr
  - Remove existing --show-commands tests from CurrentContributionDataTest
  - Verify spec tests now provide coverage
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

### 2026-07-31: Revised approach - optional capability testing
Initial approach tried to force API into specs for CLI-specific concerns. Revised after
feedback: command transparency is a behavior that SHOULD work across implementations,
not just a CLI interface detail.

**Behavior invariant**: Implementations that support command transparency must log git
commands before execution.

**What varies**:
- HOW transparency is enabled: CLI flag vs automatic (Gradle tasks) vs N/A (extension)
- WHERE output goes: stderr vs Gradle logger vs none

**Solution**: Optional capability pattern with default no-ops
- Specs provide default implementations returning false/empty
- Supporting implementations override to true and provide capture logic
- Tests skip when transparency not supported (no test pollution for extensions)
- Gradle plugin tasks get NEW functional test coverage (currently untested!)

**Benefits**:
- No API pollution: defaults are no-ops, non-supporting implementations don't see the methods
- Tests feature parity: CLI and Gradle tasks both verify logging works
- Discovers missing coverage: Gradle plugin logging currently has no tests
- Follows existing optional pattern: similar to deprecation warnings, form-factor abstraction

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
