# Remediate Gradle Best Practices Audit Findings

## Goal

Resolve all 24 Gradle audit findings and verify both standing advisories across the root build and its four included builds.

## Constraints

- Semver intent for the card is `[none]`. Each slice has a default below. Record the actual intent per change and confirm any increase with the user.
- Keep each slice safe to push. Preserve CLI stdout, plugin IDs, task entry points, artifact coordinates, and release behavior. Validate release changes through generated metadata, local artifacts, and dry-run task graphs.
- Follow [WORK_CHECKLIST.md](../context/WORK_CHECKLIST.md). Load [TESTING.md](../context/TESTING.md) and [GRADLE_PLAYBOOK.md](../context/GRADLE_PLAYBOOK.md) before implementation. Use the Testmints skill when writing or reviewing Testmints tests.
- Coordinate release changes with [publishing metadata recovery](fix-gradle-plugin-publishing-metadata.md) and [GitHub release ordering](ensure-github-release-after-distribution.md). Preserve marker and implementation POM metadata, required Javadoc artifacts, release ordering, and retry behavior.

## Checklist

- [x] Review this card against the work-card template and reconcile it with the audit inventory.

### Audit reconciliation from 2026-11-11

The latest bundled-catalog audit confirmed the following open or insufficiently
tracked work. These items supplement the historical 24-finding inventory above;
they are not treated as resolved merely because a related remediation slice is
checked off.

- [ ] **AR1 · Medium · Remove remaining `afterEvaluate` usage.** Re-audit
  `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/testmints.gradle.kts:9`
  and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/NpmCliPlugin.kt:52`.
  Replace deferred evaluation with plugin callbacks or lazy configuration, then
  rerun the full `afterEvaluate` scan. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#avoid_after_evaluate)
- [x] **AR1a · Will not implement · Separate NPM package-metadata task.**
  Kotlin's `jsPackageJson` action writes directly into its own package directory,
  regardless of its declared output property's value. A second writer would
  overlap that output. A separate staged package would avoid the overlap but
  duplicate the full package tree and change packaging, linking, and publishing
  paths for this convention alone. Keep Kotlin's generator as the sole package
  metadata owner; test lazy JS target configuration under AR1/AR2 instead. This
  decision does not close the `afterEvaluate` finding. Alternatives and evidence
  are recorded in Implementation Notes.
  [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#use_unique_output_files_and_directories)
- [ ] **AR2 · Medium · Make Kotlin plugin configuration order-safe.** In
  `NpmCliPlugin.kt:53-55`, replace the eager `findByType`/early return with
  `pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform")` and preserve
  the JS target lifecycle behavior. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#dont_assume_plugin_order)
- [ ] **AR3 · High · Remove configuration-time provider and file-collection reads.**
  Recheck `tools-plugins/dashboard-test-summary/src/main/kotlin/com/zegreatrob/tools/dashboard/DashboardTestSummaryPlugin.kt:44,99`
  and the fingerprint plugin configuration paths. Resolve providers and file
  collections only in task actions or wire them as task inputs. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#avoid_provider_get_outside_task_action)
- [x] **AR4 · Not applicable · Project-level dependency repositories.** The
  reported `tools/build.gradle.kts:64` block is `nexusPublishing.repositories`,
  not dependency resolution. All authored dependency repositories are already
  in settings files; publishing destinations remain in the publishing DSL.
- [ ] **AR5 · Medium · Filter multiple repositories.** Add explicit content
  filters where Plugin Portal and Maven Central coexist, especially root and
  included-build plugin-management settings. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#use_content_filtering)
- [ ] **AR6 · Medium · Extract duplicated conventions.** Compare repeated
  lifecycle, repository, testing, checksum, and task configuration across the
  included builds and move remaining shared behavior into convention plugins.
  [Gradle](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#use_convention_plugins)
- [x] **AR7 · Recommendation · Group and describe all user-facing tasks.**
  Cover `DiggerGuideTask`, `TaggerGuideTask`, and the
  `clearDashboardTestSummary` registration in addition to the tasks already
  tracked by T5. The guide tasks already have metadata; only the dashboard
  cleanup registration remains to be checked in the next task-metadata slice.
  [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#group_describe_tasks)
- [ ] **AR8 · Recommendation · Replace internal task-name literals.** Recheck
  string-based task references in `NpmCliPlugin.kt` and related plugin wiring;
  use typed providers or documented public task constants where available.
  [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#dont_hardcode_task_names)

The audit also reconfirmed that wrapper checksums, Kotlin DSL usage, build and
configuration caching, task cacheability annotations, path sensitivities,
TestKit coverage, root naming, and the absence of `buildSrc` are satisfied.

### 1. Wrapper and build defaults `[none]`

- [x] **W1 · High · Distribution checksum.** `gradle/wrapper/gradle-wrapper.properties:3`. Pin the published SHA-256 for the archive in `distributionUrl`. Verify the URL and digest pair against Gradle's published checksums and exercise wrapper bootstrap with an empty distribution cache. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#validate_gradle_checksum)
- [x] **W2 · Recommendation · Binary distribution.** `gradle/wrapper/gradle-wrapper.properties:3`. Use the wrapper skill to regenerate the current version as `-bin` and pin its digest in the same change. Verify the generated URL, SHA-256, and reported Gradle version. W1 and W2 can share a commit if the resulting binary archive is pinned. [Gradle](https://docs.gradle.org/current/userguide/best_practices_performance.html#prefer_bin_distribution)
- [x] **W3 · Medium · UTF-8.** `gradle.properties:4`. Append `-Dfile.encoding=UTF-8` to the existing daemon JVM arguments. Verify the daemon sees UTF-8 and the existing heap setting is retained. [Gradle](https://docs.gradle.org/current/userguide/best_practices_performance.html#use_utf8_encoding)
- [x] **A1 · Advisory · Wrapper JAR verification.** `gradle/wrapper/gradle-wrapper.jar`. Compare its digest against the trusted published checksum or byte-compare with a wrapper regenerated using a trusted Gradle installation. Record the result and verify the upgrade workflow checks future wrapper JAR changes. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#validate_wrapper_checksum)

### 2. Task wiring and outputs `[patch]`

- [x] **T1 · Medium · Unique outputs.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/reports.gradle.kts:14` and `NpmCliPlugin.kt:120` in the same directory. Consolidate producers or stage distinct outputs. Verify one owner per destination and compare collected report paths and npm package contents with the baseline. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#use_unique_output_files_and_directories)
- [x] **T2 · High · Configuration-time filesystem work.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/reports.gradle.kts:29`. Move report directory creation to execution. From a clean output tree, verify `help` leaves the report output absent and `collectResults` creates it as needed. [Gradle](https://docs.gradle.org/current/userguide/best_practices_performance.html#avoid_computations_in_configuration_phase)
- [x] **T3 · Medium · Explicit action-task dependencies.** `command-line-tools/{digger-cli,tagger-cli}/build.gradle.kts` at the template-copy and JVM smoke-test wiring, and `tools/fingerprint-plugin/src/main/kotlin/com/zegreatrob/tools/FingerprintPlugin.kt:103`. Connect producer outputs to consumer inputs and remove redundant edges. Request the consumers alone and verify the required templates, distributions, and fingerprints are built first. Preserve lifecycle and mandatory ordering semantics. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#avoid_depends_on)
- [x] **T4 · Medium · Lazy output composition.** `tools-plugins/dashboard-test-summary/src/main/kotlin/com/zegreatrob/tools/dashboard/DashboardTestSummaryPlugin.kt`. Lazily compose JUnit XML output locations while preserving the producing task dependency. Verify the collector reads a customized XML directory and respects disabled XML reporting. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#map_versus_flatmap)
- [x] **T5 · Recommendation · Task metadata.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/{reports.gradle.kts,NpmCliPlugin.kt}` and `command-line-tools/tagger-cli/build.gradle.kts:111`. Add groups and descriptions to report, npm, and CLI smoke tasks that lack them. Confirm both fields in Gradle task help. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#group_describe_tasks)
- [x] **T6 · Recommendation · Hardcoded plugin task names.** `command-line-tools/digger-cli/build.gradle.kts:142`. Prefer the Kotlin DSL, typed providers, or documented task API for Node setup and npm install wiring. Verify a clean Digger JS build still runs the required setup and install tasks. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#dont_hardcode_task_names)

### 3. Lazy release and plugin configuration `[patch]`

- [ ] **L1 · High · Early provider reads.** `tools/fingerprint-plugin/src/main/kotlin/com/zegreatrob/tools/FingerprintPlugin.kt:63`, `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt:148`, and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/NpmCliPlugin.kt:102`. Defer reads through typed inputs and resolve concrete command arguments during execution. Verify configured project selections, draft flags, and optional guide paths reach their consumers as concrete values. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#avoid_provider_get_outside_task_action)
- [x] **L2 · High · Eager release asset inspection.** `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt:169`. Keep the asset collection as a declared input and construct upload arguments during execution. Test empty assets and generated assets with a stubbed upload command, including producer ordering. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#avoid_eager_file_collection_apis)
- [x] **L3 · High · Project access during npm publish.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/NpmCliPlugin.kt:164`. Pass the snapshot decision or command arguments through task inputs. With a stubbed npm command, verify both snapshot and release arguments and configuration-cache reuse. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#dont_access_project_instance_inside_task)
- [ ] **L4 · High · `afterEvaluate` hooks.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/{publish.gradle.kts,testmints.gradle.kts,library.gradle.kts,reports.gradle.kts,NpmCliPlugin.kt}` and `tools/{certifier-plugin,digger-plugin,fingerprint-plugin,tagger-plugin}/build.gradle.kts`. Replace callbacks with plugin reactions and lazy collection configuration. Recheck all hooks and validate POMs, Javadoc artifacts, signing order, BOM wiring, report collection, and npm metadata before closing. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#avoid_after_evaluate)
- [x] **L5 · Medium · Plugin prerequisites.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/reports.gradle.kts:13`. Declare the `base` prerequisite or configure lazily when it is applied. Verify report setup through the supported plugin application paths. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#dont_assume_plugin_order)

### 4. Repositories and dependencies `[none]`

- [x] **D1 · Medium · Repository location.** `build.gradle.kts:1`, `tools/build.gradle.kts:3`, and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/versioning.gradle.kts:6` illustrate project-level repository declarations. Move dependency repositories to each build's settings. Verify dependency and plugin resolution in the root composite and each included build, and preserve publishing destinations. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#set_up_repositories_in_settings)
- [x] **D2 · Medium · Repository content filtering.** `settings.gradle.kts:1` and `tools-plugins/build.gradle.kts:7` illustrate unfiltered lists and the duplicate Plugin Portal. Deduplicate and filter repositories. Verify plugin markers and library coordinates still resolve from the intended repositories with an empty dependency cache. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#use_content_filtering)
- [x] **D3 · Medium · Redundant dependencies.** `tools-tests/digger-core-test/build.gradle.kts:16`, `tools/digger-gradle/build.gradle.kts:11`, and `tools/digger-core/build.gradle.kts:23`. Remove redundant Testmints declarations and confirm whether the direct Git adapter API edge is required. Compare dependency reports and compile consumers before and after. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#avoid_duplicate_dependencies)
- [x] **D4 · Recommendation · Catalog aliases.** `tools/gradle/libs.versions.toml:2` and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/testmints.gradle.kts:7`. Rename repetitive aliases and update both generated accessor uses and string lookups in all affected builds. Verify catalog resolution and BOM injection with the new names. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#name_version_catalog_entries)
- [x] **D5 · Recommendation · Explicit Kotlin stdlib.** `tools-plugins/build.gradle.kts:29`, `command-line-tools/digger-cli/build.gradle.kts:97`, and the strict pins in `tools/{digger-json,tagger-json}/build.gradle.kts`. Inventory explicit stdlib declarations and remove redundant ones. Verify resolved versions on affected targets and preserve necessary alignment before removing strict pins. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#dont_depend_on_kotlin_stdlib)

### 5. Build structure and reproducibility `[patch]`

- [x] **S1 · Medium · Imperative plugin application.** `tools-tests/build.gradle.kts:20`. Apply the test conventions through consuming projects' `plugins` blocks. Verify JVM and multiplatform test setup, including Testmints BOM placement. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#use_the_plugins_block)
- [ ] **S2 · Medium · Duplicated conventions.** `command-line-tools/build.gradle.kts:40`, `tools-tests/build.gradle.kts:47`, and the checksum setups in `command-line-tools/{digger-cli,tagger-cli}/build.gradle.kts`. Consolidate shared lifecycle, Yarn, checksum, and packaging logic in `tools-plugins`. Verify each behavior has one implementation and both CLIs still produce the same deliverables. [Gradle](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#use_convention_plugins)
- [ ] **S3 · Medium · Sources at an included build root.** `tools-plugins/build.gradle.kts:13` and `tools-plugins/src/main/`. Move convention and npm plugin sources to a subproject of the included build. Verify all existing plugin IDs resolve from the root composite and independently from the consuming builds. [Gradle](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#no_source_in_root)
- [x] **S4 · Medium · Unpinned CLI toolchains.** `command-line-tools/{digger-cli,tagger-cli}/build.gradle.kts:60`. Pin JDK 21 for both CLI archive producers. Verify the selected toolchain and compare JVM distribution SHA-256 hashes across clean builds with different launcher JDKs. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#builds_should_be_reproducible)
- [x] **S5 · Recommendation · Unstable API inventory.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/jvm.gradle.kts:19` and `tools/fingerprint-plugin/build.gradle.kts:1`. Record the experimental Kotlin APIs and unstable Gradle APIs the build relies on, why they are needed, and what to recheck on upgrades. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#consider_use_of_incubating_apis_carefully)
- [x] **A2 · Advisory · Isolated publishing.** `tools/build.gradle.kts:53` and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/publish.gradle.kts:6` establish publication scope. Verify the publishing environment starts fresh and isolated with build caching disabled and without reused outputs. Record the configuration evidence and close any gap without triggering an external publication during validation. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#build-published-artifacts-securely)

### Closeout

- [x] Obtain explicit authorization for the mandatory final refactor subagent in the thread that will spawn it. Record the response in Implementation Notes.
- [ ] Complete the final refactor review of all remediation commits and files per [REFACTOR_AGENT.md](../context/REFACTOR_AGENT.md). Rerun applicable detection checks and the full Gradle check.
- [ ] Confirm the completed remediation is pushed, then move this card to `agents.d/work_completed/`.

## Current State

- **Date** 2026-11-12
- **Branch and source baseline commit** `main`, `02d1f12f` (pushed to `origin/main`; this handoff note follows it).
- **Last verified commit** `02d1f12f` (focused NPM tests and `./gradlew check -q --console=plain` passed before commit).
- **Uncommitted work** None after this handoff note is committed.
- **Status** W1, W2, W3, A1, T1, T2, T3, T5, T6, L2, L3, L5, and A2 pushed. L1 is partially complete. T4, Git test environment isolation, D1, D2, D3, D4, D5, S1, S4, and S5 pushed. L4 library ordering partially complete.
- **Blockers** None recorded.
- **Next** Investigate a task-registration callback as a later Kotlin/Node hook, or retain a documented narrow `afterEvaluate` exception. AR1a and the early task registration are pushed; AR1/AR2 remain open.

## Implementation Notes

### 2026-11-12 · Lazy NPM target experiment

- Added a TestKit regression applying the NPM plugin before Kotlin, configuring `copyReadme` during project evaluation, then generating package metadata twice with the configuration cache. It failed first because `copyReadme` was not registered until `afterEvaluate`; early registration via `pluginManager.withPlugin` made it pass. The fixture needed a normal Maven Central dependency repository to run `jsPackageJson`.
- Tried replacing `afterEvaluate` with `KotlinMultiplatformExtension.targets.withType<KotlinJsIrTarget>().configureEach`. The real Digger CLI build failed while configuring its JS target because the `kotlinNodeJs` extension was not registered yet. Restored the existing hook; the focused NPM suite passes again. The target callback is too early for this package setup, but a later task callback remains an untested alternative. AR1/AR2 remain open.
- `./gradlew :tools-plugins:formatKotlin -q --console=plain`, `./gradlew :tools-plugins:test --tests '*NpmCliPluginTest' -q --console=plain`, and `./gradlew check -q --console=plain` passed after restoring the working hook. `git diff --check` passed.
- The source, regression test, and AR1a decision were committed as `02d1f12f` and pushed to `origin/main`. GitHub reported bypass of its required Build status check.

### 2026-11-12 · AR1a decision

- The user agreed that the dedicated cacheable package-metadata task is disproportionate for this build and asked for the smaller lazy-configuration alternative. AR1a is closed as "will not implement" for the reasons above; AR1 and AR2 remain open pending behavior tests.

### 2026-11-12 · NPM output ownership investigation

- Replaced string `dependsOn` references in `NpmCliPlugin.kt` with task providers. `./gradlew :tools-plugins:test --tests '*NpmCliPluginTest' -q --console=plain` and `./gradlew check -q --console=plain` passed. AR8 remains open until all references are checked and this slice is committed and pushed.
- Inspected Kotlin Gradle plugin 2.4.21 sources. `KotlinPackageJsonTask.packageJson` is annotated `@OutputFile`, and its action calls `KotlinCompilationNpmResolution.createPackageJson`, which writes directly to the Kotlin npm project directory. Changing the task's output property alone would not redirect that write.
- Considered overwriting the same `package.json` from a second task; rejected because two tasks would claim the same output and break the unique-output requirement. Considered redirecting `KotlinPackageJsonTask.packageJson`; rejected because the action ignores that property when writing. Considered publishing the tar archive directly; that changes the required npm publish arguments.
- The viable route is separate staging: generate finalized metadata as its own output, have the README/license producers consume it, then assemble a complete package in an independently owned staging directory for `jsCliTar`, `jsLink`, and `jsPublish`. Keep the existing task names and npm command arguments; verify package contents and configuration-cache reuse before accepting the change. This route has not yet been implemented.

### 2026-11-12 · Resume and baseline

- `git log -1 --oneline` reported `7838f055 [patch] Force Maven Central republish`; `git status --short --branch` reported a clean `main` branch one commit ahead of `origin/main` before this card update.
- `./gradlew check -q --console=plain` passed. Its output reported the expected local branch and release-branch warnings.
- Inspected the NPM plugin and Kotlin Gradle plugin sources before changing its lifecycle wiring. Kotlin's `jsPackageJson` task owns its package JSON output, while the current npm archive and publish tasks consume that same package directory. AR1a needs distinct output ownership and an explicit producer chain; no NPM implementation was changed during this resume.

### 2026-11-11 · Initial lazy-convention slice

- Replaced the `afterEvaluate` blocks in `tools-plugins` Testmints dependency
  injection and the publishing convention with `configureEach`, preserving the
  inline POM configuration required by the publishing incident constraint.
- The first implementation passed `:tools-plugins:compileKotlin` but failed the
  full check because BOM versions were not available to JS dependency
  resolution. The dependency is now materialized from the version-catalog
  provider before being added to each matching configuration; this is dependency
  configuration, not task execution.
- Validation: `./gradlew :tools-plugins:compileKotlin -q --console=plain`
  passed; `./gradlew :tools-plugins:test --tests '*NpmCliPluginTest' -q
  --console=plain` passed; the initial `./gradlew check -q --console=plain`
  failed on missing Testmints versions; after correction and formatting,
  `./gradlew check -q --console=plain` passed.
- AR1 remains open because `NpmCliPlugin.kt`, included-build plugin scripts,
  and remaining module build scripts still contain `afterEvaluate` hooks.

### 2026-11-11 · Task metadata reconciliation

- Confirmed `DiggerGuideTask` and `TaggerGuideTask` already receive `help`
  groups and descriptions at registration. Added the missing group and
  description to `clearDashboardTestSummary`.
- Corrected AR4: the cited `tools/build.gradle.kts:64` block is publishing
  repository configuration, not dependency repository configuration, so no
  project-level dependency repository migration is required there.
- Validation: `./gradlew :tools-plugins:dashboard-test-summary:test -q
  --console=plain` and `./gradlew check -q --console=plain` passed.

### 2026-11-11 · Included-build publication hooks

- Replaced the inline publication `afterEvaluate` hooks in the Certifier,
  Digger, Fingerprint, and Tagger build scripts with
  `publishing.publications.withType<MavenPublication>().configureEach`.
  POM metadata remains inline and unchanged; the NPM and remaining lifecycle
  hooks are still open under AR1/L4.
- Validation: `./gradlew formatKotlin -q --console=plain` and
  `./gradlew check -q --console=plain` passed.

### 2026-11-11 · NPM lifecycle solution decision

- A direct `pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform")`
  replacement was tested and reverted. The Kotlin plugin is present, but the
  `kotlinNodeJs` extension is not registered when the callback runs.
- Evaluated alternatives: task/target lifecycle callbacks, a narrowly scoped
  compatibility bridge, splitting the plugin into setup/finalize plugins, and a
  dedicated package-metadata task. The selected approach is the dedicated task:
  it gives package metadata explicit inputs/outputs and avoids relying on
  undocumented Kotlin/Node extension timing.
- Implementation must preserve the existing NPM DSL, task entry points,
  generated package contents, publish arguments, and configuration-cache tests.

### 2026-11-11 · D2 repository filtering

- This is repository configuration with no intended output change. Test the real composite with a fresh Gradle user home, since a synthetic fixture cannot cover the plugin markers and transitive dependencies used across these builds.
- Maven Central does not host `org.jmailen.gradle:kotlinter-gradle:5.7.0`; the convention build retains the Plugin Portal only for that group. The other dependency repository lists use Maven Central alone. Plugin-resolution repositories remain separate because plugin markers and implementations span multiple groups.

### 2026-11-11 · D1 repository migration preparation

- The L4 library signing-order cleanup was pushed as signed commit `c1e64b42`. The publishing convention’s POM block remains subject to the Sonatype incident constraint in `agents.d/context/GRADLE_PLUGIN_CONSTRAINTS.md`.
- Kotlin’s Node setup temporarily adds an Ivy repository to a project during execution, so global `FAIL_ON_PROJECT_REPOS` or `PREFER_SETTINGS` may break Node provisioning. Preserve Gradle’s default repository mode while moving authored dependency repositories to settings, and verify Node setup along with JVM dependency resolution.
- Moved authored dependency repositories to the five builds' settings, leaving publishing destinations intact. A convention fixture first failed under `FAIL_ON_PROJECT_REPOS` and then passed after migration. Regenerating stale precompiled-script accessors resolved a local compilation failure. The full `./gradlew check -q --console=plain` and Kotlin Node setup passed; no top-level project repository blocks remain. Dependency caches were warm; empty-cache verification belongs to D2.
- Pushed signed commit `c9594b5a`; GitHub reported bypassing its required Build status check. Each build also configured independently with `help`.

### 2026-11-11 · D2 repository filtering

- Removed duplicate dependency-resolution Plugin Portal entries from the root, CLI, tools, and test builds. The convention build retains a filtered Plugin Portal repository for `org.jmailen.gradle`, whose Kotlinter plugin is not available from Maven Central.
- Fresh-cache root resolution and `./gradlew check -q --console=plain` passed after retrying one corrupted temporary Node archive. Plugin-management repositories remain separate from dependency repositories so plugin markers continue to resolve.

### 2026-11-11 · D4 catalog aliases

- Renamed repetitive Testmints aliases to `testmints.*` in the tools, tests, and CLI catalogs, and updated all generated accessor usages plus the convention plugin's BOM lookup. The dependency coordinates and versions are unchanged.
- The focused tools, tests, CLI, and convention checks passed, followed by the full `./gradlew check -q --console=plain`.

### 2026-11-11 · S5 unstable API inventory

- Added `agents.d/context/GRADLE_UNSTABLE_APIS.md` documenting the Kotlin compiler-options opt-ins, Gradle plugin API suppression, and Kotlin/JS task-name coupling, with upgrade checks for each.
- Pushed signed commit `8fac0c51`; GitHub again reported that its required Build status check was bypassed.

### 2026-11-11 · L4 npm lifecycle investigation

- A callback on `org.jetbrains.kotlin.multiplatform` was tested as a replacement for the npm plugin's `afterEvaluate` hook. It ran before the Kotlin JS target registered the `kotlinNodeJs` extension and failed all three npm convention tests. The change was reverted without committing; the hook remains open until it can be attached to the JS target lifecycle itself.

### 2026-11-11 · A2 isolated publishing

- Ran `:tools:git-adapter:publishToMavenLocal --dry-run` with a fresh temporary Gradle user home, `--no-build-cache`, and `--no-configuration-cache`. The complete local publication graph resolved, including POM, metadata, sources, Javadoc, signing, and all three publications. No external repository or publication was contacted.

### 2026-11-11 · S2 convention extraction investigation

- A shared precompiled convention was tested for the duplicate lifecycle and Yarn task wiring. Applying an included-build convention placed Kotlinter on the classpath with an unknown version, which conflicted with the versioned Kotlinter requests in CLI subprojects. The experiment was removed without committing source changes; S2 remains open until the convention boundary can preserve plugin resolution.

### 2026-11-11 · L4 library signing order

- S1 was pushed as signed commit `efe87aea`. The library convention’s `afterEvaluate` block only configures ordering between signing and publishing tasks. A baseline dry run of Git Adapter’s JVM publication and JS signing tasks placed both sign tasks before publication.
- This slice preserves task behavior and output. Compare the real Gradle task graph before and after rather than adding a test that mirrors the configuration. Publishing is exercised with dry-run only.
- Replaced the hook with lazy task configuration. The dry-run order remained JS signing, JVM signing, then JVM publication. Git Adapter's generated JVM POM retains its name, description, URL, license, developer, and SCM fields, and its Javadoc JAR was generated. `./gradlew check -q --console=plain` passed. This partial L4 slice awaits commit and push.

### 2026-11-11 · S1 explicit test-suite convention

- S4 was pushed as signed commit `823823d2`. A new TestKit test failed because the test-suite convention did not exist. The convention applies lint, reports, and Testmints and sets the shared-main BOM option. The seven test modules now apply it explicitly; root cross-project Kotlin compiler configuration remains.
- The convention TestKit test passed with the Kotlin multiplatform plugin applied after the convention and verifies the BOM, report task, and lint task. Dependency insight confirms the JVM plugin test module still receives the Testmints BOM. `./gradlew check -q --console=plain` passed. S1 awaits commit and push.

### 2026-11-11 · S4 CLI toolchain preparation

- D5 was pushed as signed commit `7670e641`. S4 changes build configuration and archive outputs. Verify the selected compiler and distribution hashes with independent builds launched under JDK 21 and JDK 22, followed by the full check. These Gradle build and artifact comparisons exercise the behavior directly; a new test that merely checks the configuration would duplicate the implementation.
- Pinned both Kotlin JVM toolchains to JDK 21. Under a JDK 22 launcher, Kotlin compilation logged the JDK 21 home for both CLIs. Clean CLI-module builds with the build cache disabled under JDK 21 and JDK 22 produced matching distribution SHA-256 values, `526a9f3727f0866ec02e8942af0e7b43c76a1606fe714142e65d40399ab75a3c` for Tagger and `61f7f47f9ba4d593e0dfd5b3f103b9e5b6ecc60379b58d1e2d9e8b8c8fb48584` for Digger. The other included modules retained their existing outputs. Full validation is running.
- The first full check failed because the shared Tagger and Digger test libraries had been compiled for JDK 22 while the CLI test JVMs now run on JDK 21. Pinned both shared test libraries to JDK 21. The focused JVM test tasks and `./gradlew check -q --console=plain` passed afterward. S4 awaits commit and push.

### 2026-11-11 · D5 Kotlin standard library dependencies

- D3 was pushed as signed commit `75f316ef`. Removed explicit Kotlin stdlib dependencies from the convention build, Git test library, and Digger CLI test source set. The JSON test strict pins remain as dependency constraints.
- Removing the convention build dependency alone selected Kotlin 2.4.10 through Kotlinter instead of 2.4.21, so an embedded-Kotlin version constraint retains the prior selection. Dependency insight confirms 2.4.21 on affected JVM and JS configurations and retains the JSON strict pin. The unused Digger CLI catalog alias was removed. `./gradlew check -q --console=plain` passed. D5 awaits commit and push.

### 2026-11-11 · D3 redundant dependencies

- T4 and Git test isolation were pushed as agent-signed commit `7db640ea`. Digger Gradle already receives Git Adapter through Digger Core’s API. The generated JVM POM confirms that transitive compile dependency.
- Removed the redundant direct API edge and duplicate JVM Testmints declarations. Dependency insight still resolves both Testmints JVM variants at 12.6.70, and Digger Gradle still resolves Git Adapter through Digger Core. Generated JVM POMs retain that transitive compile dependency. `./gradlew check -q --console=plain` passed. D3 awaits commit and push.

### 2026-11-11 · Git test environment isolation

- The agent signing configuration uses `GIT_CONFIG_COUNT` overrides. Tagger identity-less tests inherited those overrides despite isolating global and system config, causing false successes. A shared prerequisite assertion failed with count 6 before the fix.
- Test tasks that isolate global and system Git config now also set `GIT_CONFIG_COUNT=0` and clear `GIT_CONFIG_PARAMETERS`. The focused JVM regression, the Tagger JavaScript suite, and `./gradlew check -q --console=plain` passed with signing still configured for the parent process.

### 2026-11-11 · T4 report location wiring

- Resumed at `5f0d8528` after the dashboard plugin was relocated to `tools-plugins/dashboard-test-summary`. The baseline `./gradlew check -q --console=plain` passed. Draft publication was committed as `16471e6f`.
- Replaced the nested output `get()` with `flatMap` on the report location-only provider and attached the producing test task using `builtBy`. This avoids Gradle’s report-property producer error while retaining task execution. `./gradlew :tools-plugins:dashboard-test-summary:test -q --console=plain` passed, including custom output locations, disabled XML, failing tests, and configuration-cache reuse. The full check passed after isolating Git test configuration.
- The user supplied a dedicated Codex SSH signing key and identity under the machine-wide agent instructions. Signing was verified with `ssh-keygen -Y sign`; subsequent commits must use that configuration and the human co-author trailer.

### 2026-11-11 · L1 draft publication decision

- Release creation arguments were pushed as `dad5399b`. A generated non-draft setting reproduced the publication task’s eager draft read. Publication now evaluates the declared input at execution and skips a non-draft release. Its configuration-time `enabled` value remains true until that execution-time check.
- Targeted tagger unit tests and generated draft/non-draft functional tests passed with a stubbed `gh` executable. An initial targeted run was interrupted by a Gradle daemon stop; the retry passed. Full validation is in progress.

### 2026-11-11 · L1 draft publication validation

- `./gradlew :tools-tests:tagger-plugin-test:functionalTest --tests '*AdditionalTasksFunctionalTest*' -q --console=plain` passed for generated draft and non-draft settings, including configuration-cache reuse.
- `./gradlew check -q --console=plain` passed. The release task remains eligible during configuration and skips at execution when the generated draft setting is false; draft publication executes with the resolved setting and expected `gh release edit` arguments.
- This slice is ready to commit. Remaining L1 provider reads and the other unchecked audit items remain open.

### 2026-11-11 · L1 release creation arguments

- L2 was pushed as `f4f43063`. A generated draft-setting regression failed during task configuration before the release creation change. A typed argument provider now reads the setting when the release task executes. The targeted functional test passed with configuration-cache reuse and a stubbed `gh` executable, and the tagger unit suite passed.
- The remaining L1 provider reads and the draft publication task are still open. `./gradlew check -q --console=plain` passed after correcting a Kotlin lint error. This partial slice awaits commit and push.

### 2026-11-11 · L2 release asset wiring

- L5 was pushed as `82e8c09f`. A generated file-tree asset regression showed that eager emptiness checks skipped upload before its producer ran. The task now retains the producer dependency through declared inputs and evaluates emptiness at execution. An argument provider builds the upload command from resolved inputs, passing filenames as shell arguments so spaces survive.
- The generated-asset and empty-asset functional tests passed, including configuration-cache reuse. The upload task remains eligible for an empty collection and is skipped during execution, so its configuration-time `enabled` value differs from the old behavior. The targeted unit suite and `./gradlew check -q --console=plain` passed. L2 awaits commit and push.

### 2026-11-11 · L5 reports convention prerequisite

- L3 was pushed as `e6d6e6dc`. A TestKit fixture applying the reports convention without `base` failed because `check` was absent. Declaring the base plugin in the convention made that test and the report suite pass. `./gradlew check -q --console=plain` passed. L5 awaits commit and push.

### 2026-11-11 · L3 npm publish action

- T6 was pushed as `2b28624e`. A TestKit regression reproduced the npm task configuration-cache failure from capturing `Project`; it passed after capturing the snapshot decision during configuration instead. Snapshot and release tests use a stub executable, verify the exact arguments, and rerun with configuration-cache reuse. No package was published.
- The first test attempt reached the real npm executable with `--dry-run` and failed because the fixture lacked a package manifest. The fixture now explicitly redirects the executable to a stub before testing either publish mode.
- The snapshot decision is fixed when the npm plugin configures the publish task after evaluation. `./gradlew -p tools-plugins test --tests '*NpmCliPluginTest' -q --console=plain` and `./gradlew check -q --console=plain` passed. L3 awaits commit and push.

### 2026-11-11 · T6 Node and npm task wiring

- T1 was pushed as `ced64363`. A clean Digger JavaScript package build passed after replacing local name-based task lookups with Kotlin task type configuration. Included-build task paths remain strings because the composite-build API requires task paths.
- A dry run without the wiring omitted the included build’s Node and npm setup tasks. After type-based wiring, `./gradlew :command-line-tools:kotlinNpmInstall --dry-run --console=plain` included both. Full validation is in progress.
- `./gradlew check -q --console=plain` passed. Gradle printed a local FSEvents stream warning but completed successfully. T6 awaits commit and push.

### 2026-11-11 · T1 distinct collection directories

- The user chose a comprehensible layout that retains directory distinctions. Per project, collection now separates `results/reports/` from `results/test-results/`. The root collector separates included builds under `tools/` and `tools-tests/` and synchronizes its generated output to remove obsolete layout entries. This intentionally changes the collected artifact layout.
- A TestKit regression with matching relative XML filenames failed before the change and passed afterward. The other report tests passed. `./gradlew collectResults -q --console=plain` produced the expected build and project directories, and `./gradlew check -q --console=plain` passed. T1 awaits commit and push.
- A1 is verified. GitHub Actions run 42897436491 completed successfully on `950753b0`, including both Update wrapper and Validate updated wrapper steps. T5 was pushed as `5d00a71b`.

### 2026-11-11 · T5 task metadata

- Added groups and descriptions to report, npm, and CLI tasks, including the included-build `collectResults` aggregators. This is metadata with no intended build output change. Gradle task help confirmed the report and JVM smoke task metadata, and `./gradlew check -q --console=plain` passed. T5 awaits commit and push.

### 2026-11-11 · T4 dashboard report outputs

- T3 was pushed as `64976f22`. A direct `flatMap` of Gradle’s report output property failed because the report property has no associated task. Wrapping that output without `builtBy` lost test execution. An explicitly built file collection passed the tests but still calls `get()` from a provider, so it does not resolve the catalog finding. The production code remains unchanged.
- A disabled-XML test failed as expected when the reporting guard was temporarily removed, and passed after restoring the guard. `./gradlew :tools:dashboard-test-summary:test -q --console=plain` passed, including custom XML locations and configuration-cache reuse. `./gradlew check -q --console=plain` passed. T4 remains open.

### 2026-11-11 · T3 task dependency wiring

- T2 was pushed as `ec48fded`. Its focused TestKit test reproduced the configuration-time directory creation and passed after the fix.
- T3 removes redundant fingerprint task edges and carries included-build producer dependencies on file inputs. Existing aggregate functional tests pass. Both JVM CLI smoke tasks rebuilt generated templates and distributions from clean outputs and succeeded.
- `./gradlew :tools:fingerprint-plugin:functionalTest --tests '*FingerprintAggregate*' -q --console=plain` and `./gradlew check -q --console=plain` passed. After cleaning both template tasks, requesting both `compileKotlinJs` tasks rebuilt the templates and passed. T3 awaits commit and push.

### 2026-11-11 · T2 report directory creation

- A TestKit test showed that `help` created `build/test-output` during configuration. The test failed before removal of the `afterEvaluate` directory creation and passed afterward. The existing report collection test confirms that collecting actual files creates the destination as needed.
- `./gradlew -p tools-plugins test --tests '*ReportsPluginTest' -q --console=plain` and `./gradlew check -q --console=plain` passed. T2 awaits commit and push.

### 2026-11-11 · T1 npm staging

- Resumed at `61c7df93` with a clean working tree. The required baseline `./gradlew check -q --console=plain` passed.
- A TestKit regression showed that cleaning `copyReadme` also removed the staged license. It failed at the license assertion before the fix and passed after assigning individual output files to the README and license tasks.
- `./gradlew :command-line-tools:tagger-cli:jsCliTar :command-line-tools:digger-cli:jsCliTar -q --console=plain` passed. Both archives contain README and LICENSE bytes matching their sources. A scan of existing report and test-result directories found no colliding relative file paths.
- `./gradlew check -q --console=plain` passed after the npm change. Existing report collections can still collide if a report and a test result have the same relative filename, so that case remains open.
- The report-copy changes were committed and pushed as `950753b0`. T1 remains open until npm changes are validated, committed, and pushed.

### 2026-11-10 · Final review authorization

- The user explicitly authorized one subagent for the final refactor review after implementation is complete.

### 2026-11-10 · T1 report-copy progress

- A TestKit regression demonstrated that `cleanCopyReportsToRootDirectory` deleted copied test results. The focused test failed at that assertion before the fix and passes with per-file task outputs. Collected paths and both existing task names are retained.
- `./gradlew -p tools-plugins test --tests '*ReportsPluginTest' --console=plain` passed after the change. The full `./gradlew check --console=plain` passed. The initial quiet run was interrupted for diagnostics before the successful rerun.
- T1 remains open for npm staging and for checking colliding relative paths. Changes await commit and push.

### 2026-11-10 · W3 preparation

- W3 is a build configuration change with no intended output impact. Validate the daemon JVM arguments directly and run the full check rather than adding a test that only mirrors the property.
- W3 validation passed. `./gradlew help --info --console=plain` started daemon 89893, whose log records both `-Xmx3g` and `-Dfile.encoding=UTF-8`. `./gradlew check -q --console=plain` passed. The change awaits commit and push.

### 2026-11-10 · Wrapper remediation

- The work card and IDEA changes were committed and pushed as `4d42e320`. GitHub reported that the push bypassed the required Build status check.
- W1 and W2 use the Gradle wrapper task to select Gradle 9.9.0 `-bin` with the published SHA-256 `0b00b0ffe07c4d4789c12d4935a44a7057d55774b7540bd4a3f76f336deb177a`. The wrapper task ran twice. Configuration-only changes are validated through wrapper bootstrap and the build check rather than a new test.
- Baseline and post-change `./gradlew --console=plain tasks` passed. `./gradlew check -q --console=plain` passed after the change. An empty Gradle user home successfully downloaded and verified the binary distribution with `./gradlew --version`.
- The scheduled wrapper update now selects the setup action's concrete Gradle version, fetches its binary distribution checksum, and passes both to the wrapper task. GitHub Actions execution remains to verify after the workflow is pushed.
- A1 local verification passed. `shasum -a 256 gradle/wrapper/gradle-wrapper.jar` matched Gradle's published 9.9.0 wrapper JAR checksum, `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8`. The update job now validates the generated wrapper JAR before opening a pull request.

### 2026-11-10 · Audit captured

- Static baseline used the bundled September 23, 2026 Gradle 9.9.0-nightly catalog. The wrapper points to `9.9.0-all` and has no distribution checksum. Build caching and configuration caching were already enabled.
- Append dated notes newest first. Record completed IDs, decisions, discovered constraints, and commit references. Before pausing, refresh Current State with the latest verified SHA, pending changes, blockers, and next unchecked item. On resume, verify `git log -1` and `git status`, run the full check, and record the handoff.

## Validation

- **Card inventory** Historical inventory: 24 findings (6 high, 12 medium, 6 recommendations) and 2 advisories. The reconciliation above adds 8 current audit work items, with overlap intentionally preserved until each detection recipe is rerun.
- **Per slice** Rerun the catalog detection for every affected ID and its acceptance check. For behavior changes, show the focused check failing for the expected reason before the fix. Run `./gradlew check -q --console=plain` before marking the slice verified. Log the ID, exact commands, result, and SHA here.
- **Publication changes** Generate and compare marker and implementation POMs, inspect local artifacts for required Javadoc and metadata, and check the release task graph with `--dry-run`.
- **Baseline status** Gradle checks and publication validation pending. Card creation is documentation-only.
