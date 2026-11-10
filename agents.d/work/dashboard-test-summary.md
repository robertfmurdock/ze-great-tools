# Dashboard Test Summaries

## Goal
Publish a Gradle settings plugin that aggregates JUnit XML across participating builds and use it for Ze Great Tools' CI evidence.

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
- [ ] After the published marker resolves, pin it and enroll the repository composite. Verify CI artifact and commit separately.
- [ ] Complete final refactor review and move card after the work is pushed.

## Current State
- **Commit SHA**: 86f21d7b adds the plugin. The next checkpoint refactors its wiring and tests.
- **Uncommitted work**: None in this work card's scope after the plugin checkpoint.
- **Blockers**: The dogfood slice starts after the first published plugin marker resolves.
- **Date**: 2026-11-10

## Implementation Notes

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
- [x] `./gradlew check -q --console=plain --no-daemon -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false` passed.
- [x] Generated and verified the implementation and plugin marker POMs.
- [ ] Run the CI report command locally with sample run identity after pinning the published plugin.
