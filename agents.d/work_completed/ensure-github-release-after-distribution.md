# Ensure GitHub Release After Distribution

**Work card protocol: 2**

## Goal
Order GitHub release tasks after Maven, npm, and Gradle Plugin Portal publication so a published non-draft GitHub release confirms distribution succeeded.

## Constraints and acceptance evidence
- Affected module: `tagger-plugin`. Semver intent: `[patch]` — internal task ordering, no API impact.
- Change task ordering only; preserve publication and release behavior and GitHub task idempotency.
- Identify the final task that represents successful completion for each publication channel, then order GitHub release creation after those tasks.
- Keep GitHub release ordering non-dependency-based. Gradle does not allow `mustRunAfter` references across included builds, so the root release task directly references the same publication completion tasks already brought into its graph by included-build release finalizers.
- Verify the task graph includes publication before `githubRelease`, followed by `githubReleaseUpload` and `githubReleasePublish`. Exercise the behavior with a safe test or mock scenario, including retry behavior; do not perform a real external release as a validation step.

## Decision
- The desired invariant is: if a non-draft GitHub release exists, all configured distributions succeeded. The tagger plugin uses `mustRunAfter` for same-build publication tasks and the `release` task; root release wiring names the Maven and npm completion tasks directly because included-build task ordering rules cannot express a cross-build `mustRunAfter`. These are already in the release graph through the included build orchestration, so the added references change their ordering without adding graph tasks.

## Progress
- Maven completion is `:tools:closeAndReleaseSonatypeStagingRepository`; npm completion is `:command-line-tools:tagger-cli:jsPublish`; Plugin Portal completion is `:tools-plugins:dashboard-test-summary:publishPlugins`.
- Added mock-backed coverage for publication ordering and for a failed Maven publication followed by a successful retry. The retry assertion confirms the intended failure reason and verifies GitHub was not called before success. No external release was performed.
- Targeted tagger unit and functional tests, relevant Kotlin lint tasks, and `git diff --check` passed. The root release task graph was inspected to confirm the configured publication task identities and GitHub task chain.
- `./gradlew check` and the affected-module `check` were attempted but stopped after several minutes without further output; broad check completion remains unverified.
