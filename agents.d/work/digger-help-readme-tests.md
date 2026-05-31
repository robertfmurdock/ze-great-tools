# Automated README Quality Assurance

## Goal
Documentation stays accurate and high-quality through automated verification that catches drift before users see it.

## Constraints
- Tests catch when README falls out of sync with CLI behavior
- README must reference `digger --help` and subcommand help for detailed docs
- README must not duplicate what's already in help output (prevents staleness)
- CLI examples in README must use current, working patterns
- Tests run on every check/build
- Semver intent: `[none]` - adds test infrastructure without affecting build output

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] CI catches README quality issues automatically
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify test detects missing help command references
  - Verify test detects duplicated field documentation
  - Verify test validates README exists and is readable
  - Verify test catches outdated example patterns
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move to agents.d/work_completed/

## Implementation Notes
Semver intent (initial): [none] - test infrastructure only, no impact on build output
[2026-05-31] User explicitly authorized subagent delegation for this card (yes).

## Validation
- Commands: `./gradlew :command-line-tools:digger-cli:jvmTest`, `./gradlew check`
- Results: All tests passing, full cross-module check passed
