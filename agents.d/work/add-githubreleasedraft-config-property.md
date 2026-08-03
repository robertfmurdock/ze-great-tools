# Add githubReleaseDraft Configuration Property

## Goal
Add a `githubReleaseDraft` boolean property to `TaggerExtension` that allows consumers to control whether GitHub releases are created as drafts or published immediately, fixing the unintended breaking change introduced in commit `8e6662a6`.

## Constraints
- Must preserve current default behavior (draft-first) for backward compatibility
- Must maintain supply chain security benefits as the recommended path
- Should be well-documented with clear security trade-offs
- Semver intent: `[minor]` - adds new configuration property (non-breaking, additive)
- Affected modules: `tagger-plugin`, `tagger-plugin-test`

## Root Cause Analysis
On 2026-07-29 (commit `8e6662a6`), the tagger plugin switched from creating published GitHub releases to always creating draft releases. The change:

**Before:**
```kotlin
task.commandLine(
    "gh", "api", "--method", "POST",
    "-F", "draft=false",  // Published immediately
)
```

**After:**
```kotlin
gh release create $version --draft --title $version --notes $version
```

The `--draft` flag became **hardcoded** with no configuration option. This broke existing consumers who expected `githubReleaseEnabled.set(true)` to publish releases immediately, requiring all consumers to add `gh release edit $version --draft=false` to their workflows.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Add `githubReleaseDraft` property to `TaggerExtension` (default `true`)
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/tagger/TaggerExtension.kt`
  - Location: After `githubReleaseEnabled` property (~line 76)
  - Property: `val githubReleaseDraft = objectFactory.property(Boolean::class.java).convention(true)`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Modify `draftReleaseScript()` to accept `draft: Boolean` parameter
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Lines: 115-121
  - Conditionally include/exclude `--draft` flag based on parameter
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update `registerGithubReleaseTask()` to pass `tagger.githubReleaseDraft.get()`
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Line: 112 (call to `draftReleaseScript`)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update task description to reflect configurable behavior
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Line: 109
  - Change from: "create GitHub draft release"
  - Change to: "create GitHub release (draft by default)"
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add test for default draft behavior
  - File: `tools-tests/tagger-plugin-test/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`
  - Test: Verify default creates drafts (command includes `--draft`)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add test for published release behavior
  - File: `tools-tests/tagger-plugin-test/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`
  - Test: Verify `githubReleaseDraft.set(false)` creates published releases (command excludes `--draft`)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update README.md with configuration documentation
  - File: `tools/tagger-plugin/README.md`
  - Location: After line 146 (in GitHub release section)
  - Add: Configuration example showing `githubReleaseDraft` property
  - Add: Documentation of default behavior and security implications
  - Add: Guidance on when to use `githubReleaseDraft.set(false)`
  - Add: Warning about supply chain security trade-offs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Verify documentation links and grammar (DOCUMENTATION.md protocol)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: a6df314e
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: In progress - Feature Slice 1 (default draft behavior test)
- **Date**: 2026-08-03

## Implementation Notes
_(newest first)_

### 2026-08-03: Work started
- Subagent authorization: User approved subagent use for final mandatory refactor pass
- Plan created and approved
- Beginning Feature Slice 1: default draft behavior test

### 2026-08-03: Work card created
Semver intent: `[minor]` - adds new configuration property without breaking existing behavior.

**Problem context:**
- Commit `8e6662a6` (2026-07-29) switched to draft-first pattern for security
- Change was well-intentioned but hardcoded `--draft` with no opt-out
- All consumers now required to add `gh release edit $version --draft=false` to workflows
- No way to restore immediate-publication behavior without forking plugin

**Solution:**
Add `githubReleaseDraft` property (default `true`) to allow configuration while preserving secure defaults.

**Example configuration for README:**
```kotlin
tagger {
    githubReleaseEnabled.set(true)
    
    // Draft-first (default) - recommended for supply chain security
    githubReleaseDraft.set(true)  // Can be omitted, this is the default
    
    // OR: Publish immediately - skips draft step but loses immutability protection
    githubReleaseDraft.set(false)
}
```

**Documentation should emphasize:**
- Draft-first is the secure default (tag/asset immutability, release attestations)
- Immediate publication is simpler workflow but sacrifices immutability protection
- When `githubReleaseDraft = true`: workflow must call `gh release edit $version --draft=false`
- When `githubReleaseDraft = false`: release published immediately, no extra step needed

**Critical files:**
- `TaggerExtension.kt` (add property)
- `TaggerPlugin.kt` (modify script generation logic)
- `TaggerPluginTest.kt` (add test coverage)
- `README.md` (document configuration and trade-offs)

## Validation
Commands to run before marking complete:
- [ ] `./gradlew :tools-tests:tagger-plugin-test:test --tests "*githubRelease*" -q --console=plain` - new tests pass
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] Verify README.md renders correctly with new configuration examples
- [ ] Run grammar check on README.md: `mcp__idea__get_file_problems tools/tagger-plugin/README.md`
- [ ] Verify link to GitHub's Immutable Releases documentation still works
