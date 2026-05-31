# CLI Markdown Externalization Investigation

## Goal
Identify and validate a practical path to move embedded CLI markdown content into standalone markdown files wherever feasible, improving maintainability and linkable GitHub documentation coverage.

## Constraints
- Follow the repository work-card process defined in `agents.d/context/WORK_CHECKLIST.md`.
- Keep behavior stable while investigating; this card should prioritize discovery, risk assessment, and implementation planning before broad refactors.
- Preserve current CLI help quality and existing test confidence during any pilot extraction.
- Semver intent (initial): `[none]` for investigation and planning updates only; reassess with user confirmation if implementation work expands scope.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Inventory markdown currently embedded in CLI codepaths and classify extraction candidates
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Capture which content is user-facing help/guide material vs runtime-only generated output
- [ ] Define extraction architecture for maximum practical coverage
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Specify file layout, loading strategy, fallback behavior, and packaging implications for JVM/native distributions
- [ ] Execute a pilot extraction on one representative CLI surface and evaluate results
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Validate rendering parity, test ergonomics, and linkability from GitHub docs
- [ ] Produce rollout recommendations with sequencing and risk controls for applying this broadly
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Include clear stop/go criteria and module-by-module order
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
[Semver intent (initial): [none] — this card is for investigation/planning and should avoid changing shipped behavior unless explicitly expanded]

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
