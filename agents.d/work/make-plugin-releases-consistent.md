# Make Plugin Releases Consistent

**Work card protocol: 2**

## Goal
Fix the release process so every intended plugin, including `com.zegreatrob.tools.dashboard-test-summary`, is released consistently with the project's version and release policy.

## Constraints and acceptance evidence
- Do not publish or backfill historical versions.
- Trace how plugin versions are selected and how each plugin marker and artifact enters the release workflow.
- Identify why the dashboard plugin was omitted from the `3.8.x` releases, and fix the release process so the same omission cannot recur.
- Verify the release configuration covers every intended plugin consistently, including the dashboard test summary plugin.
- Add or update automated checks for release coverage where practical. Demonstrate that the check catches a missing plugin before the fix and passes with the fix.
- Use a dry run or equivalent evidence to confirm the corrected process would release the intended plugins at the same version. Do not publish as part of this work unless separately authorized.

## Progress
The Plugin Portal reports `3.7.3` as the latest for `com.zegreatrob.tools.dashboard-test-summary`. The `3.8.5` releases for Digger, Fingerprint, and Tagger do not establish that the dashboard plugin was published. The task is to correct future release consistency, not to publish prior versions.

## Resume note
Inspect the current release configuration and workflow, compare how each intended plugin is included, then implement and verify a forward-looking fix. Record any release-policy decision that prevents a consistent fix and the next step needed.
