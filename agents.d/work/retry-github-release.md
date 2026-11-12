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
