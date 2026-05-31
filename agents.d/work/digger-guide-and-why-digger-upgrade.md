# Digger Guide and Why-Digger Upgrade

## Goal
Improve digger fit-assessment guidance and create a high-quality `why-digger.md` decision document backed by explicit user interrogation inputs.

## Constraints
- Follow `agents.d/context/WORK_CHECKLIST.md` work-card structure and completion flow.
- Keep guidance aligned with actual digger behavior in CLI/core (tag boundaries, commit parsing, regex override mechanics).
- Declare initial semver intent as `[patch]` because this updates user-facing CLI guide/help output and supporting docs.
- `why-digger.md` quality must be driven by explicit user-provided context; do not finalize rationale content from assumptions.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Define and run a structured user interrogation for why-digger quality inputs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Produce a concise question set that captures workflow, constraints, failure modes, alternatives considered, and success criteria
  - Record user responses in Implementation Notes and treat them as source inputs for rationale content
  - Do not mark this item complete until the user has answered and ambiguities are resolved
- [ ] Upgrade `digger guide` content for operational fit assessment quality
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add explicit prerequisites, anti-fit criteria, and first-run workflow guidance
  - Document regex override contract including required named groups (`storyId`, `ease`)
  - Replace broad link target with direct docs pointer once available
- [ ] Create `docs/why-digger.md` from interrogated inputs and verified behavior
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Include: principles, not-for-you cases, scope boundary, tradeoffs, failure modes, evaluation criteria, and alternatives framing
  - Ensure claims are tied to either code behavior or captured user context, not conjecture
- [ ] Improve discoverability and regression protection
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Add `digger guide` discoverability in CLI README help section
  - Strengthen guide tests to assert key quality phrases and link expectations
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
[Semver intent (initial): [patch] - updates user-facing digger guide/help output and associated documentation.]
[Quality gate: `why-digger.md` must be built from explicit user interrogation responses before finalization.]

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
