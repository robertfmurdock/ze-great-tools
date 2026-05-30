# Tagger Warnings-As-Errors Alignment

## Goal
Make `warningsAsErrors` consistently enforce non-zero exits for real warning conditions in Tagger CLI, including `calculate-version` warning output.

## Constraints
- Preserve backward compatibility for existing successful workflows unless `warningsAsErrors` is explicitly enabled.
- Keep stdout contracts stable (`--format=json` success/error payload shape stays unchanged); use exit codes and stderr for behavior changes.
- Follow existing warning conventions (`⚠️` diagnostics) and current config/flag precedence (`CLI > .tagger > defaults`).
- Scope changes to tagger core/CLI and linked docs/tests; avoid unrelated refactors.
- Declare semver intent up front and revise it if implementation discoveries show a different impact level.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Define and lock expected warnings policy surface for each command
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Implement `calculate-version` warnings escalation behind `warningsAsErrors`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Align `tag` command semantics and naming so warning/error behavior is explicit and unsurprising
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Improve operator ergonomics for CI diagnosis under strict mode (clear stderr + stable JSON + predictable exit codes)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update README and config docs to define warning classes and strict-mode behavior with examples
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

### Subagent delegation
- Not using subagents for this work card (single-agent mode for time-boxed slice, 2026-05-30).

### Semver intent (initial)
- Expected scope: `[patch]` (behavior-alignment bug fix under existing `warningsAsErrors` policy).
- Re-evaluate during implementation if changes expand beyond alignment (for example, new flags or stdout contract changes).
- If new findings indicate semver may increase (`[patch]` → `[minor]`/`[major]`), pause and ask the user to confirm direction; record the decision with date.

### Why this card exists
- Current `warningsAsErrors` behavior is asymmetric: it influences `tag` handling but does not currently escalate warning emissions from `calculate-version`.
- This creates a policy mismatch: users can enable strict mode and still get warning-only success exits from version calculation paths.

### Current warning inventory (2026-05-30)

**calculate-version command**:
1. Deprecation warning: `⚠️  --disable-detached is deprecated...` (line 116, CalculateVersion.kt)
2. Detached HEAD release-risk warning: `⚠️  Running with allowDetachedHead on release branch...` (line 48, ChangeType.kt)

**tag command**:
- Returns `TagResult.Error` for policy violations (not on release branch, etc.)
- Current behavior: `warningsAsErrors` flag controls exit code (1 if true, 0 if false) but NOT whether warnings are emitted
- Exit code path: Tag.kt lines 63-70

**Policy decision locked**: Both warnings above should escalate to non-zero exits when `warningsAsErrors=true` is set.

### Design guardrails
- Do not reclassify existing hard failures as warnings.
- Keep warning detection source-of-truth close to core result models (avoid fragile text parsing where possible).
- Preserve current JSON schema; strictness should be represented via process exit status, not response shape changes.

### Implementation notes (2026-05-30, 8.3 min elapsed)
- Added `--warnings-as-errors` flag to `calculate-version` command.
- Implemented escalation logic after success output (both TEXT and JSON formats).
- All warnings (deprecation + detached HEAD) now escalate to exit code 1 when flag is enabled.
- Three tests added at spec level: escalation with deprecation warning, escalation with detached HEAD warning, backward compatibility verification.
- ConfigFileSource automatically supports the new flag via TaggerConfig (field already existed).
- Refactored tests to CalculateVersionTestSpec so both CLI and config file implementations are tested.
- Commits: a9b25a7, cd47345, 122d5b3

### Implementation notes (2026-05-30, continued)
- Renamed `TagResult.Error` to `TagResult.Warning` for semantic clarity.
- Policy violations (not on release branch, already tagged, snapshot version) return warnings, not errors.
- These conditions exit 0 by default, only exit 1 when `warningsAsErrors=true`.
- Updated all references in core, CLI, and Gradle plugin.
- Commit: 7f957a7

### Optional follow-ups under same theme
- Consider a small internal result type rename in `tag` path (`TagResult.Warning` vs `TagResult.Error`) if semantics remain warning-like for non-strict mode.
- Add a focused matrix test for `(command x format x warningsAsErrors x warning-present)` to prevent regressions.

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:check`
- Results: All tests pass, module check successful
