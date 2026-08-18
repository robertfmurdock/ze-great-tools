# Fix Tagger Operation Failure Exit Code

## Goal
Ensure `tagger tag` and the Gradle `tag` task fail whenever annotated-tag creation or pushing fails, independently of warning strictness.

## Constraints
- Preserve policy-warning behavior: warnings exit successfully by default and fail only when `warningsAsErrors` is enabled
- Preserve current CLI text and JSON output contracts, including the `TAG_ERROR` code
- Represent expected Git operation failures explicitly as `TagResult.Failure`; unexpected programming failures continue to propagate
- Keep CLI and Gradle plugin behavior aligned through the shared tag specification
- Preserve the existing same-commit idempotency shortcut; remote reconciliation after a prior partial failure is out of scope
- Scope changes to tagger core, its CLI/plugin adapters, shared tests, and directly affected documentation
- Semver intent: `[patch]` - correct false-success behavior without changing supported stdout schemas
- Related issue: https://github.com/robertfmurdock/ze-great-tools/issues/353

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Add shared failing coverage for tag creation without a configured committer identity when `warningsAsErrors=false`
  - Verify both CLI and Gradle plugin report failure and no tag is created
  - Agent cycle: write one test -> verify expected failure -> implement minimum result-model behavior -> refactor-light -> verify pushable
  - Update plan if guidelines reveal new constraints
- [x] Add `TagResult.Failure` and distinguish policy warnings from Git operation failures
  - Catch `ProcessError` across both annotated-tag creation and tag pushing
  - Return `Failure(error.toUserMessage())` for expected subprocess failures
  - Return `Success` only after both operations complete
  - Allow unexpected exceptions to propagate
  - Agent cycle: test -> implement -> refactor-light -> verify pushable
  - Update plan if guidelines reveal new constraints
- [x] Make CLI and Gradle execution boundaries fail unconditionally for `TagResult.Failure`
  - CLI text output remains actionable and exits nonzero
  - CLI JSON output remains `status: "error"` with `code: "TAG_ERROR"` and exits nonzero
  - Gradle task throws `GradleException` with the failure message
  - `TagResult.Warning` remains controlled only by `warningsAsErrors`
  - Agent cycle: write one test -> verify expected failure -> implement -> refactor-light -> verify pushable
  - Update plan if guidelines reveal new constraints
- [ ] Add regression coverage for push failure and output contracts
  - Prove a rejected or unavailable push fails with `warningsAsErrors=false` across CLI and Gradle plugin
  - Add CLI-specific JSON coverage for the stable error payload and nonzero status
  - Retain coverage for non-strict and strict policy warnings, successful tagging, and idempotent tagging
  - Agent cycle: write one test at a time -> verify expected failure -> implement if needed -> refactor-light -> verify pushable
  - Update plan if guidelines reveal new constraints
- [ ] Clarify user-facing documentation that operational failures always fail while strict mode controls warnings
  - Preserve existing examples and JSON schema
  - Verify links, grammar, and formatting according to `DOCUMENTATION.md`
  - Agent cycle: verify current behavior coverage -> update documentation -> verify pushable
- [ ] Final refactor pass via authorized subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review all changes against applicable playbooks and run full validation
- [ ] Move this file to `agents.d/work_completed/`

## Current State
- **Commit SHA**: 9bfae02b
- **Uncommitted work**: This work card only
- **Blockers**: None
- **Status**: Ready to start with the shared failing test
- **Date**: 2026-08-18

## Implementation Notes
_(newest first)_

### 2026-08-18: Missing-identity failure slice
Added the shared non-strict missing-committer-identity scenario. The red run failed on JVM and JS because each CLI adapter returned `TestResult.Success`; after adding `TagResult.Failure` and unconditional CLI/Gradle failure handling, the CLI and Gradle plugin module checks passed. Core tagging now catches only `ProcessError`, after tag creation or pushing, and allows unexpected exceptions to propagate.

### 2026-08-18: Plan captured
Issue #353 reports that `git tag` exits 128 when committer identity is unavailable, but Tagger converts the subprocess failure to `TagResult.Warning`. The CLI then applies `warningsAsErrors=false` and exits 0 even though no tag exists.

The agreed result semantics are:
- `TagResult.Success`: the requested tag operation completed
- `TagResult.Warning`: tagging was intentionally skipped because of a policy condition; strictness remains configurable
- `TagResult.Failure`: annotated-tag creation or pushing failed; execution adapters must always fail

`TagResult` is not an externally supported Kotlin API for these systems, so adding a sealed subtype is an acceptable internal change. The core will translate known `ProcessError` instances into typed failures and will not convert unexpected programming errors into domain results.

The existing same-commit early success remains unchanged for this issue. Whether retries should reconcile a local tag missing from the remote is a separate behavior decision and should not expand this patch without new evidence.

### 2026-08-18: Subagent authorization
The user explicitly authorized the repository-required subagent for the final refactor review in this thread.

## Validation
- [x] Focused shared test demonstrates the original false-success behavior before implementation
- [x] `./gradlew :command-line-tools:tagger-cli:check -q --console=plain`
- [x] `./gradlew :tools-tests:tagger-plugin-test:check -q --console=plain`
- [ ] Modified user-facing documentation passes link, grammar, and formatting checks from `DOCUMENTATION.md`
- [ ] `./gradlew check -q --console=plain`
- **Status**: Missing-identity slice passes CLI and Gradle plugin module checks
