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
- [x] Enhance main help text (Tagger.kt) to clarify snapshot semantics and recommend JSON for automation
  - Test: Help output includes clear guidance about snapshot suffix as condition indicator
  - Test: Help prominently recommends --format=json for automation/agents
- [ ] Improve text-format snapshot reason output with actionable context
  - Test: Snapshot reasons include action hints (e.g., DIRTY → "commit or stash changes")
  - Test: Output includes footer recommending JSON format for structured data
- [ ] Add calculate-version subcommand help explaining purpose and workflow
  - Test: Subcommand help clearly explains snapshot semantics
  - Test: Help links snapshot reasons to tagging workflow
- [ ] Enhance --format option help to emphasize automation use case
  - Test: Option help explicitly mentions AI agents and CI/CD
- [ ] Update README to reference CLI help as source of truth, remove duplication
  - Test: README points to `tagger --help` and `tagger calculate-version --help` for detailed usage
  - Test: README maintains examples but defers to CLI for canonical guidance
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

### Checklist Item 2 Complete - Main Help Text Enhanced
**Files changed**:
- `Tagger.kt:11-37` - Enhanced help() method with:
  - Clarified -SNAPSHOT as "unmet conditions for tagging (not decorative text)"
  - Added explicit statement: "The version is ready to tag only when -SNAPSHOT is absent"
  - Changed "explain why" → "describe conditions that must be resolved"
  - Added new "Automation & AI Agents:" section recommending --format=json
  - Listed key JSON fields (snapshot boolean, snapshotReasons array, version string)
- `TaggerTest.kt:65-75` - Added test `helpTextGuidesAutomationToJsonFormat()` verifying automation guidance

**Validation**: `./gradlew :command-line-tools:tagger-cli:check` passed (all 94 tests, including new test)

## Implementation Notes

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
- Results: BUILD SUCCESSFUL - 155 tasks (24 executed, 131 up-to-date), 94 tests passed including new `helpTextGuidesAutomationToJsonFormat` test
