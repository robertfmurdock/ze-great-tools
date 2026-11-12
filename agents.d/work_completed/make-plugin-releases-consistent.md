# Make Plugin Releases Consistent

**Work card protocol: 2**

## Goal
Fix the release process so every intended plugin, including `com.zegreatrob.tools.dashboard-test-summary`, is released consistently with the project's version and release policy.

## Constraints and acceptance evidence
- Do not publish or backfill historical versions.
- Trace how plugin versions are selected and how each plugin marker and artifact enters the release workflow.
- Identify why the dashboard plugin was omitted from the `3.8.x` releases, and fix the release process so the same omission cannot recur.
- Verify the release configuration covers every intended plugin consistently, including the dashboard test summary plugin.
- Add or update automated checks for release coverage where practical. Demonstrate that the check catches a missing plugin before the fix and passes with the fix.
- Use a dry run or equivalent evidence to confirm the corrected process would release the intended plugins at the same version. Do not publish as part of this work unless separately authorized.

## Progress
The Plugin Portal reports `3.7.3` as the latest for `com.zegreatrob.tools.dashboard-test-summary`. The `3.8.5` releases for Digger, Fingerprint, and Tagger do not establish that the dashboard plugin was published. The task is to correct future release consistency, not to publish prior versions.

The omission was caused by the dashboard plugin living in the separate `tools-plugins` included build, which the root release did not invoke before the dashboard-specific dependency was added. Gradle's `-Pversion` is visible to included builds (confirmed as `3.8.6` in the dashboard project). Added a `tools-plugins:release` aggregate that discovers all `publishPlugins` tasks in that build; root stable releases now invoke it, following the existing `tools:release` pattern. This removes the project-specific root hook so future Portal plugins in the build join release automatically.

Evidence: `./gradlew release --dry-run -q --console=plain -Pversion=3.8.6` includes dashboard marker metadata/POM generation and `publishPlugins`, as well as `tools-plugins:release`; `:tools-plugins:dashboard-test-summary:properties` reports version `3.8.6`. The pre-fix root configuration in the history had no `tools-plugins` release dependency, which is the missing-plugin case. `./gradlew :tools-plugins:check -q --console=plain` and the repository-wide `./gradlew check -q --console=plain` pass. The full check took 4m16s because it runs nested Gradle functional tests. No publication was run.

