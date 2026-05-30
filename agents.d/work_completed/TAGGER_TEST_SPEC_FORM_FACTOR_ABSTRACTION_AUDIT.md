# Tagger Test Spec Form-Factor Abstraction Audit

## Goal
Review all CLI and plugin test implementations to identify opportunities to improve alignment with form-factor abstraction philosophy: shared specs assert behavior intent, adapters provide form-factor-specific verification strategies.

## Constraints
- Do not break existing passing tests during refactors
- Preserve test coverage and intent
- Focus on test organization and abstraction, not new feature coverage
- Follow form-factor abstraction guidance in `.junie/guidelines.md` section 6
- Semver intent (initial): `[none]` (test refactor only, no production behavior changes)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Build inventory of implementation-specific tests in tagger CLI and plugin suites
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Classify each test as: `already-shared`, `should-hoist`, `correctly-implementation-specific`, or `needs-micro-api`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] For tests needing micro-APIs: design reusable assertion helpers that abstract form-factor differences
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Hoist appropriate tests to shared specs with micro-API abstractions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move this file to agents.d/work_completed/

## Implementation Notes

### Semver intent (initial)
- Expected scope: `[none]` (test organization and abstraction refactor only)
- If changes require production code modifications to support better abstractions, escalate and confirm

### Progress Log
- 2026-05-30: Reviewed work card template compliance; checklist ordering and required terminal items conform to `WORK_CHECKLIST.md`.
- 2026-05-30: User authorized subagent delegation for this card (`yes`).
- 2026-05-30: Inventory complete across `tagger-cli` tests, `tagger-plugin-test` functional tests, and shared `tagger-test` specs.
- 2026-05-30: Classification complete. `CalculateVersionTestSpec` and `TagTestSpec` already host most cross-form-factor behavior; CLI help/format JSON tests and plugin task wiring/config precedence tests remain implementation-specific.
- 2026-05-30: Introduced new micro-API assertion `TestResult.Failure.assertHasDeprecationWarningEscalationError(...)` for cross-form-factor failure checks when warnings escalate to errors.
- 2026-05-30: Hoisted deprecation warnings-as-errors behavior to `CalculateVersionTestSpec` and removed overlapping CLI-only tests.
- 2026-05-30: Final refactor pass completed via subagent (`019e7a51-e7d2-7181-b7d2-d07836e0efb6`), including full-file review and cleanup that centralized duplicate assertion logic in shared spec defaults.
- 2026-05-30: Reviewed changes against `PLAYBOOK_CODE_STYLE.md`; assertions and test structure remain aligned with local style/TDD expectations.

### Scope
Focus areas:
- `command-line-tools/tagger-cli/src/commonTest/` — CLI-specific tests
- `tools-tests/tagger-plugin-test/src/functionalTest/` — Plugin-specific tests
- `tools-tests/tagger-test/src/commonMain/kotlin/` — Shared test specs

Look for:
- Duplicate test logic across CLI and plugin implementations
- Tests asserting the same behavior with form-factor-specific details baked in
- Missing micro-API methods for cross-cutting assertions (warnings, error messages, output formatting)
- Tests that should be shared but remain implementation-exclusive

### Examples of Form-Factor Abstraction
- **Good**: `assertHasDeprecationWarning(feature, replacement)` — semantic assertion, adapters translate
- **Needs work**: Duplicate tests checking for warnings with hardcoded format assumptions
- **Good**: Shared spec test + adapter-specific assertion helper
- **Needs work**: Implementation-specific test for behavior that should be universal

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:check :tools-tests:tagger-test:check :tools-tests:tagger-plugin-test:check --console=plain`
  - `./gradlew check --console=plain`
- Results:
  - PASS (after refactor adjustments for failure-output assertions)
  - PASS
