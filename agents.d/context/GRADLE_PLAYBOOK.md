---
load_when: modifying build.gradle.kts, settings.gradle.kts, gradle.properties, convention plugins, version catalog, or Gradle tasks
cost: ~500 tokens
brief: build scope rules, lazy task APIs, property exposure, plugin patterns, validation steps, CLI testing
---

# Gradle Playbook

## Purpose
Guide for Gradle build logic, dependencies, tasks, and repository automation.

## When To Use
- Modifying `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Updating convention plugins (`build-logic/`)
- Managing `gradle/libs.versions.toml`
- Adding/modifying Gradle tasks

## Critical Facts

**Scope**
- Root: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` (orchestration only)
- Module: `:module/build.gradle.kts` (module-specific config)
- Convention: `build-logic/` (shared behavior)
- Versions: `gradle/libs.versions.toml` (dependency versions)

**Task Implementation**
- Use typed tasks and Kotlin DSL over shell scripts
- Use lazy APIs: `register()`, `named()`, `configureEach()`
- Always set `group` and `description` on user-facing tasks (without group, hidden from `./gradlew tasks`)
- Expose `Property<T>` or `Provider<T>` directly (no getter/setter wrappers)
- Declare inputs/outputs for incremental builds
- Configuration-cache compatible

**Property Types**
- Configurable: `Property<T>`
- Read-only: `Provider<T>`
- Files: `RegularFileProperty`, `DirectoryProperty`
- Collections: `ListProperty<T>`, `SetProperty<T>`, `MapProperty<K,V>`

**Generated Resources Pattern (KMP)**
```kotlin
val copyResources by registering(Copy::class) {
    from(sourceDir)
    into(layout.buildDirectory.dir("generated/resources/commonMain"))
}

kotlin.sourceSets {
    commonMain {
        resources.srcDir(copyResources.map { it.destinationDir })
    }
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn(copyResources)  // Explicit task ordering
}
```
- Generated resources go in `build/generated/resources/<sourceSetName>/`
- Add to source set via `resources.srcDir(task.map { it.destinationDir })`
- Use `.map { it.destinationDir }` for lazy Provider-based configuration
- ProcessResources auto-discovers but explicit `dependsOn` ensures ordering
- Pattern works for both JVM and JS/Native targets in KMP

**Plugin Implementation (preference order)**
1. Binary plugins (Kotlin/Java)
2. Precompiled script plugins
3. Convention plugins
- Never Groovy
- Minimize external dependencies

**Validation**
1. Module: `./gradlew :module:task -q --console=plain`
2. Affected modules
3. Full: `./gradlew check -q --console=plain` (required before commit)

**Agent-Optimized Flags**
- `-q` — quiet mode, errors only (minimize token usage)
- `--console=plain` — no ANSI codes
- `--warning-mode=none` — suppress warnings (optional, use if noise remains)
- Override per task if diagnostics needed

**CLI Testing (Kotlin/JS)**
- Install: `./gradlew :command-line-tools:<cli>:jsLink`
- Test: `npm exec <command>`

## Constraints
- All automation via `./gradlew` tasks
- Keep build logic and consumer updates together
- Update `agents.d/context/` when conventions change
- Treat warnings as errors
- **NEVER write Copy task outputs to `src/` directories** — violates Gradle contract
  - Source directories (`src/`) = authored files only
  - Build directories (`build/`) = generated/copied files only
  - Use `build/generated/` and configure source sets to include them

## Naming Conventions
- Plugin classes: `PluginNamePlugin`
- Extensions: `PluginNameExtension`
- Tasks: `PluginNameTask`
- Properties/functions: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Plugin IDs: `com.namespace.pluginname`

## Common Mistakes
- Eager APIs: `create()`, `getByName()`, `all()`
- Missing `group`/`description` on tasks
- Wrapping `Property<T>` with getters/setters
- No task inputs/outputs declared
- Breaking configuration cache
- Duplicating logic instead of using conventions
- Separating build logic from consumer updates
- **Copying build artifacts into `src/` directories** (generates files in source control)
- Forgetting to add generated directories to source sets (files won't be packaged)
- Using direct directory paths instead of `task.map { it.destinationDir }` (breaks laziness)

## Completion Checklist
- List changed files and reasons
- List executed commands and outcomes
- Note residual risks (CI-only, platform-specific, config-cache)
