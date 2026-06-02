---
load_when: modifying build.gradle.kts, settings.gradle.kts, gradle.properties, convention plugins, version catalog, or Gradle tasks
cost: ~600 tokens
brief: build logic scope (root/module/convention/versions), task patterns, validation ladder, CLI testing
---

# Gradle Playbook

## Purpose
Guide for modifying Gradle build logic, dependencies, and repository automation.

## When To Use
- Modifying `build.gradle.kts`, `settings.gradle.kts`, or `gradle.properties`
- Changing module build files
- Updating convention plugins in `build-logic/`
- Managing `gradle/libs.versions.toml`
- Adding/modifying Gradle tasks

## Critical Facts

### Scope Classification
- Root: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Module: `:module/build.gradle.kts`
- Convention: `build-logic/`
- Versions: `gradle/libs.versions.toml`

### Source of Truth
- Dependency versions → version catalog (`gradle/libs.versions.toml`)
- Shared behavior → convention plugins (`build-logic/`)
- Module-specific → module build file
- Root → orchestration only, keep lean

### Task Implementation
- Use typed tasks and Kotlin DSL over shell scripts
- Use lazy APIs: `register`, providers (not eager creation)
- Declare inputs/outputs for incremental builds
- Configuration-cache compatible (avoid capturing script state)
- Treat warnings as errors

### Validation Ladder
1. Smallest check: `./gradlew :module:task`
2. Affected modules
3. Full: `./gradlew check` (required before commit)

### CLI Testing (Kotlin/JS)
- Install local build: `./gradlew :command-line-tools:<cli>:jsLink`
- Test via: `npm exec <command>` (uses local, not published)

## Constraints
- Express automation as Gradle tasks via `./gradlew`
- Keep build logic and consumer updates in same changeset
- Update `agents.d/context/` docs when conventions change
- Never document conventions only in `AGENTS.md` or generated files

## Key Files
- Root: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Versions: `gradle/libs.versions.toml`
- Conventions: `build-logic/`
- Module builds: `:*/build.gradle.kts`

## Common Mistakes
- Duplicating logic across module files instead of using conventions
- Adding unrelated tasks to root build file
- Eager task creation instead of lazy registration
- Not declaring task inputs/outputs
- Breaking configuration cache
- Skipping CLI testing with `jsLink` for Kotlin/JS tools
- Separating build logic changes from consumer updates

## Completion Checklist
- List changed files and why
- List executed commands and outcomes
- Note residual risks (CI-only, platform-specific, config-cache, deferred checks)
