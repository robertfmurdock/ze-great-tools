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
- [ ] Review this work card for compliance with template and update to conform
- [ ] Load agents.d/context/index.md and verify task-gated documents loaded
- [ ] Analyze improve-gradle-plugin-help.md line 65 failure mode in detail
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Checklist item: "Refactor guide tasks to share content with CLI guide markdown"
  - Explicit goal: "DRY principle: single source of truth"
  - Implementation: Resource copying creating 2 copies (1 in CLI, 1 in plugin)
  - Completion criteria gap: Agent marked complete without verifying "single source of truth" achieved
  - Document what verification step would have caught this
- [ ] Research where goal-verification guidance belongs
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Option 1: Add to WORK_CHECKLIST.md "Common Mistakes" section
  - Option 2: Add to PERSONA.md decision heuristics
  - Option 3: Add to PERSONA_EXTENDED.md as architectural risk pattern
  - Option 4: Add checklist sub-step requirement when items state explicit goals/principles
  - Document tradeoff between visibility and token cost
- [ ] Draft goal-verification guidance
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Key principle: If checklist item states explicit goal/principle (DRY, SOLID, performance target), implementation must be verified against that goal before marking complete
  - Wording should prevent rationalization like "simpler approach" when it contradicts stated goal
  - Should trigger agent to ask: "Does this implementation actually achieve the stated goal, or just solve the tactical problem?"
  - Draft specific language that would have caught line 65 failure
- [ ] Implement guidance in chosen location
  - Agent cycle: test → implement → refactor-light → verify pushable
  - MANDATORY: Load TESTING.md before implementation (even for docs)
  - If modifying WORK_CHECKLIST.md, maintain ~500 token target
  - If modifying PERSONA files, maintain token targets
  - Consider adding example failure (line 65 case) to illustrate pattern
- [ ] Spawn context-rewrite subagent if any context file was modified
  - Load agents.d/prompts/context-rewrite.md
  - Before spawning: ask user for explicit authorization and record in Implementation Notes
  - Re-optimize for token efficiency
  - Assess for loss of original intent
  - Correct integrity issues
  - Record results in Implementation Notes
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
  - Before spawning: ask user for explicit authorization and record in Implementation Notes
  - Documentation-only work, focus on clarity and consistency
  - Review all modified files
- [ ] Review changes against applicable playbooks and verify compliance
  - GIT_WORKFLOW.md: commit message standards
- [ ] Move this file to agents.d/work_completed/

## Current State
- Date: 2026-06-03
- Last commit: 4a1b6c1b Fix DRY violation: de-duplicate plugin guide markdown with build-time copying
- Status: Ready to start
- Blockers: None
- Uncommitted work: None

## Implementation Notes
*Date-stamp discoveries here, newest first*

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
