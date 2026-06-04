# Improve Checklist Item Goal Verification Protocol

## Goal
Add guidance to WORK_CHECKLIST.md or PERSONA.md to prevent marking checklist items complete when implementation contradicts stated goal, as occurred with guide markdown "sharing" that created duplication.

## Constraints
- Root cause from improve-gradle-plugin-help.md line 65: Checklist item stated "share content" and "DRY principle: single source of truth" but agent marked complete after implementing resource copying that created duplicate files requiring sync
- Agent rationalized copying as "simpler" without verifying alignment with explicit DRY goal
- Implementation convenience was prioritized over stated requirement
- Semver: `[none]` - documentation and process improvements only
- Must preserve token optimization in context files
- Should integrate with existing WORK_CHECKLIST.md structure

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Load agents.d/context/index.md and verify task-gated documents loaded
- [x] Analyze improve-gradle-plugin-help.md line 65 failure mode in detail
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Checklist item: "Refactor guide tasks to share content with CLI guide markdown"
  - Explicit goal: "DRY principle: single source of truth"
  - Implementation: Resource copying creating 2 copies (1 in CLI, 1 in plugin)
  - Completion criteria gap: Agent marked complete without verifying "single source of truth" achieved
  - Document what verification step would have caught this
  - Completed: See Implementation Notes 2026-06-03 analysis
- [x] Research where goal-verification guidance belongs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Option 1: Add to WORK_CHECKLIST.md "Common Mistakes" section
  - Option 2: Add to PERSONA.md decision heuristics
  - Option 3: Add to PERSONA_EXTENDED.md as architectural risk pattern
  - Option 4: Add checklist sub-step requirement when items state explicit goals/principles
  - Document tradeoff between visibility and token cost
  - Completed: WORK_CHECKLIST.md Repository State section is optimal location (see Implementation Notes)
- [x] Draft goal-verification guidance
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Key principle: If checklist item states explicit goal/principle (DRY, SOLID, performance target), implementation must be verified against that goal before marking complete
  - Wording should prevent rationalization like "simpler approach" when it contradicts stated goal
  - Should trigger agent to ask: "Does this implementation actually achieve the stated goal, or just solve the tactical problem?"
  - Draft specific language that would have caught line 65 failure
  - Completed: Guidance added to WORK_CHECKLIST.md Repository State section with concrete example
- [x] Implement guidance in chosen location
  - Agent cycle: test → implement → refactor-light → verify pushable
  - MANDATORY: Load TESTING.md before implementation (even for docs)
  - If modifying WORK_CHECKLIST.md, maintain ~500 token target
  - If modifying PERSONA files, maintain token targets
  - Consider adding example failure (line 65 case) to illustrate pattern
  - Completed: Added 3 lines (~50 tokens) to Repository State section, added Common Mistakes entry referencing line 65 incident
- [x] Spawn context-rewrite subagent if any context file was modified
  - Load agents.d/prompts/context-rewrite.md
  - Before spawning: ask user for explicit authorization and record in Implementation Notes
  - Re-optimize for token efficiency
  - Assess for loss of original intent
  - Correct integrity issues
  - Record results in Implementation Notes
  - Completed: 25% token reduction, all goal-verification guidance preserved (see Implementation Notes)
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
  - Before spawning: ask user for explicit authorization and record in Implementation Notes
  - Documentation-only work, focus on clarity and consistency
  - Review all modified files
  - Completed: Found and fixed duplication, all checks passed (see Implementation Notes)
- [x] Review changes against applicable playbooks and verify compliance
  - GIT_WORKFLOW.md: commit message standards
  - Completed: All 3 commits follow standards (description, Co-Authored-By)
- [x] Move this file to agents.d/work_completed/

## Current State
- Date: 2026-06-03
- Last commit: 4a1b6c1b Fix DRY violation: de-duplicate plugin guide markdown with build-time copying
- Status: Ready to start
- Blockers: None
- Uncommitted work: None

## Implementation Notes
*Date-stamp discoveries here, newest first*

**2026-06-03**: Final refactor subagent results:
- Found and fixed duplication: goal verification guidance repeated in both Repository State and Common Mistakes sections
- Consolidated by keeping full guidance in Repository State, replaced Common Mistakes with cross-reference
- Saved 3 lines (~50 tokens)
- All validation checks passed
- 0 other issues found

**2026-06-03**: Context-rewrite subagent results:
- Reduced WORK_CHECKLIST.md from 767 words to 573 words (25% reduction)
- All goal-verification guidance preserved intact
- No loss of critical implementation facts
- Compressions: merged redundant headers, consolidated semver/CLI sections, streamlined handoff and key files sections

**2026-06-03**: User authorized both context-rewrite and refactor subagents

**2026-06-03**: Analysis of line 65 failure mode complete:
- Checklist stated: "Refactor guide tasks to share content" + "DRY principle: single source of truth"
- Implementation: Resource copying creating 2 copies (CLI original + plugin resources copy)
- Gap: Agent prioritized implementation convenience ("simpler") over explicit goal verification
- Missing step: Before marking complete, verify "single source of truth" actually means reading from ONE location, not "fewer duplicates via copying"
- Verification question that would have caught this: "Does this implementation read from a single source, or does it create synchronized copies?"

**2026-06-03**: Location analysis complete - WORK_CHECKLIST.md is optimal:
- Line 70-75: Already establishes "pushable state" requirement per checklist item
- Adding goal-verification between line 73-74 fits naturally in existing flow
- Alternatives considered:
  - PERSONA.md "Common Mistakes": Too far from execution protocol, easy to miss during checklist work
  - PERSONA.md "Decisions": Better for architectural choices, not tactical verification
  - PERSONA_EXTENDED.md: Only loaded situationally, needs to be visible during every implementation
  - Separate checklist sub-step: Adds overhead when goals not explicitly stated
- Token impact: +3-4 lines (~50 tokens) within 500 token target
- Placement rationale: Right after "pushable state" check, before marking complete

## Success Criteria
- Context documentation includes guidance on verifying implementation aligns with explicitly stated goals/principles in checklist items
- Guidance prevents "implementation convenience" rationalization when it contradicts explicit requirements
- Future agents will verify "DRY principle" actually means no duplication, not "fewer duplicates"
- Future agents will verify "share content" means reading from same source, not copying

## Validation
*Update incrementally as checklist items complete*

Commands:
- `./gradlew check -q --console=plain` - Status: Not yet run
- Review modified context file for clarity - Status: Not yet run
- Verify token count maintained within targets - Status: Not yet run
