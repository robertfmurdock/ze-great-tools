# Dashboard Test Summary Context

**Work card protocol: 2**

## Goal
Make `dashboardTestSummary` generate its report without special setup in both local builds and GitHub Actions. Detect GitHub Actions context automatically and include accurate run metadata when available.

## Requirements
- Running the task locally without `-PdashboardRunId`, `-PdashboardRunAttempt`, or `-PdashboardHeadSha` succeeds and writes the report.
- Running in GitHub Actions automatically uses the current workflow run's identity and commit information from the environment.
- CI workflow YAML does not need to pass metadata through Gradle command-line properties.
- Keep test totals and current report path/consumer behavior working.

## Preparation and Questions to Resolve
- [x] Inspect the dashboard consumer/schema contract to determine whether run metadata fields can be absent or must carry a local identity.
- [x] Confirm the GitHub Actions variables that reliably identify run ID, attempt, and the relevant commit in this workflow, including pull request merge-checkout behavior.
- [x] Decide the local representation: omit GitHub-only fields, use nullable fields, or another explicit local context representation. Avoid inventing a fake GitHub run ID.
- [x] Decide whether existing Gradle properties remain supported as overrides or should be removed; the desired default must require no properties.
- [x] Add Testmints-style coverage for a local environment with GitHub variables absent and for GitHub environment auto-detection.
- [x] Update plugin README to describe the automatic local/CI behavior.
- [x] Validate the focused plugin tests and relevant build behavior; consider downstream compatibility before changing schema.

## Constraints and Acceptance Evidence
- Follow `CLAUDE.md`, `agents.d/context/WORK_CHECKLIST.md`, `agents.d/context/TESTING.md`, and the Testmints skill for implementation and behavior tests.
- Local invocation with no GitHub variables and no `-Pdashboard*` metadata properties produces a usable summary containing aggregated counts.
- Under GitHub Actions variables, the generated report contains the actual run ID, run attempt, and commit SHA expected for the workflow context.
- Missing GitHub metadata outside Actions does not fail report generation.
- Workflow invocation remains a plain Gradle task invocation without metadata forwarding.
- Confirm consumer/schema handling before finalizing the wire format.

## Decisions
- No implementation changes until the metadata consumer and local wire-format implications are understood.
- Prefer representing local context honestly over fabricating a GitHub run identity.

## Progress
- Read `CLAUDE.md` and the task-gated work-card, code-style, testing, and GitHub Actions guidance.
- Current implementation requires positive run ID, positive attempt, and nonblank SHA; it resolves Gradle properties before GitHub environment variables. The workflow invokes the task without passing those properties.
- Consumer search in this repository found no report parser or schema definition; the dashboard consumer appears external and needs inspection before a schema change.
- The dashboard consumer requires `schemaVersion: 1`, positive `runId`/`runAttempt`, and nonblank `headSha` to bind an uploaded artifact to the exact workflow run. Its artifact reader only handles GitHub Actions artifacts. Locally, the report can omit those GitHub-only fields and still provide inspectable counts; do not upload that local form.
- This repository's workflow is triggered by pushes and manual dispatch, checks out the run commit, and already exposes `GITHUB_ACTIONS`, `GITHUB_RUN_ID`, `GITHUB_RUN_ATTEMPT`, and `GITHUB_SHA`. No workflow YAML changes or Gradle property overrides are needed.
- `$testmints` is not installed in the current environment; the existing test file uses TestMints setup/exercise/verify directly.
- Implementation detects `GITHUB_ACTIONS`; in Actions it validates and writes the three environment-provided identity fields. Outside Actions it writes the schema version and test totals without fake identity fields. Removed Gradle-property overrides so they cannot replace the actual workflow context.
- Added coverage proving local mode succeeds without inherited GitHub variables while the existing Actions fixture verifies automatic ID/attempt/SHA inclusion. Updated README; no workflow change was necessary.
- `:tools-plugins:dashboard-test-summary:test` passed in an isolated checkout. `formatKotlin` passed. Repository-wide `check` ran but failed in unrelated `:tools-tests:digger-plugin-test:functionalTest` because a nested TestKit build could not access its Kotlin cache output (`dirty-sources.txt` missing) in the isolated temporary checkout.

