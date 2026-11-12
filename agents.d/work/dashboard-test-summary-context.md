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
- [ ] Inspect the dashboard consumer/schema contract to determine whether run metadata fields can be absent or must carry a local identity.
- [ ] Confirm the GitHub Actions variables that reliably identify run ID, attempt, and the relevant commit in this workflow, including pull request merge-checkout behavior.
- [ ] Decide the local representation: omit GitHub-only fields, use nullable fields, or another explicit local context representation. Avoid inventing a fake GitHub run ID.
- [ ] Decide whether existing Gradle properties remain supported as overrides or should be removed; the desired default must require no properties.
- [ ] Add Testmints coverage for a local environment with GitHub variables absent and for GitHub environment auto-detection.
- [ ] Update plugin README to describe the automatic local/CI behavior.
- [ ] Validate the focused plugin tests and relevant build behavior; consider downstream compatibility before changing schema.

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

