# Dashboard Test Summaries

## Goal
Publish a Gradle project plugin that aggregates JUnit XML per build, with explicit top-level wiring for participating composite builds, and use it for Ze Great Tools' CI evidence.

## Constraints
- Semver intent: `[minor]` for the opt-in plugin, `[none]` for CI adoption.
- Discover enabled JUnit producers via declared outputs; share evidence through Gradle variants and composite substitution.
- Report the current workflow run, including failed assertions, without publishing incomplete or stale evidence.
- Small local commits safe to push independently. No PR, push, release, or Coupling migration in this work.

## Checklist
- [x] Review work card against the template and confirm the agreed scope.
- [x] Add and validate standalone XML-to-summary behavior.
- [x] Prove composite discovery, deduplication, configuration cache, and failure behavior.
- [x] Wire publication, fingerprinting, and consumer documentation. Validate and commit the opt-in plugin.
- [x] Separate the root wiring and strengthen the composite fixture before adoption.
- [x] Relocate the dashboard plugin into the shared `tools-plugins` build so CI resolves the current checkout like the other repository plugins.
- [x] Replace the settings-plugin design with a project plugin and explicit included-build aggregation wiring.
- [ ] Complete final refactor review and move card after the work is pushed.

## Current State
- **Commit SHA**: 86f21d7b adds the plugin. The next checkpoint refactors its wiring and tests.
- **Uncommitted work**: Project-plugin migration and work-card updates are uncommitted.
- **Blockers**: Full validation currently reports four pre-existing `tagger-cli` JS/JVM test failures. The implementation and focused dashboard validation pass; the unrelated tagger failures remain for a separate follow-up.
- **Date**: 2026-11-10

### 2026-11-11: Design direction changed to project plugin
- The settings-plugin design was limiting because settings evaluation has awkward access to consumer version catalogs and combines build registration, project configuration, and aggregation in one lifecycle scope.
- The plugin now applies to `Project`. Applying it to a build root configures that build's projects, writes `build/dashboard/build-test-counts.json`, and registers the local `dashboardTestSummary` task.
- Composite builds remain explicit: the top-level build supplies included-build count files through `dashboardTestSummary.includedCounts` and depends on each included build's `dashboardTestSummaryCounts` task via `gradle.includedBuild(...).task(...)`.
- Each participating build applies the plugin in its root `build.gradle.kts`; settings files retain only ordinary `includeBuild(...)` declarations needed by the repository's existing composite/plugin wiring.
- This gives consumers normal project-plugin and version-catalog ergonomics at the cost of requiring explicit root/CI wiring for included builds.
- Focused TestKit coverage passes 5/5, including failed tests, stale XML handling, explicit included-build aggregation, and configuration-cache reuse.
- The full check still reaches the four existing unrelated `tagger-cli` JS/JVM failures.

## Implementation Notes

### 2026-11-11: CI adoption wired
- The requested source-checkout behavior is an explicit requirement: existing repository plugins resolve from the included `tools-plugins` build and do not pin a released version. Pinning `com.zegreatrob.tools.dashboard-test-summary` at `3.7.3` is therefore only a temporary experiment, not the intended completion.
- Temporarily pinned the published marker at `3.7.3` in the root, `tools`, `tools-tests`, and `command-line-tools` settings files while investigating the composite boundary; this wiring must be replaced and is not the final plan.
- Replaced the raw nested `includeBuild` declarations with `dashboardTestSummary.includeBuild(...)` registrations and assigned the `tools` composite root the `com.zegreatrob.tools` group.
- CI now runs `check dashboardTestSummary --continue` and uploads `build/dashboard/test-summary.json` as the `ci-evidence` artifact. The existing `test-output` artifact and Testspace upload remain unchanged.
- A local run generated `build/dashboard/test-summary.json` successfully with `-PdashboardRunId=1 -PdashboardRunAttempt=1 -PdashboardHeadSha=local`; the command still returned non-zero because of four unrelated `tagger-cli` test failures.
- Attempted source checkout resolution with `pluginManagement.includeBuild("tools")`; Gradle then lost the nested `tools-plugins` fingerprint plugin while configuring `command-line-tools`. Reverted that experiment. Relocating the dashboard plugin into the shared plugin build is the clean follow-up if source checkout resolution is required.

### Redirected plan: source-resolved plugin adoption
- Move the dashboard plugin implementation and its focused tests into the shared `tools-plugins` included build, or expose it through a dedicated plugin build consumed alongside `tools-plugins` if that keeps the build boundaries cleaner.
- Preserve the plugin ID and behavior, including composite evidence discovery and `build/dashboard/test-summary.json` output.
- Remove released-version declarations for the dashboard plugin from all settings files and resolve it from the checkout, matching the existing fingerprint/tagger/digger plugin convention.
- Re-run the root task and nested included-build tasks to prove both dashboard resolution and existing plugin resolution remain intact.
- Only after source resolution and the full relevant checks pass, mark the redirected checklist item complete and retain the CI artifact wiring.

### 2026-11-11: Source-resolved plugin alignment completed
- Moved the dashboard plugin module under `tools-plugins/dashboard-test-summary`, registered it in the shared plugin build, and removed the released-version pins from all settings files.
- Kept the module as a dedicated subproject so it retains its JVM/Gradle plugin classpath and focused TestKit tests while sharing the same composite plugin-resolution boundary as the other repository plugins.
- Added root plugin-management inclusion for `tools-plugins` and updated the root README link.
- `:tools-plugins:dashboard-test-summary:test` passed. The root `dashboardTestSummary` task resolved from the checkout and generated `build/dashboard/test-summary.json`.
- The aggregate command returned non-zero only because the existing `tagger-cli` JS/JVM suites reported four failures; the dashboard output was still produced with `--continue`.

### 2026-11-10: Focused refactor
- Split local count collection, variant export, and summary registration at their existing Gradle boundaries.
- Build expected JSON and the stale summary fixture with the Kotlin JSON DSL. Keep the wire fields independent of the production writer.
- Negative control: omitted one passing case and duplicated the shared build's skipped case. The total stayed at five and the test failed on the passed and skipped counts. Restored the fixture.
- Focused plugin tests and the full Gradle check passed after the refactor.

### 2026-11-10: Plugin checkpoint
- Full Gradle check passed with five plugin tests. With fail-fast, the failed-assertion fixture produced no summary; with `--continue`, it recorded both tests and preserved the failed build. The crash fixture caught a disabled stale-summary cleanup. The malformed XML test caught a parser that accepted truncated input.
- Consumer README links verified. IntelliJ reported no problems in either README and the formatter ran.
- Next slice: pin the release in each participating settings file. Give the `tools-plugins` root a group for composite substitution, enroll all four existing included builds, request `check dashboardTestSummary --continue`, and upload only the JSON as `ci-evidence` after Build.

### 2026-11-10: Standalone and composite fixtures pass
- Separated dependency scopes from resolvable configurations. The nested-build fixture required the enabled test output path to be mapped from its task provider and build evidence variants to carry participating included builds as transitive dependencies.
- Verified both generated plugin POMs contain the intended coordinates and Maven publication metadata.
- Gradle could not write repository caches in the sandbox. Run validation unsandboxed and compile Kotlin in-process to avoid reusing the restricted Kotlin daemon.

### 2026-11-10: Work card created
- Keep publication and CI adoption in separate slices to avoid a bootstrap dependency on an unpublished plugin.
- Use TestMints with observable outcomes and red-green validation. Use the Kotlin package `com.zegreatrob.tools.dashboard`.

## Validation
- [x] `:tools:dashboard-test-summary:test` ran as part of the full check (5 passed).
- [x] `:tools-plugins:dashboard-test-summary:test` passed after relocation into the shared plugin build.
- [x] Root `dashboardTestSummary` resolved from the checkout and generated `build/dashboard/test-summary.json`.
- [x] `./gradlew check -q --console=plain --no-daemon -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false` passed.
- [x] Generated and verified the implementation and plugin marker POMs.
- [x] Run the CI report command locally with sample run identity after source-resolving the plugin.
- [ ] Full repository check remains blocked by four existing `tagger-cli` JS/JVM test failures.
