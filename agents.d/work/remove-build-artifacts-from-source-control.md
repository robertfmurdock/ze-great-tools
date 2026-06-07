# Remove Build Artifacts from Source Control

## Goal
Refactor Copy tasks to place build artifacts in `build/` directories instead of `src/` directories, following Gradle best practices that build outputs should never be written to source-controlled locations.

## Background
The digger-cli build uses a `copyGuideResources` task to copy `digger-guide.md` from `tools/digger-guide/src/commonMain/resources` into `command-line-tools/digger-cli/src/commonMain/resources/help/` at build time. This violates Gradle best practices:

**Current (incorrect) pattern:**
- Copies build artifact INTO `src/commonMain/resources/` (a source directory)
- File ends up tracked in git (currently showing as modified)
- Confuses developers about which file is the source of truth

**Correct Gradle pattern:**
- Copy artifacts to `build/generated/resources/` or similar
- Add generated location to source set's resource directories
- Source directories (`src/`) contain only hand-written files
- Build directories (`build/`) contain only generated/copied files
- No .gitignore gymnastics needed - `build/` is already ignored

## Constraints
- Copy tasks must target `build/` directories, never `src/` directories
- Source sets must be configured to include generated resource directories
- Must verify no other build artifacts are tracked in source control
- Must not break existing build/test processes
- IDE should still function correctly (IDEA may need resource directories marked appropriately)
- ProcessResources task must still package the copied files into JARs
- Semver intent: `[none]` - infrastructure cleanup, no functional changes

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Audit: Find all `Copy` tasks in `*.gradle.kts` files that copy to `src/` directories
  - Agent cycle: investigate only
  - Document findings in Implementation Notes (digger-cli, possibly others)
- [x] Audit: Check if any target paths from Copy tasks are tracked in git
  - Agent cycle: investigate only
  - Update plan if more artifacts found
- [x] Research: Gradle best practices for Copy tasks and build outputs
  - Where should Copy task outputs go? (`build/` vs `src/`)
  - When to use Copy vs consuming artifacts via dependencies
  - Gradle docs on generated sources and resources
  - Best practices for avoiding pollution of source directories
  - Anti-patterns to avoid (copying into `src/`, committing generated files)
  - Agent cycle: investigate only
  - Document key findings in Implementation Notes
  - Update plan if findings suggest different approach
- [x] Research: Correct Gradle pattern for generated resources in Kotlin Multiplatform
  - How to add `build/generated/resources/` to source sets
  - How ProcessResources integrates with custom resource directories
  - KMP-specific considerations for resource handling across platforms
  - Agent cycle: investigate only
  - Update plan based on findings
- [ ] Refactor digger-cli build.gradle.kts:
  - Change `copyGuideResources` to copy into `build/generated/resources/commonMain/help/`
  - Configure `commonMain` source set to include generated resources directory
  - Remove dependency on `copyGuideResources` from ProcessResources if no longer needed (may be implicit)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if pattern differs from research
- [ ] Refactor any other Copy tasks found in audit (if any)
  - Apply same pattern: `build/generated/` + source set configuration
  - Agent cycle: test → implement → refactor-light → verify pushable
- [ ] Remove tracked build artifacts from git:
  - `git rm --cached command-line-tools/digger-cli/src/commonMain/resources/help/digger-guide.md`
  - Remove any other artifacts found in audit
  - Agent cycle: test → implement → refactor-light → verify pushable
- [ ] Verify build creates resources in correct location:
  - Run `./gradlew :command-line-tools:digger-cli:copyGuideResources`
  - Confirm file created in `build/generated/resources/`
  - Confirm file NOT created in `src/`
  - Agent cycle: verify pushable
- [ ] Verify resources are packaged into JAR:
  - Run `./gradlew :command-line-tools:digger-cli:jvmJar`
  - Inspect JAR contents to confirm `help/digger-guide.md` is included
  - Agent cycle: verify pushable
- [ ] Verify tests pass: Run `./gradlew check`
  - Agent cycle: verify pushable
- [ ] Verify IDE still recognizes resources (check in IDEA):
  - Open project in IDEA
  - Confirm generated resources directory marked correctly
  - Confirm code completion / resource resolution works
  - Agent cycle: verify pushable
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: aa6f43f7 (current HEAD)
- **Uncommitted work**: Documentation terminology updates in progress (separate work)
- **Blockers**: None
- **Status**: Ready to start
- **Date**: 2026-06-07

## Implementation Notes
_(newest first)_

### 2026-06-07: Research findings - implementation pattern confirmed
**Implementation pattern:**
```kotlin
val copyGuideResources by registering(Copy::class) {
    into(layout.buildDirectory.dir("generated/resources/commonMain"))
    // ... rest of config
}

kotlin.sourceSets {
    commonMain { 
        resources.srcDir(copyGuideResources.map { it.destinationDir })
    }
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn(copyGuideResources)  // May still be needed for task ordering
}
```

**Key insights:**
- Project already uses correct pattern for generated Kotlin sources (`copyTemplates` → `build/generated-sources/`)
- Same pattern applies to resources: `build/generated/resources/commonMain/`
- Source set configuration makes resources available to both JVM and JS targets
- ProcessResources auto-discovers resources from all configured srcDirs
- Explicit `dependsOn` still recommended for task ordering
- `build/` already in .gitignore, no gymnastics needed

**Files to remove from git after refactor:**
- `command-line-tools/digger-cli/src/commonMain/resources/help/digger-guide.md`
- `command-line-tools/tagger-cli/src/commonMain/resources/help/tagger-guide.md`

### 2026-06-07: Audit findings - two CLIs affected
**Copy tasks copying to src/:**
1. `digger-cli/build.gradle.kts`: `copyGuideResources` copies from `tools/digger-guide/src/commonMain/resources` → `digger-cli/src/commonMain/resources`
2. `tagger-cli/build.gradle.kts`: `copyGuideResources` copies from `tools/tagger-guide/src/commonMain/resources` → `tagger-cli/src/commonMain/resources`

Both tasks include only the guide file (`*-guide.md`).

**Build artifacts tracked in git:**
- `command-line-tools/digger-cli/src/commonMain/resources/help/digger-guide.md` (copied from `tools/digger-guide`)
- `command-line-tools/tagger-cli/src/commonMain/resources/help/tagger-guide.md` (copied from `tools/tagger-guide`)

**Other files in help/ directories:**
These appear to be hand-written source files (NOT build artifacts):
- digger-cli: `all-contribution-data.md`, `current-contribution-data.md`, `digger.md`
- tagger-cli: `calculate-version.md`, `tag.md`, `tagger.md`

**Other Copy tasks found (non-problematic):**
- digger-cli: `copyReadme` → npm project dir (build output)
- digger-cli: `copyTemplates` → generated directory (build output)
- tagger-cli: `copyReadme` → npm project dir (build output)
- tagger-cli: `copyHelpResources` → npm project dir (build output)
- tagger-cli: `copyTemplates` → generated directory (build output)
- reports plugin: `copyReportsToRootDirectory`, `copyTestResultsToRootDirectory` → build output
- root: `collectResults` → build output

Both CLIs follow identical problematic pattern and need same refactor.

### 2026-06-07: Subagent authorization granted
User authorized subagent use for final refactor pass.

### 2026-06-07: Issue discovered during CI/CD terminology cleanup
While updating documentation to replace "CI/CD" jargon with "build automation", discovered that `command-line-tools/digger-cli/src/commonMain/resources/help/digger-guide.md` is both:
1. A build artifact (copied during build from tools/digger-guide)
2. Tracked in source control (shows up in `git status`)

The build.gradle.kts clearly shows this is a copy task:
```kotlin
val copyGuideResources by registering(Copy::class) {
    from(rootProject.layout.projectDirectory.dir("../tools/digger-guide/src/commonMain/resources"))
    into(layout.projectDirectory.dir("src/commonMain/resources"))
    include("help/digger-guide.md")
}
```

Commit 8f5a47ae message states "ONE digger-guide.md in source tree" but the build artifact ended up committed anyway.

## Related Context
- Original implementation: commit 8f5a47ae "[patch] Implement single-source digger guide via digger-guide module"
- Similar pattern may exist for tagger-guide (should audit)
- Correct pattern: standalone module → copy to `build/generated/` → add to source set → package in JAR
- Reference: Gradle docs on [Generated Sources](https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_source_sets)
