# Improve Work Card Template Refactor Pass Clarity

## Goal
Update work card template and WORK_CHECKLIST.md to prevent skipping mandatory REFACTOR_AGENT.md subagent pass by making requirements explicit and unambiguous.

## Constraints
- Root cause from improve-gradle-plugin-help.md line 83: Generic checklist wording "Review changes against applicable playbooks" did not trigger recognition that REFACTOR_AGENT.md adversarial quality audit with subagent was mandatory
- WORK_CHECKLIST.md clearly states final refactor is MANDATORY with subagent (line 40)
- Work card checklist item was too generic and conflated lightweight review with mandatory refactor phase
- Semver: `[none]` - documentation and template changes only
- Must preserve existing WORK_CHECKLIST.md structure and token optimization
- Template changes should make final refactor pass explicit and separate from other review steps

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Load agents.d/context/index.md and verify task-gated documents loaded
- [ ] Analyze improve-gradle-plugin-help.md failure mode
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Line 83 root cause: Generic "Review changes against applicable playbooks" wording
  - WORK_CHECKLIST.md states final refactor is MANDATORY (line 40, line 44)
  - Gap: Checklist item didn't explicitly mention REFACTOR_AGENT.md or subagent requirement
  - Document what specific wording would have prevented this failure
- [ ] Review current work card template (if exists) or common patterns in agents.d/work_completed/
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Check 5-10 recent work cards for final refactor checklist wording patterns
  - Identify which cards have explicit "REFACTOR_AGENT.md subagent" vs generic "review" wording
  - Document successful vs problematic patterns
- [ ] Propose checklist item rewording for final refactor pass
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Must clearly distinguish lightweight refactor (during slices) from mandatory final refactor
  - Must explicitly reference REFACTOR_AGENT.md
  - Must explicitly state subagent requirement
  - Must include reminder about authorization requirement
  - Draft specific wording that would have caught the line 83 failure
- [ ] Update WORK_CHECKLIST.md if needed to strengthen guidance
  - Agent cycle: test → implement → refactor-light → verify pushable
  - MANDATORY: Load TESTING.md before making changes (even for docs)
  - Check if "Common Mistakes" section should add this failure pattern
  - Check if Checklist Execution Order section (lines 31-42) needs clarification
  - Use context-rewrite.md optimization if making content changes
- [ ] Spawn context-rewrite subagent if WORK_CHECKLIST.md was modified
  - Load agents.d/prompts/context-rewrite.md
  - Before spawning: ask user for explicit authorization and record in Implementation Notes
  - Re-optimize modified content for token efficiency (500 token target)
  - Assess for loss of original intent
  - Correct any integrity issues
  - Record results in Implementation Notes
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
  - Before spawning: ask user for explicit authorization and record in Implementation Notes
  - This work is documentation-only, so refactor focuses on clarity and consistency
  - Reviews all modified files for token efficiency and clarity
- [ ] Review changes against applicable playbooks and verify compliance
  - GIT_WORKFLOW.md: commit message standards
  - Document compliance
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
- WORK_CHECKLIST.md explicitly mentions final refactor requires REFACTOR_AGENT.md subagent
- Work card template (or common patterns) includes explicit final refactor checklist item mentioning REFACTOR_AGENT.md
- Future agents cannot confuse lightweight review with mandatory adversarial refactor phase
- All changes maintain token efficiency (WORK_CHECKLIST.md stays ~500 tokens per frontmatter)

## Validation
*Update incrementally as checklist items complete*

Commands:
- `./gradlew check -q --console=plain` - Status: Not yet run
- Review updated WORK_CHECKLIST.md for clarity improvements - Status: Not yet run
- Verify token count if modified (~500 target) - Status: Not yet run
