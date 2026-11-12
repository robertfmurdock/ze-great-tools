---
load_when: creating work cards, executing implementation tasks, reviewing work cards
cost: ~350 tokens
brief: versioned work card format, decisions, evidence, and handoffs
---

# Work Card Protocol

**Current version: 2**

Use for active work tracked in `agents.d/work/`. A card should preserve intent and help an agent make sound choices; it is not a transcript of routine work.

## Version history

- **Version 2** — Replaces the checklist and command-log format with task-specific acceptance evidence, consequential decisions, and concise handoffs. Shared repository rules remain in their existing instructions.
- **Version 1** — Historical format used by cards created before version 2. Completed cards remain unchanged and are interpreted according to the version they carry, if present.

## Card format

Start each new or revised card with `**Work card protocol: 2**` below its title. Keep only sections that carry useful information; task-specific headings are encouraged.

- **Goal**: the intended outcome in a sentence.
- **Constraints and acceptance evidence**: important boundaries and observable evidence that the outcome works. For behavior changes, identify the behavior that needs a test; follow [TESTING.md](TESTING.md) and the Testmints skill, including confirming a new test fails for the intended reason before implementation. Record that failure briefly when it is meaningful evidence.
- **Decisions**: include only consequential choices, alternatives considered, and why the choice fits the goal. Omit this section if no decision needs preserving.
- **Progress**: concise status or completed outcomes when needed to coordinate ongoing work. Avoid per-step activity logs.
- **Resume note**: include only when pausing or handing off; state remaining work, relevant uncommitted changes, blockers, and the next useful step. Git is the source of truth for branch and commit state.

## Shared rules

Follow repository-wide testing, validation, documentation, commit, push, delegation, and review instructions in `CLAUDE.md` and the applicable context documents. Cards should link to relevant guidance and record task-specific exceptions, risks, or evidence rather than repeat generic commands and procedures. Follow project semver guidance where changes affect shipped behavior.

Update the card when a decision changes the approach, scope, risk, or acceptance criteria. Do not record routine successful commands, unchanged repository state, volatile commit hashes, template-compliance steps, or generic process checkboxes. Close or move a card according to shared repository policy after its goal and acceptance evidence are satisfied.
