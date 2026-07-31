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
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Add showCommands property to tagger-plugin
  - Add `showCommands: Property<Boolean>` to TaggerExtension
  - Default to false
  - Modify command logging to check property before logging
  - Test: verify commands not logged when property false (default)
  - Test: verify commands logged when property true
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Add showCommands property to digger-plugin
  - Add `showCommands: Property<Boolean>` to DiggerExtension
  - Default to false
  - Modify command logging to check property before logging
  - Test: verify commands not logged when property false (default)
  - Test: verify commands logged when property true
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Rename transparency → showCommands in test specs
  - Rename parameter in CalculateVersionTestSpec.configureWithOverrides
  - Rename parameter in SetupWithOverrides.setupWithOverrides
  - Update test names: withTransparencyEnabled → withShowCommandsEnabled
  - Update test names: withTransparencyDisabled → withShowCommandsDisabled
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Update all test implementations for renamed parameter
  - Rename transparency → showCommands in all CLI test implementations
  - Rename enableTransparency → showCommands in all Gradle plugin test implementations
  - Update to use new Gradle DSL property instead of -q flag manipulation
  - Verify all tests pass
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: 0fad5c9b
- **Uncommitted work**: Work card updates only
- **Blockers**: None
- **Status**: Implementation complete, awaiting refactor pass
- **Date**: 2026-07-31

## Implementation Notes
_(newest first)_

### 2026-07-31: Phases 1-4 complete
All implementation phases completed:
- Phase 1 (e3359673): Added showCommands property to TaggerExtension with false default. Modified all tagger tasks to conditionally log based on showCommands.
- Phase 2 (8be51bbb): Added showCommands property to DiggerExtension with false default. Modified digger tasks to set logger based on showCommands.
- Phase 3 (ce04ab4b): Renamed transparency → showCommands in all test specs and test method names.
- Phase 4 (0fad5c9b): Updated all test implementations (CLI and plugin functional tests) to use showCommands parameter.

All tests pass. Ready for mandatory refactor pass.

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
- [x] `./gradlew check -q --console=plain` - all checks pass
- [x] Verify Gradle plugins have showCommands property with false default
- [x] Verify commands not logged by default in Gradle plugins
- [x] Verify commands logged when showCommands = true in Gradle plugins
- [x] Verify all test parameters renamed from transparency to showCommands
- [x] Verify test names updated to reflect showCommands terminology
- [x] Verify no references to "transparency" remain in test code
