# Tagger Test-Spec Hoist Parity Follow-up

## Goal
Hoist behavior-parity tests from CLI/plugin-exclusive suites into shared test specs where appropriate, while preserving intentional implementation-specific warning/output divergences.

## Constraints
- Keep behavior parity assertions at spec level; keep wiring/format/help/output-contract assertions implementation-specific.
- Use prior parity decisions as constraints: CLI deprecation-warning emission is intentionally divergent from Gradle plugin behavior.
- Before hoisting any warning-related test, classify the warning surface as: shared behavior vs intentional divergence, with evidence.
- Do not broaden scope into unrelated refactors; keep this card focused on test placement and any required minimal behavior alignment.
- Semver intent (initial): `[none]` (test-structure changes only). If implementation behavior changes are required to satisfy a spec hoist, escalate to `[patch]` and confirm with user.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Build a hoist candidate inventory across CLI/plugin-exclusive tests and classify each as `hoist`, `stay-exclusive`, or `blocked by divergence`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Resolve warning-support consistency gate before hoisting warning-related tests (document evidence and decisions)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Hoist approved candidates into shared specs (starting with detached-head tag behavior and lightweight-tag calculate-version behavior) and wire both CLI + plugin implementations
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add/adjust config-file parse failure parity coverage at shared-spec level (or documented spec-adapter equivalent) for both implementations
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Reconcile implementation-specific suites to remove redundant coverage and retain only invocation-specific assertions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

### Semver intent (initial)
- Expected scope: `[none]` for test hoists/reorganization only.
- If any hoist exposes true behavior mismatch requiring production code change, pause for semver escalation confirmation (`[patch]`) before proceeding.

### Known prior context to respect
- Prior card documented pushback on hoisting due warning inconsistency and locked specific divergence decisions.
- Candidate warning surfaces must be explicitly checked against:
  - shared strict-mode behavior (`warningsAsErrors`) parity targets, and
  - intentional CLI-only warning emission (notably deprecation warning output).

### Candidate list (seed)
- `TagCommandTest.allowDetachedHeadPermitsTaggingDetachedHead` → likely hoist to `TagTestSpec`.
- `LightweightTagErrorTest` behavior assertions (not formatting) → likely hoist target to calculate-version shared spec.
- `.tagger` invalid parse failure parity coverage across CLI + plugin → likely hoist/spec-adapter parity target.

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
