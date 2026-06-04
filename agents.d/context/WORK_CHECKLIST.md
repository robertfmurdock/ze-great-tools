---
load_when: creating work cards, executing implementation tasks, reviewing work card compliance
cost: ~500 tokens
brief: work card structure, execution protocol, handoff, semver, subagent rules
---

# Work Card Protocol

## Purpose
Work card structure, TDD cycle, validation for implementation tasks.

## When To Use
Creating/executing/reviewing work cards in `agents.d/work/*.md`.

## Critical Facts

### Template Sections
- **Goal**: one sentence
- **Constraints**: boundaries, semver intent
- **Checklist**: broad feature slices (not micro-tasks)
- **Current State**: commit SHA, uncommitted work, blockers, date
- **Implementation Notes**: date-stamped discoveries (newest first)
- **Validation**: commands, pass/fail status (update per item)

### Execution Order (sequential, no reordering)
1. Review card template compliance
2. If subagents: ask user authorization, record in notes
3. Feature slices (strict TDD per TESTING.md):
   - Load TESTING.md BEFORE code changes
   - Write ONE failing test, verify expected failure
   - Minimal implementation to pass
   - Refactor-light (just-written code only)
   - `./gradlew check -q --console=plain` passes
4. Final refactor: MANDATORY subagent (REFACTOR_AGENT.md), reviews ALL commits/files
5. Move to `agents.d/work_completed/`

### Semver Annotations
- `[major]`: breaking change
- `[minor]`: new backward-compatible feature
- `[patch]`: bug fix, refactor, build output changes
- `[none]`: docs, work cards, build config

CLI: stdout = API (changes = `[major]`), stderr = diagnostic (changes = `[patch]`)

Deprecation: build new → mark old deprecated → test both → remove at major boundary

## Constraints

### Subagents
- Ask user authorization BEFORE spawn (every thread, even if previously authorized)
- Record authorization in Implementation Notes with date
- No explicit "yes" = single-agent mode

### Repository State
- Every checklist item = pushable state
- Run `./gradlew check -q --console=plain` before marking complete
- **Goal verification**: When checklist states explicit goal/principle (DRY, SOLID, performance), verify implementation achieves goal before complete
  - Example: "DRY: single source" means reading from ONE location, not synchronized copies
  - Ask: "Does this achieve stated goal, or just solve tactical problem?"
  - Implementation convenience does not override explicit requirements
- Mark complete only after commit pushed
- Update Validation incrementally
- Never commit failing tests

### Handoff
Before pause: update Current State (SHA, uncommitted changes, status, blockers)

Resume: read Current State, verify git (`git log -1`, `git status`), run `./gradlew check -q --console=plain`, log handoff in notes

### Adaptation
Project guidelines override work card. Update plan when constraints discovered. Ask user if semver impact increases. Log discoveries in Implementation Notes.

## Key Files
Required reads:
- `agents.d/context/PERSONA.md`
- `agents.d/context/TESTING.md` (MANDATORY before any feature slice)
- `agents.d/context/PLAYBOOK_CODE_STYLE.md` (code changes)
- `agents.d/context/GRADLE_PLAYBOOK.md` (build changes)
- `agents.d/context/GIT_WORKFLOW.md` (commits, PRs)
- `agents.d/context/GITHUB_ACTIONS_PLAYBOOK.md` (workflow changes)
- `agents.d/context/REFACTOR_AGENT.md` (final refactor)

## Decisions
Use `./gradlew` only. Module-scoped validation. Focused changes per module. Follow existing patterns. Fix pre-existing violations during refactor.

## Common Mistakes
- Implementing code before writing test (violates TDD)
- Pattern-matching work as "just metadata" and skipping test (if behavior changes, test first)
- Not loading TESTING.md before implementation
- Using generic "Review changes" instead of "Final refactor via MANDATORY subagent (REFACTOR_AGENT.md)"
- Marking complete without goal verification (see Repository State section)
- Spawning subagents without authorization
- Marking items out of order
- Batching checklist updates
- Final refactor reviewing only recent changes (must review ALL)
- Skipping final refactor
- Not updating Current State before pause
- Not updating Validation incrementally
- Marking complete before push
- Resuming without verifying git state
