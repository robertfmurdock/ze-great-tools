# Rich Markdown-Formatted Help Output

## Goal
Digger's help text displays with rich formatting (bold, code blocks, tables) matching tagger's help system quality.

## Constraints
- Users see structured, readable help with visual hierarchy
- Code examples appear in formatted blocks
- Tables render properly for field descriptions and option lists
- Default values display inline with options
- Implementation follows tagger's established pattern
- Semver intent: `[patch]` - improves help output without changing functionality

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Enable rich markdown rendering in help output
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify bold/emphasis, code blocks, and tables render correctly in terminal
  - Verify default values display with options
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes
Semver intent (initial): [patch] - enhancing help output formatting without changing CLI behavior or functionality

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
