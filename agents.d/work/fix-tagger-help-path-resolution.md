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
- [ ] [patch] Fix getTaggerGuideContent to use package-relative path resolution
- [ ] [patch] Verify help renders correctly from arbitrary working directories
- [ ] [none] Final refactor via MANDATORY subagent (REFACTOR_AGENT.md)

## Current State
**Commit**: d0d22da4 (Add work card: fix Gradle plugin publishing metadata)
**Status**: Not started
**Blockers**: None
**Date**: 2026-06-04

## Implementation Notes
*Newest entries first, date-stamped*

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
