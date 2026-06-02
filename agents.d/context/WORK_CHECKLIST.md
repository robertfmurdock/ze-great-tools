# Work Card Implementation Guide

## Purpose
Defines work card structure, TDD cycle, and validation requirements for implementation tasks.

## When To Use
- Creating new work cards (user request: "create a work card")
- Executing any implementation task
- Reviewing work card compliance

## Critical Facts

### Work Card Location & Format
- Work cards are markdown files in `agents.d/work/`
- NOT Claude Code's built-in task tool
- Template structure:
  - Goal: one sentence
  - Constraints: boundaries, semver intent
  - Checklist: broad feature slices
  - Implementation Notes: discoveries, deviations
  - Validation: commands and results

### Required Checklist Items (Ordered)
1. First: `Review this work card for compliance with template and update to conform`
2. Second (if using subagents): `If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes`
3. Feature slices (broad, not micro-tasks)
4. Second-to-last: `Review changes against applicable playbooks and verify compliance`
5. Last: `Move this file to agents.d/work_completed/`

### Agent Cycle (Per Feature Slice)
1. **Test**: Write one test, confirm fail/pass reason
2. **Implement**: Simplest solution
3. **Refactor-light**: Clean up names, duplication, structure
4. **Verify pushable**: Run validation, ALL tests must pass
5. **Commit**: With semver annotation and co-authorship

### Refactoring Levels
- **Light**: During slices, clean what you just wrote
- **Final**: MANDATORY subagent, review ALL commits/files in work scope (see REFACTOR_AGENT.md)

### Semver Annotations
- `[major]`: breaking change
- `[minor]`: new backward-compatible functionality
- `[patch]`: bug fix, refactor, build output changes
- `[none]`: docs, work cards, build config (no output impact)

### CLI-Specific
- stdout = API (parseable), stderr = diagnostic
- Changing stdout format = `[major]`
- Improving stderr = `[patch]`

### Deprecation Workflow
1. Build and test new feature first
2. Mark old API deprecated (why, replacement, removal timing)
3. Test both APIs for backward compatibility
4. Remove only at major version boundaries

## Constraints

### Subagent Authorization
- Before ANY subagent spawn: ask user explicitly in-thread
- Record answer in Implementation Notes with date
- Re-ask in new threads even if previously authorized
- No explicit "yes" = single-agent mode

### Repository State
- Every checklist item = pushable state
- No failing tests committed
- Run `./gradlew check` before completion
- Mark checklist items complete as you go

### Adaptation
- Project guidelines override work card plans
- Update plan when constraints discovered
- Pause and ask user if semver impact increases
- Log discoveries in Implementation Notes

### Co-Authorship (Required)
All commits in live sessions: `Co-Authored-By: <Agent Name> <noreply@<agent-provider>.com>`

## Key Files
- Work cards: `agents.d/work/*.md`
- Completed: `agents.d/work_completed/*.md`
- Required reads:
  - `agents.d/context/PERSONA.md`
  - `agents.d/context/PLAYBOOK_CODE_STYLE.md` (code changes)
  - `agents.d/context/GRADLE_PLAYBOOK.md` (build changes)
  - `agents.d/context/GITHUB_ACTIONS_PLAYBOOK.md` (workflow changes)
  - `agents.d/context/REFACTOR_AGENT.md` (final refactor)

## Decisions
- Use Gradle wrapper (`./gradlew`) only
- Start with module-scoped validation
- Keep changes focused on impacted modules
- Follow existing patterns and ownership
- Prefer existing libraries over custom implementations

## Common Mistakes
- Work cards suggesting "tests may fail initially" (violates TDD cycle)
- Subagent references without authorization prompt item
- Batching checklist updates to end (mark complete as you go)
- Silently "fixing" intentional configurations
- Final refactor reviewing only latest changes (must review ALL commits)
- Spawning subagents without explicit user authorization
- Committing failing tests
- Not running `./gradlew check` before completion
