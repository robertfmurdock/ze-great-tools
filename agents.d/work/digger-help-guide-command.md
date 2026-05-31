# Fit Assessment and Workflow Philosophy Guide

## Goal
Users can run `digger guide` to quickly assess whether digger fits their needs and understand its workflow philosophy.

## Constraints
- Users see clear "Use digger when..." criteria
- Users see clear "Do not use digger when..." anti-patterns
- Best practices for contribution tracking are explicit
- Workflow philosophy explains git-history-as-truth and tag-based boundaries
- Guide links to deeper documentation when available
- Content tested for key decision-making phrases
- Semver intent: `[minor]` - adds new subcommand functionality

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Users can assess fit for their use case from guide output
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify "use when" and "don't use when" criteria are clear
  - Verify workflow philosophy explains core principles
  - Verify best practices are actionable
  - Verify guide is accessible via `digger guide` command
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes
Semver intent (initial): [minor] - adds new user-facing subcommand

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
