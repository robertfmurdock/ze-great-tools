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

### 1. Wrapper and build defaults `[none]`

- [x] **W1 · High · Distribution checksum.** `gradle/wrapper/gradle-wrapper.properties:3`. Pin the published SHA-256 for the archive in `distributionUrl`. Verify the URL and digest pair against Gradle's published checksums and exercise wrapper bootstrap with an empty distribution cache. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#validate_gradle_checksum)
- [x] **W2 · Recommendation · Binary distribution.** `gradle/wrapper/gradle-wrapper.properties:3`. Use the wrapper skill to regenerate the current version as `-bin` and pin its digest in the same change. Verify the generated URL, SHA-256, and reported Gradle version. W1 and W2 can share a commit if the resulting binary archive is pinned. [Gradle](https://docs.gradle.org/current/userguide/best_practices_performance.html#prefer_bin_distribution)
- [x] **W3 · Medium · UTF-8.** `gradle.properties:4`. Append `-Dfile.encoding=UTF-8` to the existing daemon JVM arguments. Verify the daemon sees UTF-8 and the existing heap setting is retained. [Gradle](https://docs.gradle.org/current/userguide/best_practices_performance.html#use_utf8_encoding)
- [ ] **A1 · Advisory · Wrapper JAR verification.** `gradle/wrapper/gradle-wrapper.jar`. Compare its digest against the trusted published checksum or byte-compare with a wrapper regenerated using a trusted Gradle installation. Record the result and verify the upgrade workflow checks future wrapper JAR changes. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#validate_wrapper_checksum)

### 2. Task wiring and outputs `[patch]`

- [ ] **T1 · Medium · Unique outputs.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/reports.gradle.kts:14` and `NpmCliPlugin.kt:120` in the same directory. Consolidate producers or stage distinct outputs. Verify one owner per destination and compare collected report paths and npm package contents with the baseline. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#use_unique_output_files_and_directories)
- [ ] **T2 · High · Configuration-time filesystem work.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/reports.gradle.kts:29`. Move report directory creation to execution. From a clean output tree, verify `help` leaves the report output absent and `collectResults` creates it as needed. [Gradle](https://docs.gradle.org/current/userguide/best_practices_performance.html#avoid_computations_in_configuration_phase)
- [ ] **T3 · Medium · Explicit action-task dependencies.** `command-line-tools/{digger-cli,tagger-cli}/build.gradle.kts` at the template-copy and JVM smoke-test wiring, and `tools/fingerprint-plugin/src/main/kotlin/com/zegreatrob/tools/FingerprintPlugin.kt:103`. Connect producer outputs to consumer inputs and remove redundant edges. Request the consumers alone and verify the required templates, distributions, and fingerprints are built first. Preserve lifecycle and mandatory ordering semantics. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#avoid_depends_on)
- [ ] **T4 · Medium · Lazy output composition.** `tools/dashboard-test-summary/src/main/kotlin/com/zegreatrob/tools/dashboard/DashboardTestSummaryPlugin.kt:61`. Compose JUnit XML output locations with `flatMap`. Verify the collector reads a customized XML directory and respects disabled XML reporting. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#map_versus_flatmap)
- [ ] **T5 · Recommendation · Task metadata.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/{reports.gradle.kts,NpmCliPlugin.kt}` and `command-line-tools/tagger-cli/build.gradle.kts:111`. Add groups and descriptions to report, npm, and CLI smoke tasks that lack them. Confirm both fields in Gradle task help. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#group_describe_tasks)
- [ ] **T6 · Recommendation · Hardcoded plugin task names.** `command-line-tools/digger-cli/build.gradle.kts:142`. Prefer the Kotlin DSL, typed providers, or documented task API for Node setup and npm install wiring. Verify a clean Digger JS build still runs the required setup and install tasks. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#dont_hardcode_task_names)

### 3. Lazy release and plugin configuration `[patch]`

- [ ] **L1 · High · Early provider reads.** `tools/fingerprint-plugin/src/main/kotlin/com/zegreatrob/tools/FingerprintPlugin.kt:63`, `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt:148`, and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/NpmCliPlugin.kt:102`. Defer reads through typed inputs and resolve concrete command arguments during execution. Verify configured project selections, draft flags, and optional guide paths reach their consumers as concrete values. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#avoid_provider_get_outside_task_action)
- [ ] **L2 · High · Eager release asset inspection.** `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt:169`. Keep the asset collection as a declared input and construct upload arguments during execution. Test empty assets and generated assets with a stubbed upload command, including producer ordering. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#avoid_eager_file_collection_apis)
- [ ] **L3 · High · Project access during npm publish.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/NpmCliPlugin.kt:164`. Pass the snapshot decision or command arguments through task inputs. With a stubbed npm command, verify both snapshot and release arguments and configuration-cache reuse. [Gradle](https://docs.gradle.org/current/userguide/best_practices_tasks.html#dont_access_project_instance_inside_task)
- [ ] **L4 · High · `afterEvaluate` hooks.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/{publish.gradle.kts,testmints.gradle.kts,library.gradle.kts,reports.gradle.kts,NpmCliPlugin.kt}` and `tools/{certifier-plugin,digger-plugin,fingerprint-plugin,tagger-plugin}/build.gradle.kts`. Replace callbacks with plugin reactions and lazy collection configuration. Recheck all hooks and validate POMs, Javadoc artifacts, signing order, BOM wiring, report collection, and npm metadata before closing. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#avoid_after_evaluate)
- [ ] **L5 · Medium · Plugin prerequisites.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/reports.gradle.kts:13`. Declare the `base` prerequisite or configure lazily when it is applied. Verify report setup through the supported plugin application paths. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#dont_assume_plugin_order)

### 4. Repositories and dependencies `[none]`

- [ ] **D1 · Medium · Repository location.** `build.gradle.kts:1`, `tools/build.gradle.kts:3`, and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/versioning.gradle.kts:6` illustrate project-level repository declarations. Move dependency repositories to each build's settings. Verify dependency and plugin resolution in the root composite and each included build, and preserve publishing destinations. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#set_up_repositories_in_settings)
- [ ] **D2 · Medium · Repository content filtering.** `settings.gradle.kts:1` and `tools-plugins/build.gradle.kts:7` illustrate unfiltered lists and the duplicate Plugin Portal. Deduplicate and filter repositories. Verify plugin markers and library coordinates still resolve from the intended repositories with an empty dependency cache. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#use_content_filtering)
- [ ] **D3 · Medium · Redundant dependencies.** `tools-tests/digger-core-test/build.gradle.kts:16`, `tools/digger-gradle/build.gradle.kts:11`, and `tools/digger-core/build.gradle.kts:23`. Remove redundant Testmints declarations and confirm whether the direct Git adapter API edge is required. Compare dependency reports and compile consumers before and after. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#avoid_duplicate_dependencies)
- [ ] **D4 · Recommendation · Catalog aliases.** `tools/gradle/libs.versions.toml:2` and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/testmints.gradle.kts:7`. Rename repetitive aliases and update both generated accessor uses and string lookups in all affected builds. Verify catalog resolution and BOM injection with the new names. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#name_version_catalog_entries)
- [ ] **D5 · Recommendation · Explicit Kotlin stdlib.** `tools-plugins/build.gradle.kts:29`, `command-line-tools/digger-cli/build.gradle.kts:97`, and the strict pins in `tools/{digger-json,tagger-json}/build.gradle.kts`. Inventory explicit stdlib declarations and remove redundant ones. Verify resolved versions on affected targets and preserve necessary alignment before removing strict pins. [Gradle](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#dont_depend_on_kotlin_stdlib)

### 5. Build structure and reproducibility `[patch]`

- [ ] **S1 · Medium · Imperative plugin application.** `tools-tests/build.gradle.kts:20`. Apply the test conventions through consuming projects' `plugins` blocks. Verify JVM and multiplatform test setup, including Testmints BOM placement. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#use_the_plugins_block)
- [ ] **S2 · Medium · Duplicated conventions.** `command-line-tools/build.gradle.kts:40`, `tools-tests/build.gradle.kts:47`, and the checksum setups in `command-line-tools/{digger-cli,tagger-cli}/build.gradle.kts`. Consolidate shared lifecycle, Yarn, checksum, and packaging logic in `tools-plugins`. Verify each behavior has one implementation and both CLIs still produce the same deliverables. [Gradle](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#use_convention_plugins)
- [ ] **S3 · Medium · Sources at an included build root.** `tools-plugins/build.gradle.kts:13` and `tools-plugins/src/main/`. Move convention and npm plugin sources to a subproject of the included build. Verify all existing plugin IDs resolve from the root composite and independently from the consuming builds. [Gradle](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#no_source_in_root)
- [ ] **S4 · Medium · Unpinned CLI toolchains.** `command-line-tools/{digger-cli,tagger-cli}/build.gradle.kts:60`. Pin JDK 21 for both CLI archive producers. Verify the selected toolchain and compare JVM distribution SHA-256 hashes across clean builds with different launcher JDKs. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#builds_should_be_reproducible)
- [ ] **S5 · Recommendation · Unstable API inventory.** `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/jvm.gradle.kts:19` and `tools/fingerprint-plugin/build.gradle.kts:1`. Record the experimental Kotlin APIs and unstable Gradle APIs the build relies on, why they are needed, and what to recheck on upgrades. [Gradle](https://docs.gradle.org/current/userguide/best_practices_general.html#consider_use_of_incubating_apis_carefully)
- [ ] **A2 · Advisory · Isolated publishing.** `tools/build.gradle.kts:53` and `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/publish.gradle.kts:6` establish publication scope. Verify the publishing environment starts fresh and isolated with build caching disabled and without reused outputs. Record the configuration evidence and close any gap without triggering an external publication during validation. [Gradle](https://docs.gradle.org/current/userguide/best_practices_security.html#build-published-artifacts-securely)

### Closeout

- [x] Obtain explicit authorization for the mandatory final refactor subagent in the thread that will spawn it. Record the response in Implementation Notes.
- [ ] Complete the final refactor review of all remediation commits and files per [REFACTOR_AGENT.md](../context/REFACTOR_AGENT.md). Rerun applicable detection checks and the full Gradle check.
- [ ] Confirm the completed remediation is pushed, then move this card to `agents.d/work_completed/`.

## Current State

- **Date** 2026-11-11
- **Branch and baseline commit** `main` at `29fca6d5206eef625e58a24e02e7a797e9cc89a1`
- **Last verified commit** `8f092c6e` (npm staging slice).
- **Uncommitted work** T2 report directory fix, regression test, and card progress.
- **Status** W1, W2, and W3 pushed. T1 report and npm fixes pushed, with colliding report filenames still to resolve. T2 validated locally. Wrapper workflow execution and A1 remain to verify.
- **Blockers** None recorded.
- **Next** T1, separate overlapping report and npm task outputs.

## Implementation Notes

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

- **Card inventory** 24 findings (6 high, 12 medium, 6 recommendations) and 2 advisories.
- **Per slice** Rerun the catalog detection for every affected ID and its acceptance check. For behavior changes, show the focused check failing for the expected reason before the fix. Run `./gradlew check -q --console=plain` before marking the slice verified. Log the ID, exact commands, result, and SHA here.
- **Publication changes** Generate and compare marker and implementation POMs, inspect local artifacts for required Javadoc and metadata, and check the release task graph with `--dry-run`.
- **Baseline status** Gradle checks and publication validation pending. Card creation is documentation-only.
