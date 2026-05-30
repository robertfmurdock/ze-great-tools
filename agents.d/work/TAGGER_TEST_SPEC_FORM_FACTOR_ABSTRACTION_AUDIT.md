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
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Build inventory of implementation-specific tests in tagger CLI and plugin suites
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Classify each test as: `already-shared`, `should-hoist`, `correctly-implementation-specific`, or `needs-micro-api`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] For tests needing micro-APIs: design reusable assertion helpers that abstract form-factor differences
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Hoist appropriate tests to shared specs with micro-API abstractions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

### Semver intent (initial)
- Expected scope: `[none]` (test organization and abstraction refactor only)
- If changes require production code modifications to support better abstractions, escalate and confirm

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
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
