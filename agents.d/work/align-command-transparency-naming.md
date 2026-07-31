# Align Command Transparency Naming and Make Gradle Feature Explicit

## Goal
Rename test parameter from `transparency` to `showCommands` and add explicit `showCommands` property to Gradle plugins to make command transparency discoverable and consistently named across CLI and Gradle implementations.

## Constraints
- Semver intent: `[minor]` for Gradle DSL property addition, `[patch]` for changing default logging behavior, `[none]` for test refactor
- CLI already uses `--show-commands` flag and defaults to false (opt-in)
- Both CLI and Gradle should default to `showCommands = false` (explicit opt-in)
- Current Gradle behavior (always logging via lifecycle) is accidental, not intentional
- Follow TestMints patterns from TESTING.md
- Universal language: `showCommands` across all implementations

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Add showCommands property to tagger-plugin
  - Add `showCommands: Property<Boolean>` to TaggerExtension
  - Default to false
  - Modify command logging to check property before logging
  - Test: verify commands not logged when property false (default)
  - Test: verify commands logged when property true
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add showCommands property to digger-plugin
  - Add `showCommands: Property<Boolean>` to DiggerExtension
  - Default to false
  - Modify command logging to check property before logging
  - Test: verify commands not logged when property false (default)
  - Test: verify commands logged when property true
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Rename transparency → showCommands in test specs
  - Rename parameter in CalculateVersionTestSpec.configureWithOverrides
  - Rename parameter in SetupWithOverrides.setupWithOverrides
  - Update test names: withTransparencyEnabled → withShowCommandsEnabled
  - Update test names: withTransparencyDisabled → withShowCommandsDisabled
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update all test implementations for renamed parameter
  - Rename transparency → showCommands in all CLI test implementations
  - Rename enableTransparency → showCommands in all Gradle plugin test implementations
  - Update to use new Gradle DSL property instead of -q flag manipulation
  - Verify all tests pass
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: a4ea7c50
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Ready to start
- **Date**: 2026-07-31

## Implementation Notes
_(newest first)_

### 2026-07-31: Work card created
Context: After completing hoist-show-commands-tests-to-specs, identified friction in naming.
Current state uses `transparency` as test parameter, but:
- CLI flag is named `--show-commands`
- Gradle plugin has no explicit property (accidentally logs via lifecycle level)
- No discoverable feature on Gradle side

Decision: Use universal language `showCommands` and make it explicit in both platforms.
Both should default to false (opt-in) for consistency and intentionality.

Previous work: hoist-show-commands-tests-to-specs (completed in a4ea7c50)

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] Verify Gradle plugins have showCommands property with false default
- [ ] Verify commands not logged by default in Gradle plugins
- [ ] Verify commands logged when showCommands = true in Gradle plugins
- [ ] Verify all test parameters renamed from transparency to showCommands
- [ ] Verify test names updated to reflect showCommands terminology
- [ ] Verify no references to "transparency" remain in test code
