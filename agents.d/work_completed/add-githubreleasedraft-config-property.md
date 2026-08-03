# Add githubReleaseDraft Configuration Property

## Goal
Add a `githubReleaseDraft` boolean property to `TaggerExtension` that allows consumers to control whether GitHub releases are created as drafts or published immediately, fixing the unintended breaking change introduced in commit `8e6662a6`.

## Constraints
- Must restore pre-8e6662a6 behavior (publish immediately) for true backward compatibility
- Must maintain supply chain security benefits as the recommended opt-in path
- Should be well-documented with clear security trade-offs
- Semver intent: `[minor]` - adds new configuration property (non-breaking, restores previous behavior)
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
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Add `githubReleaseDraft` property to `TaggerExtension` (default `true`)
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/tagger/TaggerExtension.kt`
  - Location: After `githubReleaseEnabled` property (~line 76)
  - Property: `val githubReleaseDraft = objectFactory.property(Boolean::class.java).convention(true)`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Commit: a7272e38
- [x] Modify `draftReleaseScript()` to accept `draft: Boolean` parameter
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Lines: 115-121
  - Conditionally include/exclude `--draft` flag based on parameter
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Commit: ac34e27d
- [x] Update `registerGithubReleaseTask()` to pass `tagger.githubReleaseDraft.get()`
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Line: 112 (call to `draftReleaseScript`)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Commit: ac34e27d
- [x] Update task description to reflect configurable behavior
  - File: `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Line: 109
  - Change from: "create GitHub draft release"
  - Change to: "create GitHub release (draft by default)"
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Commit: ac34e27d
- [x] Add test for default draft behavior
  - File: `tools-tests/tagger-plugin-test/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`
  - Test: Verify default creates drafts (command includes `--draft`)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Commit: a7272e38
- [x] Add test for published release behavior
  - File: `tools-tests/tagger-plugin-test/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`
  - Test: Verify `githubReleaseDraft.set(false)` creates published releases (command excludes `--draft`)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Commit: ac34e27d
- [x] Update README.md with configuration documentation
  - File: `tools/tagger-plugin/README.md`
  - Location: After line 146 (in GitHub release section)
  - Add: Configuration example showing `githubReleaseDraft` property
  - Add: Documentation of default behavior and security implications
  - Add: Guidance on when to use `githubReleaseDraft.set(false)`
  - Add: Warning about supply chain security trade-offs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Commit: 6439cf48
- [x] Verify documentation links and grammar (DOCUMENTATION.md protocol)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - All links verified (200 OK)
  - Commit: 6439cf48
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
  - Agent completed comprehensive quality audit
  - Result: PASS - Zero issues found
  - All quality checks passed (function length, duplication, comments, unused code, data flow, naming, documentation)
  - Cross-module validation: All tests pass (unit + functional)
- [x] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: 4537f6e9
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Complete
- **Date**: 2026-08-03

## Implementation Notes
_(newest first)_

### 2026-08-03: Default flipped for true backward compatibility
User correctly identified that default should be `false` (publish immediately) to restore pre-8e6662a6 behavior.
- Changed convention from `true` to `false`
- Updated all tests to verify new default
- Updated documentation to recommend draft-first as opt-in for security
- Commit: 4537f6e9

### 2026-08-03: Work complete
Final mandatory refactor pass completed via subagent. Zero issues found. All tests pass (unit + functional).

Implementation successfully adds `githubReleaseDraft` property with true backward-compatible default (false), comprehensive tests, and clear documentation of security trade-offs.

### 2026-08-03: Feature slices complete
All three feature slices implemented and committed:
1. a7272e38: [minor] Add githubReleaseDraft property (default true)
2. ac34e27d: [minor] Wire githubReleaseDraft through script generation
3. 6439cf48: [none] Document githubReleaseDraft configuration

All validation checks passed. Ready for final mandatory refactor pass via subagent.

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
- [x] `./gradlew :tools-tests:tagger-plugin-test:test --tests "*githubRelease*" -q --console=plain` - new tests pass ✓
- [x] `./gradlew check -q --console=plain` - all checks pass ✓
- [x] Verify README.md renders correctly with new configuration examples ✓
- [x] Verify link to GitHub's Immutable Releases documentation still works ✓ (200 OK)
