# Tagger CLI Flags Improvements

## Goal
Make tagger flags more intuitive and support common CI workflows by addressing Issue #310 friction points.

## Constraints
- Maintain backwards compatibility where possible (deprecated flags work for one release)
- Exit codes must remain consistent
- Flag behavior must be consistent across commands
- Follow PERSONA, code style playbook, and Gradle playbook
- Changes must comply with TestMints patterns and verification requirements from `.junie/guidelines.md`

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] Make --version flag clearer (required marker or auto-calculate)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Rename --disable-detached to --allow-detached-head with clear semantics
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add detached HEAD support to tag command
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add --dry-run flag to tag command
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

### Background from Issue #310
Issue #310 identified several CLI design issues causing confusion:
- `--version` flag on `tag` command isn't clearly marked as required
- `--disable-detached` flag name is inverted from its actual behavior
- `tag` command lacks detached HEAD support that `calculate-version` has
- No `--dry-run` option to preview what would happen

These design gaps force users into workarounds and cause failed pipeline runs during setup.

### Implementation Options Identified

**Option A (--version flag):** Mark as required in help text
```
--version=<text>  [required]  The version to tag.
```

**Option B (--version flag - preferred):** Make it optional and auto-calculate
- When `--version` omitted, internally run `calculate-version` logic
- Apply `--strip-snapshot` semantics automatically
- Allow `--version=X` as override for explicit cases
- Document this one-shot pattern in README

**Detached HEAD flag rename:**
```
--allow-detached-head=<true|false>  (default: false)
    Allow tagging when HEAD is detached (common in CI)
```

**Target error message format:**
```
Inappropriate configuration: HEAD is detached.
Re-run with --allow-detached-head=true to tolerate detached HEAD in CI.
See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md
```

**Dry-run output format:**
```
$ tagger tag --release-branch=main --version=0.0.1 --dry-run
Would create annotated tag '0.0.1' at abc123 on branch 'main'.
Would push to remote 'origin'.
(no changes made)
```

### Discoveries and Adaptations
[Agents log discoveries, deviations, or learned constraints here as they work]

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
