# Enum-Based Help Content Testing

## Goal
Ensure enum-based help content in digger and tagger CLIs is exhaustively tested such that adding a new enum element causes test failures, forcing documentation updates.

## Constraints
- Must follow TestMints pattern (setup/exercise/verify) per `.junie/guidelines.md`
- Tests must be Gradle-integrated (no standalone shell scripts) per project preference
- Declaration of semver intent: `[patch]` — enhances test coverage for existing help features without changing public API
- Applies to:
  - `digger-cli`: `OutputFormat`, `SemverType` (if documented in help)
  - `tagger-cli`: `OutputFormat`, `SnapshotReason`, `ChangeType` (if documented in help)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Audit digger and tagger help/guide content to identify all enum-based documentation
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Identify which enums appear in help text and how
  - Document findings in Implementation Notes
- [x] Add exhaustive tests for SnapshotReason documentation in tagger calculate-version help
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test must verify every enum element is documented
  - Test must fail if a new element is added without corresponding documentation
- [x] Add exhaustive tests for OutputFormat documentation if present in help text
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Cover both digger and tagger if applicable
- [x] Add exhaustive tests for ChangeType documentation if present in help text
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Add exhaustive tests for SemverType documentation if present in help text
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move this file to agents.d/work_completed/

## Implementation Notes
[Semver intent (initial): [patch] — strengthens existing test coverage for help content, no API changes]

### Enum Inventory
- **digger-cli**:
  - `OutputFormat` (TEXT, JSON) — used in multiple commands
  - `SemverType` (None, Patch, Minor, Major) — core domain model
  
- **tagger-cli**:
  - `OutputFormat` (TEXT, JSON) — used in calculate-version
  - `SnapshotReason` (FORCED, DIRTY, AHEAD, BEHIND, NOT_RELEASE_BRANCH, NO_NEW_VERSION) — documented in calculate-version help table
  - `ChangeType` (Major, Minor, Patch, None) — core domain model

### Testing Strategy
- Follow existing `GuideTest` and `CalculateVersionCommandTest` patterns
- Use enum `.entries` to iterate over all values
- Assert each enum element appears in help output with appropriate context
- Tests will fail compilation or at runtime if enum changes but test doesn't

### 2026-05-31 - Help/Guide Audit Findings
- `tagger calculate-version --help` includes a Snapshot Reasons table listing every `SnapshotReason` by enum name.
- `OutputFormat` appears in help for:
  - `tagger calculate-version --help` (`--format`, default `text`)
  - `tagger tag --help` (`--format`, default `text`)
  - `digger current-contribution-data --help` (`--format`, text/json behavior)
  - `digger all-contribution-data --help` (`--format`, text/json behavior)
- `ChangeType` enum names are not directly documented in CLI help content.
- `SemverType` values are documented in `digger current-contribution-data --help` in lowercase (`major`, `minor`, `patch`, `none`).

### 2026-05-31 - Progress Notes
- Subagent authorization prompt sent in-thread: `Authorize subagent delegation for this card? (yes/no)`.
- User response on 2026-05-31: `yes` (subagent delegation authorized for this card).
- Added `helpTextDocumentsEverySnapshotReasonEnum` in `CalculateVersionCommandTest` to assert `SnapshotReason.entries` are all present in `calculate-version --help`.
- Initial red step failed because markdown backticks are not preserved by rendered help output; updated assertion to match enum names directly in help text.
- Added exhaustive `OutputFormat` help coverage:
  - `helpTextDocumentsEveryOutputFormatEnumForCalculateVersion`
  - `helpTextDocumentsEveryOutputFormatEnumForTag`
  - `helpTextDocumentsEveryOutputFormatEnumForCurrentContributionData`
  - `helpTextDocumentsEveryOutputFormatEnumForAllContributionData`
- Added `helpTextDocumentsEverySemverTypeEnum` in `CurrentContributionDataTest` to assert all `SemverType.entries` appear in help output.
- `ChangeType` is not documented in CLI help/guide content; per checklist condition ("if present"), no new ChangeType help test was added.
- Mutation confirmation:
  - Temporarily adding a new `SnapshotReason` enum value caused `helpTextDocumentsEverySnapshotReasonEnum` to fail.
  - Temporarily adding a new `SemverType` enum value caused `helpTextDocumentsEverySemverTypeEnum` to fail.
  - Temporarily adding a new tagger `OutputFormat` enum value failed compilation because `when` expressions are exhaustive; this blocks drift before tests run.
- Playbook compliance review (2026-05-31):
  - `PLAYBOOK_CODE_STYLE.md`: tests remain in setup/exercise/verify style and use existing assertion patterns.
  - `.junie/guidelines.md`: Gradle-integrated validation completed, including full `./gradlew check`.
- Final refactor pass via subagent (2026-05-31):
  - Subagent completed mandatory `REFACTOR_AGENT.md` checklist and reviewed all 5 in-scope files.
  - Minor fix applied: replaced `error(...)` with `fail(...)` in `TagCommandTest` verify path to keep verify assertions test-native.
  - Subagent re-ran cross-module validation: `./gradlew check --console=plain` passed.

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest --tests com.zegreatrob.tools.tagger.cli.TagCommandTest --console=plain`
  - `./gradlew :command-line-tools:digger-cli:jvmTest --tests com.zegreatrob.tools.digger.cli.CurrentContributionDataTest --tests com.zegreatrob.tools.digger.cli.AllContributionDataTest --console=plain`
  - `./gradlew :command-line-tools:digger-cli:jvmTest --tests com.zegreatrob.tools.digger.cli.CurrentContributionDataTest --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.helpTextDocumentsEverySnapshotReasonEnum --console=plain` (with temporary added `SnapshotReason`)
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.helpTextDocumentsEveryOutputFormatEnumForCalculateVersion --tests com.zegreatrob.tools.tagger.cli.TagCommandTest.helpTextDocumentsEveryOutputFormatEnumForTag --console=plain` (with temporary added tagger `OutputFormat`)
  - `./gradlew :command-line-tools:digger-cli:jvmTest --tests com.zegreatrob.tools.digger.cli.CurrentContributionDataTest.helpTextDocumentsEverySemverTypeEnum --console=plain` (with temporary added `SemverType`)
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.helpTextDocumentsEverySnapshotReasonEnum --tests com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.helpTextDocumentsEveryOutputFormatEnumForCalculateVersion --tests com.zegreatrob.tools.tagger.cli.TagCommandTest.helpTextDocumentsEveryOutputFormatEnumForTag --console=plain` (after revert)
  - `./gradlew :command-line-tools:digger-cli:jvmTest --tests com.zegreatrob.tools.digger.cli.CurrentContributionDataTest.helpTextDocumentsEveryOutputFormatEnumForCurrentContributionData --tests com.zegreatrob.tools.digger.cli.AllContributionDataTest.helpTextDocumentsEveryOutputFormatEnumForAllContributionData --tests com.zegreatrob.tools.digger.cli.CurrentContributionDataTest.helpTextDocumentsEverySemverTypeEnum --console=plain` (after revert)
  - `./gradlew check --console=plain`
- Results:
  - First run failed at task selection (`commonTest` task does not exist for module).
  - First `jvmTest` run failed in new test as expected (string match was too strict on markdown formatting).
  - Final `jvmTest` run passed for `CalculateVersionCommandTest`.
  - Targeted tagger tests (`CalculateVersionCommandTest`, `TagCommandTest`) passed.
  - Targeted digger tests (`CurrentContributionDataTest`, `AllContributionDataTest`) passed.
  - Final focused rerun of `CurrentContributionDataTest` passed with SemverType exhaustive coverage.
  - With temporary added `SnapshotReason`, the snapshot exhaustive help test failed as expected.
  - With temporary added `SemverType`, the semver exhaustive help test failed as expected.
  - With temporary added tagger `OutputFormat`, compilation failed on exhaustive `when` expressions before tests, preventing enum drift.
  - After reverting mutation edits, all targeted exhaustive tests passed again.
  - Full repository validation via `./gradlew check` passed.
