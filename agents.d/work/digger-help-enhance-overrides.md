# Comprehensive Inline Help Documentation

## Goal
Users get complete, contextual help at every command level with practical examples, field explanations, and CI integration patterns directly in their terminal.

## Constraints
- Main command help shows typical CI/build script usage patterns
- Subcommand help explains what each field means (storyId, semver, ease, etc.)
- CI integration examples demonstrate real-world automation patterns
- Date/time formats clearly documented (ISO 8601)
- Regex customization options visible and explained
- Help text guides users to guide command for deeper philosophy
- Semver intent: `[patch]` - improves help documentation without changing functionality

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Users see practical usage examples at main command level
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify overview explains what digger does
  - Verify typical script usage patterns are clear
- [ ] Users understand contribution data fields from current-contribution-data help
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify field descriptions explain purpose and format
  - Verify CI integration examples are actionable
- [ ] Users understand contribution boundaries from all-contribution-data help
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify explanation of tag-based subdivision is clear
  - Verify output structure is documented
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes
Semver intent (initial): [patch] - enhancing help text without changing CLI behavior or functionality

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
