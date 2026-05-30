# Tagger CLI Agent Discoverability Improvements

## Goal
Make tagger's snapshot suffix semantics and intended workflow more discoverable for AI agents through improved CLI messaging, without duplicating help content across the repository.

## Constraints
- Avoid duplicating help content in multiple places (follow DRY principle for documentation)
- Changes must be backward-compatible (existing scripts and automation must continue working)
- Follow PERSONA values: clarity over cleverness, show your work
- CLI is the source of truth for usage guidance - README should reference CLI help, not duplicate it
- Text output format must remain stable for existing users
- JSON format already correct - focus on guiding agents to discover and use it

## Identified Issues
AI agents using tagger via npm are:
1. Treating `-SNAPSHOT` suffix as decorative text to strip rather than as a condition indicator
2. Not discovering or using `--format=json` for structured automation data
3. Seeing snapshot reasons on stderr but not understanding they describe required actions
4. Not understanding the relationship between snapshot status and tagging workflow

Current CLI outputs explain *what* happens but don't clearly communicate *why* or *what to do*.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Enhance CLI help text for agent discoverability
- [x] Improve snapshot diagnostic output with actionable guidance
- [x] Enhance context-sensitive help (subcommand and options)
- [x] Update README to defer to CLI as documentation source
- [x] Final refactor pass (code style, patterns, efficiency)
- [x] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

### Process Improvement Needed
**Issue**: Agent violated TDD cycle by implementing before writing behavioral test. Root cause: plan specified unit test at wrong level (testing `SnapshotReason.message` property) instead of behavioral test at CLI boundary. Need to improve planning process to emphasize behavioral testing at highest meaningful level per TestMints principles.

**Corrective action**: Writing CLI-level test first that verifies stderr output format with actionable guidance.

### Checklist Item 1 Complete - Work Card Restructured
Consolidated duplicate Implementation Notes sections and restructured checklist items from micro-tasks to broad feature slices following WORK_CHECKLIST.md template guidelines.

### Checklist Item 2 Complete - CLI Help Text Enhanced
**Files changed**:
- `Tagger.kt:11-37` - Enhanced help() method with:
  - Clarified -SNAPSHOT as "unmet conditions for tagging"
  - Added explicit directive: "Snapshot versions should not be used in releases or tags"
  - Changed "explain why" → "describe conditions that must be resolved"
  - Added new "Automation & AI Agents:" section recommending --format=json
  - Listed key JSON fields (snapshot boolean, snapshotReasons array, version string)
  - Provided example command and reference to subcommand help
- `TaggerTest.kt:65-75` - Added test `helpTextGuidesAutomationToJsonFormat()` verifying automation guidance

**Refinement applied**: Removed "decorative text" concept to avoid negative framing in agent context

**Validation**: `./gradlew :command-line-tools:tagger-cli:check` passed (all 94 tests, including new test)

### Checklist Item 3 Complete - Snapshot Diagnostic Output Enhanced
**Files changed**:
- `ChangeType.kt:131-153` - Added `message: String` property to SnapshotReason enum with actionable guidance for each reason
- `CalculateVersion.kt:82-92` - Updated output() to format reasons as `"ENUM_NAME - message"` on separate lines to stderr
- `CalculateVersionCommandTest.kt:343-372` - Added test `snapshotReasonsIncludeActionableGuidance()` verifying format

**Output format**:
- Before: `[DIRTY, AHEAD]` (single line, brackets, comma-separated)
- After: Multi-line with enum name and guidance:
  ```
  DIRTY - Uncommitted changes in working directory. Commit or stash before tagging.
  AHEAD - Local branch ahead of remote. Push changes before tagging.
  ```

**Design decisions**:
- TDD at CLI boundary (not unit testing enum property)
- Enum names preserved at line start for parseability
- No emoji prefix (keeps separation from warnings clean per existing partition logic)
- Follows FailureVersionReasons pattern for message property
- JSON format unchanged (still outputs enum names as strings)

**Validation**: `./gradlew :command-line-tools:tagger-cli:check` passed (all 95 tests including new behavioral test)

### Checklist Item 4 Complete - Context-Sensitive Help Enhanced
**Files changed**:
- `CalculateVersion.kt:34-40` - Added help text for `--implicit-patch` option explaining automatic patch bumping behavior
- `CalculateVersion.kt:36-39` - Added help text for `--allow-detached-head` option explaining detached HEAD blocking
- `CalculateVersion.kt:38-40` - Added help text for `--force-snapshot` option explaining snapshot forcing behavior
- `CalculateVersion.kt:41-43` - Added help text for `--release-branch` option explaining branch-based versioning
- `CalculateVersionCommandTest.kt:315-323` - Added test `helpTextExplainsForceSnapshotOption()`
- `CalculateVersionCommandTest.kt:326-334` - Added test `helpTextExplainsReleaseBranchOption()`
- `CalculateVersionCommandTest.kt:337-345` - Added test `helpTextExplainsAllowDetachedHeadOption()`
- `CalculateVersionCommandTest.kt:348-356` - Added test `helpTextExplainsImplicitPatchOption()`

**Help text added for four priority options**:
1. `--implicit-patch` - Explains automatic patch bumping (default: true)
2. `--allow-detached-head` - Explains detached HEAD blocking and why
3. `--force-snapshot` - Explains snapshot forcing for testing/CI workflows
4. `--release-branch` - Explains branch-based release vs snapshot logic

**TDD approach**: Each option followed test-first cycle - write failing test, add help text, verify pass, commit.

**Validation**: `./gradlew :command-line-tools:tagger-cli:check` passed (all 99 tests)

**Commits**:
- fec7bb5 `[patch] add help text for --force-snapshot option`
- 438b59c `[patch] add help text for --release-branch option`
- 997aa8c `[patch] add help text for --allow-detached-head option`
- 156f0a7 `[patch] add help text for --implicit-patch option`

### Checklist Item 5 Complete - README Updated to Defer to CLI Help
**Files changed**:
- `command-line-tools/tagger-cli/README.md` - Restructured to defer to CLI help as documentation source:
  - Added early reference to `--help` commands after basic command examples
  - Removed detailed field documentation (data.snapshot, data.version, snapshotReasons) that duplicates CLI help
  - Removed error code documentation (CONFIGURATION_ERROR, TAG_ERROR) that duplicates CLI help
  - Strengthened help section at end to explicitly reference all help commands
  - Kept high-value content: installation, basic examples, CI integration patterns, troubleshooting
- `command-line-tools/tagger-cli/src/jvmTest/kotlin/com/zegreatrob/tools/tagger/cli/ReadmeTest.kt` - Added behavioral test enforcing DRY principle:
  - Verifies README exists and is readable
  - Verifies README references `tagger --help` and `calculate-version --help`
  - Verifies README does NOT duplicate field documentation

**Design decisions**:
- Test placed in jvmTest (not commonTest) since it requires file I/O
- Test enforces ongoing compliance with DRY constraint
- Kept CI integration examples (they show usage patterns, not field definitions)
- Kept Common Errors section (troubleshooting, not feature documentation duplication)

**Validation**: `./gradlew :command-line-tools:tagger-cli:check` passed (118 tasks, all 103 tests including 4 new README tests)

**Commit**: c815658 `[none] update README to defer to CLI help as documentation source`

### Checklist Item 6 Complete - Final Refactor Pass
**Files changed**:
- `tools/tagger-core/src/commonMain/kotlin/com/zegreatrob/tools/tagger/core/ChangeType.kt`
  - Lines 73-84: Refactored `findAppropriateIncrement()` to break complex chain into named intermediate variables
  - Lines 163-167: Extracted `padWeekNumber()` helper from `weekNumber()` for clarity
- `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/CalculateVersion.kt`
  - Line 122: Renamed `versionRegex()` → `buildVersionRegex()` (action verb for intent clarity)
  - Line 105: Extracted `formatSnapshotReason(reason)` helper function

**Design decisions**:
- All changes are behavioral-preserving refactorings
- Improved readability per PLAYBOOK_CODE_STYLE.md (function length, naming, data flow clarity)
- All tests pass (no behavioral changes)

**Validation**: `./gradlew check` passed (299 tasks, all tests including 103 tagger-cli tests)

### Checklist Item 7 Complete - Playbook Compliance Review
Verified against PLAYBOOK_CODE_STYLE.md:
- ✓ Function length: All functions <10 lines or acceptably scoped for complexity
- ✓ Data flow: Immutable structures, functional transforms throughout
- ✓ Naming: Intent over implementation (buildVersionRegex, formatSnapshotReason, padWeekNumber)
- ✓ Comments: No unnecessary comments added; code clarity prioritized
- ✓ TDD: All feature work followed red-green-refactor cycle
- ✓ Backward compatibility: All changes are [patch] or [none]; no breaking changes

GRADLE_PLAYBOOK.md: Not applicable (no Gradle build changes)
GITHUB_ACTIONS_PLAYBOOK.md: Not applicable (no CI/CD changes)

### Key Files
- `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/Tagger.kt` (main help)
- `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/CalculateVersion.kt` (command implementation)
- `command-line-tools/tagger-cli/README.md` (to be updated for DRY)
- Test files in `tools-tests/tagger-test/`

### Snapshot Reasons and Action Hints
From ChangeType.kt:131-153:
- `FORCED` → snapshot forced via --force-snapshot flag
- `DIRTY` → uncommitted changes; suggest commit or stash
- `AHEAD` → local ahead of remote; suggest push
- `BEHIND` → local behind remote; suggest pull
- `NOT_RELEASE_BRANCH` → not on configured release branch; suggest switch
- `NO_NEW_VERSION` → no commits since last tag; version unchanged

### Help Content Strategy
- CLI help text is the canonical source
- README should say "run `tagger --help` for full usage" and maintain high-level examples only
- Avoid repeating snapshot reason explanations in multiple places
- Make CLI help self-sufficient for agents discovering the tool

### Semver Impact
All changes are `[patch]` - improving messaging without changing behavior or API.

## Validation
- Commands: `./gradlew :command-line-tools:tagger-cli:check`
- Results: BUILD SUCCESSFUL - 118 tasks (14 executed, 104 up-to-date), 103 tests passed including 4 help text tests and 4 README tests

### Final Refactor Pass Validation
- Commands: 
  - `./gradlew :tools:tagger-core:check` (after ChangeType.kt refactoring)
  - `./gradlew :command-line-tools:tagger-cli:check` (after CalculateVersion.kt refactoring)
  - `./gradlew check` (full validation)
- Results: BUILD SUCCESSFUL - 299 tasks (32 executed, 267 up-to-date), all 103 tagger-cli tests passed
- Changes applied:
  - `ChangeType.kt:73-84` - Broke multi-chain `findAppropriateIncrement()` into intermediate variables (commits, changeTypes, highest)
  - `ChangeType.kt:163-167` - Extracted `padWeekNumber()` helper function for clarity
  - `CalculateVersion.kt:122` - Renamed `versionRegex()` → `buildVersionRegex()` (action verb)
  - `CalculateVersion.kt:105` - Extracted `formatSnapshotReason()` helper function
