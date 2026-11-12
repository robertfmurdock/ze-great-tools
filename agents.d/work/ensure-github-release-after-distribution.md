# Ensure GitHub Release After Distribution

**Work card protocol: 2**

## Goal
Order GitHub release tasks after Maven, npm, and Gradle Plugin Portal publication so a published non-draft GitHub release confirms distribution succeeded.

## Constraints and acceptance evidence
- Affected module: `tagger-plugin`. Semver intent: `[patch]` — internal task ordering, no API impact.
- Change task ordering only; preserve publication and release behavior and GitHub task idempotency.
- Identify the final task that represents successful completion for each publication channel, then order GitHub release creation after those tasks.
- Use `mustRunAfter`, not `dependsOn`, so disabling GitHub releases or skipping publication does not force unrelated tasks into the graph.
- Verify the task graph includes publication before `githubRelease`, followed by `githubReleaseUpload` and `githubReleasePublish`. Exercise the behavior with a safe test or mock scenario, including retry behavior; do not perform a real external release as a validation step.

## Decision
- The desired invariant is: if a non-draft GitHub release exists, all configured distributions succeeded. Ordering-only constraints enforce it without changing which publication tasks are requested.

## Resume note
Not started. Next, inspect the release task graph to identify the final Maven, npm, and Plugin Portal publication tasks. Relevant paths likely include Sonatype close/release, CLI `jsPublish`, and `publishPlugins`; verify these against the current build before wiring them.
