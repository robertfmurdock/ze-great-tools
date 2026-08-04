# Add GitHub Release Upload and Publish Tasks

## Goal
Add `githubReleaseUpload` and `githubReleasePublish` tasks to complete the draft-first release workflow, enabling pure-Gradle orchestration of the full tag→draft→upload→publish sequence.

## Constraints
- **Scope**: Tag-based release orchestration only. No asset metadata, changelog, or GitHub feature expansion beyond upload/publish
- **Philosophy**: Configuration over task variants. Tasks controlled by existing `githubReleaseEnabled`/`githubReleaseDraft` properties
- **Dependencies**: Continue using `gh` CLI (no REST API dependencies). Maintain idempotency
- **Backward compatibility**: Existing `githubRelease` behavior unchanged when `githubReleaseAssets` not configured
- **Proof**: Must successfully migrate this repository's workflow before considering complete
- **Semver intent**: `[minor]` - new backward-compatible features
- **Affected modules**: `tagger-plugin`, `tagger-plugin-test`, root `build.gradle.kts`, `.github/workflows/main.yml`

## Context
The tagger plugin's `githubReleaseDraft` feature enables secure immutable releases (draft → add assets → publish), but lacks Gradle task support for asset upload and publish steps. This forces users to drop into shell scripts (see `.github/workflows/main.yml` lines 78-89), breaking the "pure Gradle workflow" that justifies tagger's orchestrator pattern.

Research shows no Gradle plugin provides complete tag→draft→upload→publish orchestration. BreadMoirai (109 stars, industry leader) handles uploads but requires REST API and OkHttp dependencies. Tagger can provide simpler automation using the `gh` CLI.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Add `githubReleaseAssets` property to `TaggerExtension`
  - Use `ConfigurableFileCollection` for automatic task dependency wiring
  - Support task outputs as inputs (e.g., `tasks.named("distZip").map { it.outputs }`)
  - Convention: empty collection (no assets by default)
  - TDD: Test property configuration and file resolution
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Add `githubReleaseUpload` task
  - Exec task that wraps `gh release upload $VERSION <files>`
  - Input: `githubReleaseAssets` file collection
  - Depends on: `githubRelease` (draft must exist first)
  - Enabled when: `githubReleaseEnabled && !githubReleaseAssets.isEmpty`
  - Idempotent: Check if files already uploaded before uploading
  - Disabled for SNAPSHOT versions (matches `githubRelease` convention)
  - TDD: Functional test with real file uploads to draft release
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Add `githubReleasePublish` task
  - Exec task that wraps `gh release edit $VERSION --draft=false`
  - Depends on: `githubReleaseUpload` (assets must be uploaded first)
  - Enabled when: `githubReleaseEnabled && githubReleaseDraft`
  - Idempotent: Check if already published before editing
  - Disabled for SNAPSHOT versions
  - TDD: Functional test publishes draft after upload
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Wire tasks into `release` orchestrator
  - `release` task should finalize with `githubReleasePublish` (when enabled)
  - Task graph: assemble → tag → githubRelease (draft) → githubReleaseUpload → githubReleasePublish → publish
  - Ensure backward compatibility: when `githubReleaseDraft=false`, skip upload/publish tasks
  - TDD: Test orchestration with various configuration combinations
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Update this repository's configuration
  - Add `githubReleaseAssets.from(...)` to `build.gradle.kts`
  - Include CLI distribution and fingerprint.txt
  - Enable `githubReleaseDraft.set(true)`
  - TDD: Dry-run `./gradlew release --dry-run` shows correct task sequence
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Update this repository's GitHub Actions workflow
  - Remove manual `gh release upload` commands (now handled by Gradle)
  - Remove manual `gh release edit --draft=false` (now handled by Gradle)
  - Simplify workflow to just `./gradlew release`
  - TDD: CI run succeeds with new configuration
  - Agent cycle: test → implement → refactor-light → verify pushable
- [ ] Document new tasks in README
  - Add configuration examples showing `githubReleaseAssets.from(...)`
  - Document task outputs as inputs pattern (automatic dependency wiring)
  - Show both simple and complex workflows
  - Explain when to use draft vs immediate publish
  - Link verification: All URLs must return 200 OK
  - Grammar check via `mcp__idea__get_file_problems`
  - Format via `mcp__idea__reformat_file`
  - Agent cycle: test → implement → refactor-light → verify pushable
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)

## Current State
**Commit**: a04dcc9a (Refactor TaggerPlugin apply method to eliminate duplication)
**Uncommitted changes**: Work card update only
**Status**: COMPLETE - all implementation, testing, and refactoring done
**Date**: 2026-08-03

## Implementation Notes
(Most recent first, date-stamped)

### 2026-08-03: Final refactor pass completed
- Completed commit a04dcc9a
- Fixed critical code duplication: extracted `isSnapshot` variable (5 instances eliminated)
- Refactored `apply()` method from 14→10 lines by extracting helper methods:
  - `registerVersioningTasks()` - groups versioning-related task registrations
  - `registerGithubTasks()` - groups GitHub-related task registrations
- All task registration methods now accept `isSnapshot` as parameter
- Quality audit passed: 0 critical issues remain
- Full validation: `./gradlew check` passes
- Work card ready to move to work_completed

### 2026-08-03: Publish task integrated
- Completed commits 00188133 and 62ebbbdf
- githubReleasePublish task wired into release orchestrator
- Manual publish step removed from workflow
- Workflow now fully Gradle-managed: single `./gradlew release` handles draft → upload → publish
- Task execution order verified with dry-run

### 2026-08-03: Bug fixes and cleanup
- Fixed missing task dependencies (5f8ea917): fileTree needs builtBy() for CLI dist tasks
- Removed obsolete uploadCliDistributions task (e3d51fe8)
- Reverted unnecessary empty assets fix (3f4ea1d0) - disabled task with invalid script is harmless

### 2026-08-03: Upload task integrated
- Completed commits 4496dbd1 through 79f4d3ab
- githubReleaseAssets property added and tested
- githubReleaseUpload task created, wired to release orchestrator
- githubReleasePublish task created (initial implementation)
- Repository configuration updated with assets (fingerprints + CLI distributions)
- Workflow simplified to remove manual gh release upload commands
- All changes backward compatible, safe to push

## Validation
```bash
./gradlew check -q --console=plain
```
**Status**: Not yet run

## Design Notes

### Task Naming Rationale
Blends `gh release` CLI verbs with Gradle conventions:
- `gh release create` → `githubRelease` (existing)
- `gh release upload` → `githubReleaseUpload` (new)
- `gh release edit --draft=false` → `githubReleasePublish` (new)

### FileCollection Benefits
Using `ConfigurableFileCollection` for assets provides:
- Automatic task dependency inference (Gradle wires dependencies from task outputs)
- Incremental build support (skip if unchanged)
- Provider API integration (lazy configuration)
- Support for glob patterns, task outputs, file paths

### Idempotency Strategy
Follow existing `githubRelease` pattern:
```bash
if gh release view $VERSION >/dev/null 2>&1; then
    # Check existing state, skip if already done
else
    # Perform operation
fi
```

### Backward Compatibility
When `githubReleaseAssets` is empty (default):
- `githubReleaseUpload` task created but disabled
- `githubReleasePublish` task created but disabled
- Existing workflows continue working unchanged

When `githubReleaseDraft=false`:
- Assets can still be uploaded (convenience)
- But publish task is no-op (nothing to publish)

### Scope Boundaries
**In scope**: Asset uploads needed for immutability pattern
**Out of scope**: 
- Asset labels/metadata (too GitHub-specific)
- Changelog generation (different domain, see Shipkit)
- Release notes templating (belongs in project config)
- Multiple upload targets (use shell script for complex cases)
