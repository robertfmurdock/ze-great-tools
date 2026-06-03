---
load_when: creating work cards, executing implementation tasks, reviewing work card compliance
cost: ~500 tokens
brief: work card structure, execution protocol, handoff, semver, subagent rules
---

# Work Card Protocol

## Purpose
Work card structure, TDD cycle, and validation for implementation tasks.

## When To Use
- Creating new work cards
- Executing any implementation task
- Reviewing work card compliance

## Critical Facts

### File Location
`agents.d/work/*.md` (NOT Claude Code task tool)

### Template Sections
- **Goal**: one sentence
- **Constraints**: boundaries, semver intent
- **Checklist**: broad feature slices (not micro-tasks)
- **Current State**: commit SHA, uncommitted work, blockers, session date
- **Implementation Notes**: date-stamped discoveries (newest first)
- **Validation**: commands and pass/fail status (update per checklist item)

### Checklist Execution Order
Sequential execution; violates work discipline if out of order:
1. Review card for template compliance
2. If using subagents: ask user authorization, record in Implementation Notes
3. Feature slices (each follows strict TDD):
   - **BEFORE any code changes**: Load TESTING.md, locate/create test file, write ONE failing test, verify fails with expected reason
   - **Implement**: Minimal code to pass the test
   - **Refactor-light**: Clean what you just wrote
   - **Verify pushable**: `./gradlew check` must pass
   - See TESTING.md for complete TDD cycle
4. Final refactor (MANDATORY subagent - see REFACTOR_AGENT.md, reviews ALL commits/files in work scope)
5. Move file to `agents.d/work_completed/`

### Refactoring Levels
- **Light**: During slices, clean what you just wrote
- **Final**: MANDATORY subagent, review ALL commits/files in work scope

### Semver Annotations
- `[major]`: breaking change
- `[minor]`: new backward-compatible feature
- `[patch]`: bug fix, refactor, build output changes
- `[none]`: docs, work cards, build config (no output impact)

### CLI-Specific
- stdout = API (parseable), stderr = diagnostic
- Changing stdout format = `[major]`
- Improving stderr = `[patch]`

### Deprecation
Build new feature → mark old deprecated → test both → remove at major boundary

## Constraints

### Subagent Rules
- Before ANY subagent spawn: ask user explicitly in-thread
- Record authorization in Implementation Notes with date
- Re-ask in new threads even if previously authorized
- No explicit "yes" = single-agent mode

### Repository State
- Every checklist item = pushable state
- Run `./gradlew check` before marking complete
- Mark complete only after commit pushed
- Update Validation incrementally
- Never commit failing tests

### Handoff (Before Pause)
Update Current State: last SHA, uncommitted changes, checklist status, blockers

### Handoff (Resume)
- Read Current State
- Verify git state (`git log -1`, `git status`)
- Check `./gradlew check` passes
- Add handoff note to Implementation Notes

### Adaptation
- Project guidelines override work card
- Update plan when constraints discovered
- Ask user if semver impact increases
- Log discoveries in Implementation Notes

## Key Files
- Work: `agents.d/work/*.md`
- Completed: `agents.d/work_completed/*.md`
- Required reads:
  - `agents.d/context/PERSONA.md`
  - `agents.d/context/TESTING.md` (MANDATORY: load BEFORE implementing any feature slice)
  - `agents.d/context/PLAYBOOK_CODE_STYLE.md` (code changes)
  - `agents.d/context/GRADLE_PLAYBOOK.md` (build changes)
  - `agents.d/context/GIT_WORKFLOW.md` (commits, PRs)
  - `agents.d/context/GITHUB_ACTIONS_PLAYBOOK.md` (workflow changes)
  - `agents.d/context/REFACTOR_AGENT.md` (final refactor)

## Decisions
- Use `./gradlew` only
- Module-scoped validation
- Focused changes per module
- Follow existing patterns
- Fix pre-existing violations found during refactor

## Common Mistakes
- **Implementing code before writing test** (violates TDD - see 2026-06-03 incident in strengthen-test-first-discipline.md)
- **Pattern-matching work as "just metadata/configuration" and skipping test attempt** (if behavior changes, test first)
- **Not loading TESTING.md before starting implementation** (required before any feature slice)
- Spawning subagents without authorization
- Marking items out of order
- Batching checklist updates
- Final refactor reviewing only recent changes (must review ALL)
- Skipping final refactor
- Not updating Current State before pause
- Not updating Validation incrementally
- Marking complete before push
- Resuming without verifying git state
