# Implement Immutable Releases for Supply Chain Security

## Goal
Implement GitHub immutable releases to prevent supply chain attacks by ensuring published release assets and git tags cannot be modified or deleted after publication.

## Constraints
- Git tag MUST be created before any publication to registries (Maven/Gradle/npm) to prevent version consumption without tagging
- Release assets uploaded to draft before publication to allow atomic transition to immutable state
- Remove `--clobber` flags that bypass immutability protections
- Maintain idempotency for CI retries and manual re-runs
- Semver intent: `[patch]` - internal release process improvement, no API changes
- No changes to published artifact contents, signatures, or consumer workflow
- Must work with existing fingerprint and contribution tracking

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Reorder release task dependencies to create git tag BEFORE publication
  - Modify `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Change `registerReleaseTask` to use `dependsOn(tag)` instead of `finalizedBy(tag)`
  - Ensures tag exists if any publication succeeds
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Make git tag creation idempotent
  - Modify `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/tagger/TagVersion.kt`
  - Check if tag exists on current commit before creating
  - Fail if tag exists on different commit (prevents version reuse)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Switch GitHub release creation to draft-first pattern
  - Modify `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - Replace `gh api POST` with `gh release create --draft`
  - Remove redundant `generate_release_notes` parameter
  - Add idempotency check (skip if release exists)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Remove `--clobber` flag from CLI distribution uploads
  - Modify `build.gradle.kts` line 106 in `uploadCliDistributions` task
  - Remove `--clobber` argument from `gh release upload` command
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Update GitHub Actions workflow for draft-publish flow
  - Modify `.github/workflows/main.yml`
  - Remove `--clobber` from fingerprint upload (line 87)
  - Remove `continue-on-error: true` from fingerprint upload (line 82)
  - Add "Publish Release" step after uploadCliDistributions
  - Add "Release Summary" step to verify immutability
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Verify idempotency of entire release flow
  - Test tag creation on existing tag (same commit)
  - Test GitHub release creation when release exists
  - Test asset upload when assets exist
  - Confirm appropriate success/failure behavior
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Verify immutability enforcement
  - Create test release and publish it
  - Attempt asset upload without `--clobber` (should fail)
  - Verify tag cannot be deleted via GitHub API
  - Confirm release attestations generated
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: dbc0c9a8
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: In progress - 5 of 9 implementation tasks complete
- **Date**: 2026-07-29

## Implementation Notes
_(newest first)_

### 2026-07-29: Updated GitHub Actions workflow for draft-publish flow (commit dbc0c9a8)
Removed `--clobber` from fingerprint upload and `continue-on-error: true` to enforce immutability and fail-fast. Added "Publish Release" step using `gh release edit --draft=false` to transition draft to published after all assets uploaded. Added "Release Summary" step to verify immutability and list published assets in GitHub Step Summary. Draft-first pattern ensures atomic asset attachment before publication. All checks pass.

### 2026-07-29: Removed --clobber flag (commit a5dec429)
Removed `--clobber` flag from `uploadCliDistributions` task in build.gradle.kts. This enforces immutability after GitHub release publication - assets can only be uploaded once, preventing supply chain modification attacks. No unit tests for build configuration (verified via CI execution). All checks pass.

### 2026-07-29: Draft-first GitHub release with idempotency (commit 8e6662a6)
Replaced `gh api POST` with `gh release create --draft` for immutable release pattern. Added idempotency check using `gh release view` - skips creation if release already exists. Removed `generate_release_notes` parameter (not needed with gh release create). Tests verify draft flag presence, idempotency check, and absence of deprecated parameters. All checks pass.

### 2026-07-29: Idempotent tagging completed (commit b060dcd7)
Modified Tag.kt to check if tag exists on same commit (returns Success idempotently) or different commit (returns Warning with error message). Added tests to verify both scenarios. All tests pass.

### 2026-07-29: Tag-first ordering completed (commit fe3ca5a8)
Changed release task to use `dependsOn(tag)` instead of `finalizedBy(tag)`. Test added to verify tag task is in release task's dependency tree. All checks pass.

### 2026-07-29: Subagent authorization granted
User authorized subagent usage for this work card, specifically for mandatory final refactor pass.

### 2026-07-29: Work card created

**Context**: GitHub's immutable releases feature (https://docs.github.com/en/code-security/concepts/supply-chain-security/immutable-releases) provides supply chain security through:
1. Tag immutability - tags cannot be changed/deleted once release published
2. Asset protection - release assets cannot be modified/deleted
3. Release attestations - cryptographic verification records

**Current Issues**:
- `--clobber` flag on asset uploads bypasses immutability
- Git tags created AFTER publication via `finalizedBy`
- If publication succeeds but tagging fails, version consumed without tag
- Release created immediately (not draft-first), preventing atomic asset attachment

**Critical Constraint**: Once ANY registry publication succeeds (Maven/Gradle/npm), that version is consumed and cannot be reused. Tag MUST exist before or during publication to mark version as used.

**Key Changes**:
1. Tag-first ordering: `release.dependsOn(tag)` instead of `release.finalizedBy(tag)`
2. Draft-first: Create draft → upload assets → publish atomically
3. Remove clobber: Enforce immutability after publication
4. Idempotency: Safe CI retries after partial failures

**Files to modify**:
- `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt` - Release task ordering, draft creation
- `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/tagger/TagVersion.kt` - Idempotent tagging
- `build.gradle.kts` - Remove clobber from uploadCliDistributions
- `.github/workflows/main.yml` - Remove clobber, add publish step

**Testing Strategy**:
- Create test branch with trivial change to force version bump
- Verify tag created before publication tasks
- Verify draft release created and published after assets attached
- Verify immutability enforced after publication
- Verify retry safety (re-running doesn't fail on existing tag/release)

**Semver**: `[patch]` - internal workflow improvement, no user-facing changes

**Risk**: Tag created but publication fails = "burned" version number. This is acceptable tradeoff vs version published without tag (worse).

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] `./gradlew release --dry-run | grep -E "(tag|publish)"` - verify tag runs before publish
- [ ] Create test release and verify draft created: `gh release view <version> --json isDraft`
- [ ] Publish draft and verify immutability: `gh release edit <version> --draft=false && gh release upload <version> test.txt` (should fail)
- [ ] Verify tag immutability: `gh api -X DELETE /repos/<owner>/<repo>/git/refs/tags/<version>` (should fail with 422)
- [ ] Verify attestations exist: `gh release view <version> --json assets` (check for attestation files)
- [ ] Test CI retry: Re-run workflow after successful release (should not error on existing tag/release)
- [ ] Verify SNAPSHOT versions skip release: `./gradlew calculateVersion -PtaggerForceSnapshot=true` should not create release
