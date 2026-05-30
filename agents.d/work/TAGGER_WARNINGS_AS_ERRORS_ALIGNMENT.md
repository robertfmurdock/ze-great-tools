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
- [ ] Review this work card for compliance with template and update to conform
- [ ] Define and lock expected warnings policy surface for each command
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Implement `calculate-version` warnings escalation behind `warningsAsErrors`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Align `tag` command semantics and naming so warning/error behavior is explicit and unsurprising
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

### Semver intent (initial)
- Expected scope: `[patch]` (behavior-alignment bug fix under existing `warningsAsErrors` policy).
- Re-evaluate during implementation if changes expand beyond alignment (for example, new flags or stdout contract changes).
- If new findings indicate semver may increase (`[patch]` → `[minor]`/`[major]`), pause and ask the user to confirm direction; record the decision with date.

### Why this card exists
- Current `warningsAsErrors` behavior is asymmetric: it influences `tag` handling but does not currently escalate warning emissions from `calculate-version`.
- This creates a policy mismatch: users can enable strict mode and still get warning-only success exits from version calculation paths.

### Candidate warnings to include under strict mode
- `calculate-version` deprecation warning when `--disable-detached` is used.
- `calculate-version` release-risk warning when running `--allow-detached-head` on release branch without upstream tracking.

### Design guardrails
- Do not reclassify existing hard failures as warnings.
- Keep warning detection source-of-truth close to core result models (avoid fragile text parsing where possible).
- Preserve current JSON schema; strictness should be represented via process exit status, not response shape changes.

### Optional follow-ups under same theme
- Consider a small internal result type rename in `tag` path (`TagResult.Warning` vs `TagResult.Error`) if semantics remain warning-like for non-strict mode.
- Add a focused matrix test for `(command x format x warningsAsErrors x warning-present)` to prevent regressions.

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
