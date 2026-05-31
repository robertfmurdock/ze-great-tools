# Digger Guide and Why-Digger Upgrade

## Goal
Improve digger fit-assessment guidance and create a high-quality `why-digger.md` decision document backed by explicit user interrogation inputs.

## Constraints
- Follow `agents.d/context/WORK_CHECKLIST.md` work-card structure and completion flow.
- Keep guidance aligned with actual digger behavior in CLI/core (tag boundaries, commit parsing, regex override mechanics).
- Declare initial semver intent as `[patch]` because this updates user-facing CLI guide/help output and supporting docs.
- `why-digger.md` quality must be driven by explicit user-provided context; do not finalize rationale content from assumptions.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Define and run a structured user interrogation for why-digger quality inputs
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
[Subagent authorization: User approved subagent delegation 2026-05-31]

### User Interrogation Responses (2026-05-31)

1. **Workflow Context**: Created to extract richer contribution analytics from git history. Uses tags as natural boundaries defining "contributions" (work between versions). Focus: timing patterns between releases, individual contribution characteristics. Extends tag-based release marking with standardized contribution format.

2. **Not-For-You Cases**: Teams without git tags for releases. Primary anti-fit: no downstream plan for the data. (Note: External tracking systems like JIRA are complementary - git is mechanical truth vs. human time tracking. Real-time supported via current-contribution.)

3. **Design Principles**: "Surface contribution data in a consistent shape with maximum extractable information from git, focused on main-line releases, giving programmers flexibility in consumption." Single-pipeline focus (main releases, not hotfix/forks) is mechanical constraint, not philosophy.

4. **Scope Boundary**: In-scope: extraction only (commits, tags, timing, contributors, metadata). Out-of-scope: visualization, stats, pairing correlation, insights, validation, enforcement. Digger is non-judgmental; decision-making happens in consuming tools.

5. **Failure Modes**: Story ID regex mismatches for project-specific data. Inconsistent tag discipline during transition to tag-based releases (resolves with full adoption). Recovery: commit to consistent tagging, tune regex for project conventions.

6. **Success Criteria**: Indirect - digger works well when downstream analysis/process works consistently and delivers ongoing operational insights.

7. **Alternatives**: Key drivers for building rather than using existing tools: (a) Privacy/control - data stays inside org walls, no external API deps, (b) Universal git support - works anywhere git works, (c) Simple enough to just build rather than research alternatives.

8. **Prerequisites**: Tags mark version boundaries consistently. Git history depth: full for all-contribution-data, at least back to last tag for current-contribution-data (CI shallow clones can break this). Commit message conventions if extracting metadata. Downstream consumption plan for the data.

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
