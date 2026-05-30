# Tagger CLI/Gradle Exhaustive Parity Audit and Implementation

## Goal
Produce an exhaustive, evidence-backed parity matrix between tagger CLI and Gradle plugin, then close as many behavior gaps as practical with spec-level coverage and explicit documentation of approved divergences.

## Constraints
- Maintain backward compatibility unless user explicitly approves a breaking change.
- Prefer spec-level tests for cross-implementation behavior; keep implementation-level tests for invocation-specific wiring/formatting only.
- Treat parity as behavior parity first (results, warnings, errors, side effects), not only option/property presence.
- Do not assume a feature is parity-complete because one layer has tests; prove both paths via shared specs or equivalent cross-layer tests.
- For CLI tools, stdout is API; avoid changing CLI stdout formats unless intentionally scoped and semver-reviewed.
- Any newly discovered intentional divergence must be documented with rationale and owner approval in this card before implementation continues.
- Semver intent (initial): `[patch]` for parity fixes and tests; if parity requires net-new backward-compatible capability, escalate to `[minor]` and confirm with user before proceeding.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Build a complete parity inventory and gap matrix across CLI, Gradle extension, Gradle tasks, `.tagger` config, docs, and tests
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Classify each gap as implement-now, intentional divergence, or deferred with rationale and explicit risk notes
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Implement prioritized parity gaps in small slices with spec-level coverage and backward-compat checks for old/new paths
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Reconcile docs and help/config guidance with finalized parity decisions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

### Semver intent (initial)
- Expected scope: `[patch]` for parity fixes and test alignment; may become `[minor]` if gaps require adding new backward-compatible plugin or CLI capabilities.
- If semver impact increases, pause and confirm with user before proceeding; record decision with date.

### Caveats to improve execution success
- Do not stop at a single discovered/fixed gap; parity inventory must continue until all known feature surfaces are enumerated.
- Produce and keep updating a written matrix in this card as the source of truth (feature, CLI status, Gradle status, config support, test coverage, decision, owner).
- For every claimed parity feature, include evidence pointers (file paths/tests) in the matrix.
- Treat prior completed cards as context, not proof; re-verify against current code.
- Avoid “parity by inference” (e.g., extension property exists) without behavior validation.
- Keep slices pushable: each checklist item should end with passing targeted checks and updated matrix state.

### Working matrix (to fill during execution)
- [feature row template] Feature | CLI | Gradle Extension | Gradle Tasks | `.tagger` | Shared Spec Coverage | Decision | Evidence

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
