# Task: Improve TaggerPlugin "no remote" error message and link to CI guidance doc

## Goal
Make the `NoRemote` failure reason actionable for CI users who hit it on any CI system,
by pointing to a shared documentation page rather than embedding CI-specific YAML inline.

## Background
The error fires from `ChangeType.kt:18-19`:
```kotlin
if (disableDetached && gitStatus.upstream.isEmpty()) {
    return VersionResult.Failure(listOf(FailureVersionReasons.NoRemote))
}
```
`upstream` is empty whenever git HEAD is in a detached state with no tracking branch — which is
exactly what `actions/checkout@v4` produces for `pull_request` events (it checks out
`refs/pull/N/merge`, a synthetic merge commit, not the source branch). Other CI systems can
trigger the same condition via different mechanisms.

The full current error output (prefix added by `VersionResult.message`):
```
Inappropriate configuration: repository has no remote.
```

That message is misleading: the remote *does* exist; what is missing is a tracking branch for
the current (detached) HEAD.

`VersionResult.kt` prepends `"Inappropriate configuration: "` to every `FailureVersionReasons`
message, so the `NoRemote.message` value must be written to complete that sentence.

## Hard constraints
- Do not change the enum value name `NoRemote` (public API surface).
- Keep the message short — CI-specific details belong in the linked doc, not inline.

## What to change

### 1. `docs/tagger-detached-head.md` (new file, top-level `docs/` directory)
Create a new shared documentation page explaining:
- Root cause: detached HEAD / no upstream tracking branch
- GitHub Actions fix (`pull_request` events): use `ref: ${{ github.head_ref || github.ref }}`
  with `fetch-depth: 0`
- Alternative: set `disableDetached = false` in the tagger block for builds where versioning
  from a detached HEAD is acceptable
- Leave room for other CI system examples in future

### 2. `tools/tagger-core/.../FailureVersionReasons.kt`
Update `NoRemote.message` to:
```kotlin
NoRemote("HEAD has no upstream tracking branch. See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md"),
```

### 3. (Optional) KDoc on `TaggerExtension.disableDetached`
Add a brief note explaining when `disableDetached = false` is appropriate vs. fixing the
checkout step, with a link to the same doc.

## Checklist

### Code changes
- [ ] `docs/tagger-detached-head.md` created with root-cause explanation, GitHub Actions fix,
  and `disableDetached = false` alternative
- [ ] `FailureVersionReasons.NoRemote` message updated to short form with doc link
- [ ] (Optional) `disableDetached` KDoc in `TaggerExtension.kt` links to doc

### Validation
- [ ] Existing `NoRemote` test in `CalculateVersionTestSpec` updated to assert on new message text
- [ ] `./gradlew :tools-tests:check` passes
- [ ] Move this file to `agents.d/tasks_completed/`

## Definition of done
- A developer hitting the error reads the message, follows the link, and knows immediately
  what to change — regardless of which CI system they use.
- The doc is the single source of truth for CI workarounds; the error message stays stable
  as new CI examples are added.
