# Context System Compliance Improvements

## Goal

Improve agent adherence to mandatory process steps (refactor pass, checklist ordering, pre-existing violation fixes, tool safety) through targeted context enhancements without significantly increasing token costs.

## Constraints

- Token cost increase: ≤250 tokens across all modified files
- Changes must be backward-compatible with existing work cards
- Focus on preventing silent process skipping, not adding new processes
- Semver intent: `[none]` (agent documentation only, no build output impact)

## Checklist

- [ ] Review this work card for compliance with template and update to conform
- [ ] Analyze failure patterns and root causes
  - Map each issue (refactor skip, out-of-order checklist execution, hesitation on pre-existing violations, manual edits without IDEA tools) to context gaps
  - Identify which context files agents load at critical decision points
  - Determine minimal signal additions that prevent each failure mode
- [ ] Enhance WORK_CHECKLIST.md for process adherence
  - Add explicit "Final refactor pass (MANDATORY - see REFACTOR_AGENT.md)" as second-to-last checklist item
  - Strengthen ordering signal: Replace "(Ordered)" with "EXECUTE IN ORDER - marking items complete out of sequence violates work discipline"
  - Update "Common Mistakes" section: Add checklist reordering violation + refactor pass skipping
  - Add decision heuristic: "Pre-existing violations found during refactor = fix now (already in context)"
  - Token budget: ≤100 tokens added
- [ ] Enhance REFACTOR_AGENT.md for violation handling clarity
  - Add "Violation Handling" section: Critical/major violations = fix immediately (orchestrator already has context)
  - Clarify that "pre-existing" is not a reason to defer if severity is critical/major
  - Token budget: ≤60 tokens added
- [ ] Enhance PLAYBOOK_CODE_STYLE.md for IDEA tool preference
  - Add "Tool Safety" section: Prefer IDEA refactoring tools (extract function, rename, move) over manual edits when available
  - Add to "Common Mistakes": Manual edits without considering IDEA refactoring tools
  - Token budget: ≤60 tokens added
- [ ] Validate changes against real scenarios
  - Review modified context files against the three failure scenarios
  - Verify added guidance is specific and actionable (not generic reminders)
  - Confirm token cost increases are within budget
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

**Semver intent (initial):** `[none]` - agent documentation only, no build output impact

**Failure pattern analysis:**

1. **Refactor pass skip** (major): Agent completed feature work, marked checklist items, moved work card to completed without running mandatory final refactor. Root cause: Process step not explicit enough in working checklist, agent loses track during implementation focus.

2. **Out-of-order checklist execution** (major): Agents mark checklist items complete out of sequence, potentially skipping critical early steps (compliance review, subagent authorization). Root cause: "(Ordered)" signal is too weak - agents see it as suggestion not requirement.

3. **Pre-existing violation hesitation** (moderate): Refactor agent found 2 critical function length violations and asked what to do instead of fixing them. Root cause: Unclear guidance on handling pre-existing violations when agent already has context.

4. **Manual editing without IDEA tools** (minor): Agent manually edited test files without considering safer IDEA refactoring tools. Root cause: No explicit preference signal for tool-assisted refactoring over manual edits.

**Context loading pattern:**
- Agents reliably load WORK_CHECKLIST.md when working on work cards (confirmed by index.md gate)
- Refactor subagents load REFACTOR_AGENT.md (mandatory for final pass)
- PLAYBOOK_CODE_STYLE.md loads when modifying source code

**Strategy:**
- Make final refactor pass visually prominent in WORK_CHECKLIST.md (MANDATORY flag)
- Strengthen ordering requirement: Replace weak "(Ordered)" with explicit violation language
- Add decision heuristic directly where agents need it (at moment of decision)
- Keep additions terse, token-optimized (no prose, direct signals)
- Focus on "what to do when X happens" not "remember to do X"

**Token cost tracking:**
- Baseline measurements needed before edits
- Target: ≤250 tokens total across 3 files
- Will measure after implementation

## Validation

- Commands: 
  - Token count validation: `wc -w` before/after on modified files (approximate)
  - Scenario walkthrough: Review each modified file against failure scenarios
  - `./gradlew check` (no build impact expected, but verify)
- Results: (populated after implementation)
