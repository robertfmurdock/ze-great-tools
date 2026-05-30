# CLI Documentation UX Improvements

## Goal
Fix counterintuitive CLI documentation patterns and usage examples to make tools more intuitive and consistent with standard CLI conventions.

## Constraints
- Maintain backward compatibility (especially `--file=''` pattern must still work)
- Follow TestMints patterns for test structure
- Each checklist item must result in a pushable state
- Use semver annotations aligned with WORK_CHECKLIST: `[none]` for docs/work-card-only changes, `[patch]` for code changes (including runtime warning behavior), `[minor]` for new optional behaviors

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] Fix documentation-only issues (Digger naming, boolean syntax, required flags, format behavior)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Improve empty string flag pattern in Tagger `--file` option
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add deprecation warning for `--disable-detached` flag
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

### Identified Issues

**Issue 1: Empty String Flag Pattern (Tagger CLI - `--file=''`)**
- Files: `command-line-tools/tagger-cli/README.md` (lines 62, 74), `GenerateSettingsFile.kt` (line 24)
- Pattern: `tagger generate-settings-file --file=''`
- Problem: Empty strings typically mean "no value"; this is backwards from CLI norms
- Current logic: `file.orEmpty().ifBlank { ".tagger" }`
- Solution: Support bare `--file` to mean "use default file"; keep explicit `--file=<path>` behavior and maintain `--file=''` backward compatibility

**Issue 2: Inconsistent Command Naming (Digger CLI)**
- File: `command-line-tools/digger-cli/README.md`
- Problem: Line 87 shows `digger currentContributionData` (camelCase - wrong), other lines show correct kebab-case
- Solution: Replace all camelCase command references with `current-contribution-data`, `all-contribution-data`

**Issue 3: Boolean Flag Syntax (Tagger CLI - `--merge`)**
- Files: `tagger-cli/README.md` (line 74), `GenerateSettingsFile.kt` (lines 26-27)
- Pattern: Only shows `--merge=true`
- Problem: Doesn't show `--merge` alone works (Clikt's `.boolean()` supports both)
- Solution: Update examples and help text to show both forms

**Issue 4: Unclear Optional vs Required (Tagger CLI - `--release-branch`)**
- File: `Tag.kt` (lines 30-31)
- Problem: Marked `.required()` but can be satisfied by `.tagger` config file
- Solution: Update help text to clarify config file relationship

**Issue 5: Deprecated Inverted Flag (Tagger CLI - `--disable-detached`)**
- File: `CalculateVersion.kt` (lines 40, 46)
- Problem: Hidden deprecated flag with inverted logic, no warning when used
- Solution: Add runtime deprecation warning, document migration path

**Issue 6: Format Flag Changes Output Destination (Digger CLI)**
- File: `digger-cli/README.md` (lines 106-121)
- Problem: `--format=text` writes to file, `--format=json` writes to stdout - not obvious
- Solution: Make this explicit in documentation

### Work Log

- 2026-05-30: Reviewed work card structure against `WORK_CHECKLIST.md`; template and checklist ordering already conformed.
- 2026-05-30: Implemented bare `--file` support in `GenerateSettingsFile` via Clikt `optionalValue(".tagger")` while preserving legacy `--file=''` behavior.
- 2026-05-30: Added CLI test coverage for `tagger generate-settings-file --file` and updated README examples to prefer bare `--file`.
- 2026-05-30: Validation note: one failed `./gradlew check` run was caused by two Gradle builds running in parallel and colliding on test result files; reran sequentially and passed.

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests com.zegreatrob.tools.tagger.cli.GenerateSettingsFileCommandTest --console=plain`
  - `./gradlew check --console=plain`
- Results:
  - First targeted jvmTest run failed due a regression in merge behavior; fixed by keeping default `.tagger` resolution when `--file` is omitted.
  - Targeted jvmTest rerun passed.
  - One `./gradlew check` run failed due parallel Gradle invocation collisions (`NoSuchFileException` / `EOFException` in test result binaries); sequential rerun passed.
