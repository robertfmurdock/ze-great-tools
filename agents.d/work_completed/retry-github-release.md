# Retry transient GitHub release failures

**Work card protocol: 2**

## Goal

Recover from transient GitHub CLI failures in `githubRelease` by default. [Issue #380](https://github.com/robertfmurdock/ze-great-tools/issues/380)

## Acceptance evidence

- Retry transient lookup and creation failures with bounded exponential backoff and jitter. Give secondary rate limits at least 60 seconds.
- Recheck the release before each write and require the remote tag to exist. Preserve immediate failure and diagnostics for permanent errors.
- Exercise recovery, ambiguous creation, classification, and exhaustion through TestKit. Preserve draft handling and configuration-cache reuse.

## Decisions

Keep the existing Exec task and CLI. Scope retries to release creation. Semver intent is patch.

## Completion evidence

- TestKit reproduced the reported TLS failure before the fix. The final suite verifies release recovery, read-before-write ordering, permanent errors, retry exhaustion, deterministic jitter, draft arguments, and configuration-cache reuse.
- Final audit found a lost-response gap on the fourth create. A final read-only lookup resolves that case. An explicit secondary-rate-limit rejection at the attempt limit ends without another request.
- `./gradlew check -q --console=plain` passed after the audit fixes. The additional-tasks functional suite passed all 24 tests.
