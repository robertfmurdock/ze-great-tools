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
- [ ] Review this work card for compliance with template and update to conform
- [ ] CI catches README quality issues automatically
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify test detects missing help command references
  - Verify test detects duplicated field documentation
  - Verify test validates README exists and is readable
  - Verify test catches outdated example patterns
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes
Semver intent (initial): [none] - test infrastructure only, no impact on build output

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
