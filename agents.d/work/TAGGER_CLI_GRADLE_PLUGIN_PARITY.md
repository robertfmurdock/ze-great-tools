# Tagger CLI and Gradle Plugin Feature Parity

## Goal
Achieve feature parity between tagger CLI and Gradle plugin to ensure consistent behavior across invocation methods.

## Constraints
- Maintain backward compatibility for existing Gradle plugin users
- CLI-specific features (help text, format flags) are not in scope
- Deprecation warnings may be omitted from Gradle plugin if they would pollute build output
- Follow existing patterns: TaggerExtension properties → task properties → core behavior
- Declare initial semver intent and update if implementation discoveries change impact

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Audit CLI commands vs Gradle tasks to identify feature gaps
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Document intentional divergences (deprecation warnings, CLI-specific formatting)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Implement missing features with tests at spec level where appropriate
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

### Semver intent (initial)
- Expected scope: TBD after audit - likely `[patch]` if only completing existing feature implementations, `[minor]` if new behavior surfaces
- Re-evaluate during implementation if changes expand scope
- If new findings indicate semver may increase, pause and ask the user to confirm direction; record the decision with date

### Why this card exists
During warningsAsErrors implementation (2026-05-30), discovered that:
- Gradle plugin lacked `warningsAsErrors` support initially (CLI had it)
- Gradle plugin doesn't emit deprecation warnings (CLI does)
- Test spec assumption of feature parity was incorrect

This suggests potential gaps in other areas. Need systematic audit to ensure users get consistent behavior regardless of invocation method.

### Discovered gaps (fill in during audit)
- `warningsAsErrors` support: ✅ completed (commit cf74f67)
- Deprecation warning emission: intentional divergence (pollutes build logs)
- [audit findings go here]

### Design principles
- Feature behavior should match across implementations (exit codes, warning detection, version calculation)
- Output format differences are acceptable (CLI stderr vs Gradle logging)
- Deprecation warnings are CLI-specific since Gradle users see property deprecation via @Deprecated annotations
- New Gradle properties should follow existing convention: `TaggerExtension.property` → `Task.property` → core logic

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
