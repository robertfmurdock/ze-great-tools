# Improve Tagger CLI JSON Configuration Documentation

## Goal
Make the `.tagger` JSON configuration file capability discoverable through CLI help text and README documentation.

## Constraints
- Must follow TestMints pattern for any new tests
- CLI help text must remain concise (existing style is brief and practical)
- README must remain scannable (don't add walls of text)
- Changes must preserve existing help text format and structure
- Follow "default to no comments" guideline in code changes
- Use regex patterns for help text assertions (per `.junie/guidelines.md` rule about CLI output testing)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Add configuration section to main Tagger help text
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Enhance generate-settings-file command help text with descriptions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Add configuration file section to README with examples
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Add config file mention to calculate-version and tag command help
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
  - calculate-version: DONE
  - tag: DONE
- [x] Final refactor pass (code style, patterns, efficiency)
- [x] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
Initial analysis shows:
- Tagger CLI supports `.tagger` JSON config file (fully implemented but undocumented)
- Config file feature is NOT mentioned in help text or README
- Users must discover feature through code reading or trial
- Digger CLI does NOT support config files (only uses --output-file for output)
- generate-settings-file command exists but has minimal help text

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests "*TaggerTest*"`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests "*GenerateSettingsFileCommandTest*"`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests "*CalculateVersionCommandTest.helpTextMentionsConfigFile"`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests "*TagCommandTest.helpTextMentionsConfigFile"`
  - `./gradlew :command-line-tools:tagger-cli:check`
  - `./gradlew check`
- Results: All tests pass, all linting passes, no cross-module issues detected
