---
load_when: creating, executing, or reviewing work cards
cost: ~300 tokens
brief: versioned card format, test quality before commit, and closeout
---

# Work Card Protocol

**Current version: 2**

Cards track intent, decisions, and evidence—not routine activity. New or revised cards start with `**Work card protocol: 2**` below the title.

## Card content

- **Goal**: intended outcome.
- **Constraints and acceptance evidence**: task-specific boundaries and observable proof of success. For behavior changes, identify the behavior to test and follow [TESTING.md](TESTING.md) and the Testmints skill.
- **Decisions**: consequential choices, alternatives, and rationale; omit when unnecessary.
- **Progress**: current status or meaningful outcomes, when useful.
- **Resume note**: only when pausing or handing off; state remaining work, blockers, relevant uncommitted changes, and next step. Git records branch and commit state.

Use task-specific headings as needed. Link to shared repository guidance instead of copying its generic rules. Record exceptions and meaningful evidence; omit routine successful commands, volatile state, generic checklists, and activity logs.

## Before committing behavior changes

1. Write a test for the intended behavior and confirm it fails for the expected reason before implementation.
2. Review the test against Testmints guidance. Fix findings and repeat the review until no actionable test-quality issues remain.
3. Review and refine the implementation, then rerun relevant validation.
4. Commit only when no review findings remain unresolved. If a finding cannot be resolved, explain why and leave the production change uncommitted until its disposition is clear.

Passing tests alone does not establish commit readiness. Test-quality review is iterative and happens before commit, not at card closeout.

## Closeout

After the goal and acceptance evidence are satisfied, review the completed change against the card and applicable project guidance, resolve remaining issues, and confirm the evidence still holds. Then complete required repository closeout and move the card from `agents.d/work/` to `agents.d/work_completed/`. Keep unfinished or blocked cards active with a concise resume note; do not leave finished cards active.

## Versions

- **2** — Replaces routine checklists and command logs with concise decisions and acceptance evidence; adds iterative pre-commit test review and explicit closeout.
- **1** — Earlier checklist and command-log format. Completed cards remain unchanged; interpret them under their original guidance.
