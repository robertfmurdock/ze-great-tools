# Digger CLI README Duplication Tests

## Goal
Establish README testing for digger-cli to prevent documentation drift, following the pattern proven in tagger-cli.

## Constraints
- Must follow PERSONA values: clarity over cleverness, tests demonstrate intent
- Must use TestMints patterns per `.junie/guidelines.md`
- Must use jvmTest source set (file I/O requirement)
- This is observation-level testing: verify documentation choices, don't enforce arbitrary rules
- Pattern must match tagger-cli ReadmeTest structure for consistency
- Tests may initially fail on current README state - this is intentional documentation of baseline

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Create jvmTest directory and ReadmeTest.kt with basic tests
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tests: readmeExistsAndIsReadable, readmeReferencesMainHelp
  - Helper: readReadme() with path resolution
- [x] Add field documentation duplication detection
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test: readmeDoesNotDuplicateFieldDocumentation
  - Helper: containsFieldDocumentation() with regex pattern
- [x] Add error code and SemverType duplication detection
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tests: readmeDoesNotDuplicateErrorCodeDocumentation, readmeDoesNotDuplicateSemverTypeDocumentation
  - Helpers: containsErrorCodeDocumentation(), containsSemverTypeDocumentation()
- [ ] Add subcommand help reference tests
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tests: readmeReferencesCurrentContributionDataHelp, readmeReferencesAllContributionDataHelp
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

### Context
Recent tagger-cli work (TAGGER_CLI_AGENT_DISCOVERABILITY.md, commits c815658, 7d35b78, 32e3109) established README testing as codebase best practice. Digger-cli is a sibling tool with similar structure but no README tests.

### Reference Pattern
`/Users/robertfmurdock/git/ze-great-tools/command-line-tools/tagger-cli/src/jvmTest/kotlin/com/zegreatrob/tools/tagger/cli/ReadmeTest.kt`

### Digger-Specific Adaptations
- Main command: `digger --help` (vs `tagger --help`)
- Subcommands: `current-contribution-data`, `all-contribution-data` (vs `calculate-version`)
- Domain enum: `SemverType` at `/Users/robertfmurdock/git/ze-great-tools/tools/digger-core/src/commonMain/kotlin/com/zegreatrob/tools/digger/core/SemverType.kt` (vs `SnapshotReason`)
- README path: `command-line-tools/digger-cli/README.md`

### Expected Test Behavior
**Should pass initially:**
- readmeExistsAndIsReadable (README exists at line 260)
- readmeReferencesMainHelp (README contains `digger --help` at line 260)
- readmeDoesNotDuplicateErrorCodeDocumentation (no error codes documented)
- readmeDoesNotDuplicateSemverTypeDocumentation (SemverType only in JSON examples)

**May fail initially (documents baseline):**
- readmeDoesNotDuplicateFieldDocumentation (README lines 161-168 document fields)
- readmeReferencesCurrentContributionDataHelp (may not reference subcommand help)
- readmeReferencesAllContributionDataHelp (may not reference subcommand help)

Test failures document current state and establish guardrails. Follow-up work can address by updating README.

### Key Files
- Create: `command-line-tools/digger-cli/src/jvmTest/kotlin/com/zegreatrob/tools/digger/cli/ReadmeTest.kt`
- Test subject: `command-line-tools/digger-cli/README.md`
- Domain enum: `tools/digger-core/src/commonMain/kotlin/com/zegreatrob/tools/digger/core/SemverType.kt`

## Validation
- Commands:
  - Phase 1: `./gradlew :command-line-tools:digger-cli:jvmTest --tests ReadmeTest`
  - Phase 2: `./gradlew :command-line-tools:digger-cli:jvmTest --tests ReadmeTest`
  - Phase 3: `./gradlew :command-line-tools:digger-cli:jvmTest --tests ReadmeTest`
- Results:
  - Phase 1: 2 tests, 2 passing ✓
  - Phase 2: 3 tests, 2 passing, 1 failing (field documentation detected as expected)
  - Phase 3: 5 tests, 4 passing, 1 failing (error code & SemverType tests pass as expected)
