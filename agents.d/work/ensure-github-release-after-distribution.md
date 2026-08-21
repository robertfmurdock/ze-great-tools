# Ensure GitHub Release After Distribution

## Goal
Wire GitHub release tasks to run after Maven/npm/Gradle Plugin Portal publication so that the presence of a GitHub release confirms successful distribution to all package repositories.

## Constraints
- **Scope**: Task ordering only - no changes to publication or release logic
- **Philosophy**: GitHub release as "confirmation signal" - if it exists, distributions succeeded
- **Dependencies**: Must maintain idempotency of GitHub tasks (allow retries without duplication)
- **Backward compatibility**: Existing task dependencies unchanged - only add ordering constraints
- **Semver intent**: `[patch]` - internal task ordering change, no API impact
- **Affected modules**: `tagger-plugin`

## Context
Currently, GitHub release tasks (githubRelease, githubReleaseUpload, githubReleasePublish) run in parallel with Maven/npm publication. This means a GitHub release can exist even if Maven/npm publication fails.

This creates ambiguity: does a GitHub release mean the code was successfully distributed? Currently: no.

Desired invariant: **If a GitHub release exists (non-draft), then Maven/npm publication succeeded.**

This makes the GitHub release a reliable signal that the release was completed successfully across all distribution channels.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Identify all publication tasks in the release task graph
  - Maven Central (Sonatype): which tasks complete the publication?
  - npm: which tasks complete the publication?
  - Gradle Plugin Portal: which tasks complete the publication?
  - Document the task names and what they represent
  - Agent cycle: research → document → verify
- [ ] Add mustRunAfter constraints to githubRelease task
  - githubRelease should run after all Maven publication tasks complete
  - githubRelease should run after all npm publication tasks complete
  - githubRelease should run after all Gradle plugin publication tasks complete
  - Use mustRunAfter (not dependsOn) to avoid forcing publication when GitHub release is disabled
  - TDD: Verify task ordering with --dry-run
  - Agent cycle: test → implement → refactor-light → verify pushable
- [ ] Verify task graph with dry-run
  - Run `./gradlew release --dry-run` and verify ordering
  - Confirm all publication tasks appear before githubRelease
  - Confirm githubReleaseUpload appears after githubRelease
  - Confirm githubReleasePublish appears after githubReleaseUpload
  - Document the verified task ordering in Implementation Notes
  - Agent cycle: verify → document
- [ ] Test with actual release (or mock scenario)
  - Ensure publications complete before GitHub release creation
  - Verify idempotency if tasks are re-run
  - Agent cycle: test → verify → document
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)

## Current State
**Commit**: Not started
**Uncommitted changes**: None
**Status**: Ready to start
**Date**: 2026-08-04

## Implementation Notes
(Most recent first, date-stamped)

### 2026-08-04: Work card created
Context from discussion:
- Current behavior: GitHub release tasks can run in parallel with Maven/npm publication
- Problem: GitHub release existence doesn't guarantee successful distribution
- Solution: Use mustRunAfter to ensure publications complete before GitHub release
- Benefit: GitHub release becomes a reliable "success signal" for complete distribution

### 2026-08-20: Subagent authorization

User explicitly authorized the mandatory final refactor subagent.

Key consideration: Use `mustRunAfter` not `dependsOn` so that:
- When githubReleaseEnabled=false, publications still run
- When publications are skipped (snapshots), GitHub tasks don't force them to run
- Ordering is enforced only when both sets of tasks are in the task graph

## Validation
```bash
./gradlew check -q --console=plain
```
**Status**: Not yet run

## Design Notes

### Task Ordering Pattern
Use `mustRunAfter` rather than `dependsOn`:
```kotlin
githubRelease.configure {
    mustRunAfter(":tools:closeAndReleaseSonatypeStagingRepository")
    mustRunAfter(":command-line-tools:tagger-cli:jsPublish")
    mustRunAfter(":command-line-tools:digger-cli:jsPublish")
}
```

This ensures ordering without creating dependency edges that would force tasks to run when not needed.

### Publication Task Discovery
Need to identify the "final" publication tasks that represent complete publication:
- Maven: likely `closeAndReleaseSonatypeStagingRepository` (confirms staged artifacts released)
- npm: likely `jsPublish` tasks for each CLI (confirms packages published to npm)
- Gradle Plugin Portal: likely `publishPlugins` or similar (confirms plugins published)

Use `./gradlew release --dry-run` to see the full task graph and identify these tasks.

### Verification Strategy
Can verify ordering without actual publication by:
1. Using `--dry-run` to see task order
2. Checking task dependencies programmatically
3. Running a test release with `githubReleaseDraft=true` to see if publications complete first

### Scope Boundaries
**In scope**: Task ordering constraints only
**Out of scope**:
- Changing publication logic
- Adding new tasks or features
- Modifying GitHub release creation logic
- Changing how publications work
