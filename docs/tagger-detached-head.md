# TaggerPlugin: Detached HEAD / No Upstream Tracking Branch

## Root Cause

The TaggerPlugin error

```
Inappropriate configuration: HEAD has no upstream tracking branch.
```

means git HEAD is in a **detached state** with no tracking branch configured. The remote
repository itself is not missing — what is missing is a branch reference that HEAD can follow.

This is the default checkout behaviour for `pull_request` events on GitHub Actions: the runner
checks out `refs/pull/N/merge` (a synthetic merge commit), not the source branch. Other CI
systems may produce the same condition through different mechanisms.

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

## Alternative: Disable the Check

If you intentionally run versioning from a detached HEAD (e.g. a tag-triggered release build),
set `disableDetached = false` in your tagger block:

```kotlin
tagger {
    disableDetached = false
}
```

Use this only when a detached HEAD is expected and acceptable for the build in question.
Leaving `disableDetached = true` (the default) is the safer choice for most workflows.
