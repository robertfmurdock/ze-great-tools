---
issue: https://github.com/robertfmurdock/ze-great-tools/issues/317
semver: [patch]
created: 2026-06-04
---

# Fix tagger --help path resolution

## Goal
Fix `npx tagger --help` crash by making `getTaggerGuideContent` resolve help file relative to package location instead of cwd.

## Constraints
- [patch] — bug fix, no API changes
- Affects only top-level `tagger --help` command
- Subcommand help (e.g., `tagger tag --help`) already works and is unaffected
- Must work from any working directory
- Solution should prevent similar issues in future

## Checklist
- [x] [patch] Fix getTaggerGuideContent to use package-relative path resolution
- [x] [patch] Verify help renders correctly from arbitrary working directories
- [x] [none] Add regression tests to prevent reintroduction
- [x] [none] Final refactor via MANDATORY subagent (REFACTOR_AGENT.md)

## Current State
**Commit**: e26c4aed ([none] Add regression tests for help from different working directories)
**Status**: In progress - awaiting final refactor pass 2
**Blockers**: None
**Date**: 2026-06-04

## Implementation Notes
*Newest entries first, date-stamped*

### 2026-06-04: Regression tests added (TDD violation)
- Added JS-specific tests in `tagger-cli/src/jsTest` and `digger-cli/src/jsTest`
- Tests change working directory to tmpdir, invoke --help, verify it works
- **PROTOCOL VIOLATION**: Should have written these tests BEFORE implementing the fix
- Tests verified to catch the bug: failed with buggy code, pass with fix ✓
- Commits: e26c4aed

### 2026-06-04: Refactor complete - subagent authorized
- Refactor agent found identical bug in `digger-guide` (CRITICAL severity)
- Applied same fix to `DiggerGuideJs.kt` to prevent `npx digger --help` crashes
- Identified Node externals duplication (MAJOR) - architectural decision deferred
- All quality checks passed: function length, naming, data flow, unused code, comments ✓
- Full validation: `./gradlew check -q --console=plain` passes ✓

### 2026-06-04: Implementation complete
- Modified `TaggerGuideJs.kt` to use `__dirname` (via `nodeDirname`) with `NodePath.join()`
- Path now resolves to `join(__dirname, "help", "tagger-guide.md")` instead of `"./kotlin/help/tagger-guide.md"`
- Compiled JS places modules in `/kotlin/` subdirectory with help files in `/kotlin/help/`
- Using `__dirname` makes path relative to module location, not cwd
- Reused existing patterns from `NodeExternals.kt` (nodeDirname, NodePath)
- Tests pass: `:tools:tagger-guide:check` ✓
- Manual verification: `npx tagger --help` works from both project root and subdirectories ✓

### 2026-06-04: Work card created
- Issue #317: `npx tagger --help` crashes with ENOENT
- Root cause: `./kotlin/help/tagger-guide.md` resolves relative to cwd, not package
- Confirmed only one instance: `readFileSync('./` in getTaggerGuideContent
- File exists at `node_modules/@continuous-excellence/tagger/kotlin/help/tagger-guide.md`
- Solution needs to be on Kotlin/Gradle side since JS is generated code

## Validation
*Update incrementally as checklist progresses*

- [x] `./gradlew check -q --console=plain` passes
- [x] `npx tagger --help` works from project root
- [x] `npx tagger --help` works from subdirectory
- [x] Subcommand help still works (sanity check)
- [x] Regression tests catch the bug (verified by reverting to broken code)
