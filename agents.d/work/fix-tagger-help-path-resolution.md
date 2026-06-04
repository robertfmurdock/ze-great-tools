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
- [ ] [patch] Verify help renders correctly from arbitrary working directories
- [ ] [none] Final refactor via MANDATORY subagent (REFACTOR_AGENT.md)

## Current State
**Commit**: d0d22da4 (Add work card: fix Gradle plugin publishing metadata)
**Status**: Not started
**Blockers**: None
**Date**: 2026-06-04

## Implementation Notes
*Newest entries first, date-stamped*

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

- [ ] `./gradlew check -q --console=plain` passes
- [ ] `npx tagger --help` works from project root
- [ ] `npx tagger --help` works from subdirectory
- [ ] Subcommand help still works (sanity check)
