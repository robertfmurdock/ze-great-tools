# TaggerPlugin: Detached HEAD / No Upstream Tracking Branch

## Root Cause

The TaggerPlugin error

```
⚠️  HEAD has no upstream tracking branch (detached HEAD).
```

means git HEAD is in a **detached state** with no tracking branch configured. The remote
repository itself is not missing — what is missing is a branch reference that HEAD can follow.

This is the default checkout behaviour for `pull_request` events on GitHub Actions: the runner
checks out `refs/pull/N/merge` (a synthetic merge commit), not the source branch. Other CI
systems may produce the same condition through different mechanisms.

## Understanding the Risk

Without upstream tracking, tagger cannot compare your current commit against the remote to detect:
- **AHEAD**: commits not yet pushed (should produce snapshot)
- **BEHIND**: commits on remote not in local history (should produce snapshot)

On **feature branches**, this is safe: the `NOT_RELEASE_BRANCH` check adds `-SNAPSHOT` regardless.

On **release branches** (main, master), missing upstream is dangerous:
- Both `calculate-version` and `tag` may produce stable versions (1.2.3) instead of snapshots
- Stable versions trigger release automation: Maven Central, Docker Hub, production deploys
- `calculate-version` is read-only but its output drives irreversible writes downstream
- The safety check exists to prevent accidental production releases

### When Safe / When Dangerous

**Safe scenarios (feature branches):**
- Running `calculate-version` or `tag` on feature/topic branches
- The `NOT_RELEASE_BRANCH` snapshot reason protects you
- Missing upstream reduces accuracy but won't trigger releases

**Dangerous scenarios (release branches):**
- Running `calculate-version` on main/master in CI
- Running `tag` on main/master in CI
- Any workflow where the version output drives release automation
- Missing `AHEAD`/`BEHIND` checks can produce stable versions unintentionally

### Decision Matrix

| Branch Type | Command | With Upstream | Without Upstream (allowDetachedHead=true) |
|-------------|---------|---------------|-------------------------------------------|
| Feature | calculate-version | Snapshot (NOT_RELEASE_BRANCH) | Snapshot (NOT_RELEASE_BRANCH) ✅ Safe |
| Feature | tag | Snapshot (NOT_RELEASE_BRANCH) | Snapshot (NOT_RELEASE_BRANCH) ✅ Safe |
| Release | calculate-version | Stable or Snapshot (accurate) | May produce stable ⚠️ Dangerous |
| Release | tag | Stable or Snapshot (accurate) | May produce stable ⚠️ Dangerous |

## Fix: GitHub Actions (`pull_request` events)

Add `ref` and `fetch-depth` to your `actions/checkout` step:

```yaml
- uses: actions/checkout@v4
  with:
    ref: ${{ github.head_ref || github.ref }}
    fetch-depth: 0
```

- `ref: ${{ github.head_ref || github.ref }}` checks out the real branch rather than the
  synthetic merge commit, giving HEAD a tracking branch.
- `fetch-depth: 0` fetches the full history so the tagger can walk tags correctly.

## Fix: GitLab CI

Update your GitLab CI configuration:

```yaml
variables:
  GIT_STRATEGY: clone
  GIT_DEPTH: 0
```

## Fix: Azure DevOps

Update your Azure DevOps pipeline checkout:

```yaml
- checkout: self
  fetchDepth: 0
```

## Bypass: allowDetachedHead Flag

If you intentionally run versioning from a detached HEAD (e.g. a tag-triggered release build on a feature branch),
set `allowDetachedHead = true` in your tagger block:

```kotlin
tagger {
    allowDetachedHead = true
}
```

**⚠️ WARNING:** On release branches (main, master), this removes the safety check that prevents
accidental production releases. Only use when:
- You're certain the branch type will trigger snapshot (e.g., always running on feature branches), OR
- You fully understand the risk and have other safeguards in place

Leaving `allowDetachedHead = false` (the default) is the safer choice for most workflows.
