# Tagger Output Format Improvements

## Goal
Improve calculate-version help text and add --strip-snapshot flag so AI agents and users can reliably script CI/versioning workflows.

## Constraints
- Maintain backwards compatibility for current behavior (version on stdout, diagnostics on stderr)
- Errors must always go to stderr, never stdout
- Each checklist item must result in a pushable state
- Follow TestMints patterns from `.junie/guidelines.md`
- Use `./gradlew` for all validation tasks

## Feature Details

### Help Text Improvements
**Problem:** AI agents and users encountering tagger don't understand the stdout/stderr split, leading to fragile parsing with `2>/dev/null` that hides real errors.

**Current help text:**
```
  -q, --quiet          Suppress welcome message
  --format=text|json   Output format
```

**Improved help text:**
```
  -q, --quiet          Suppress welcome message. Version goes to stdout,
                       diagnostics to stderr (safe for: VERSION=$(tagger -q ...))
  --format=text|json   Output format (default: text). Use json for structured data
                       with version, snapshot status, and diagnostic flags.
```

**Additional help section to add:**

```
Output:
  Text format writes version to stdout, diagnostics to stderr.
  Command substitution captures only stdout: VERSION=$(tagger -q calculate-version ...)
  
  Snapshot reasons (DIRTY, AHEAD, etc.) on stderr explain why -SNAPSHOT was added.
  Use --strip-snapshot to remove -SNAPSHOT suffix for release version numbers.
```

### Strip Snapshot Flag
**Target:** Add `--strip-snapshot` for clean CI extraction of release versions

**Use case:** CI needs `1.2.3` not `1.2.3-SNAPSHOT` for tagging, but users still want to see diagnostic reasons on stderr.

Current workaround:
```bash
VERSION=$(tagger -q calculate-version ... | sed 's/-SNAPSHOT$//')
```

Expected:
```bash
VERSION=$(tagger -q calculate-version --strip-snapshot ...)
# Returns: 1.2.3 (even if would normally be 1.2.3-SNAPSHOT)
# Diagnostics still on stderr
```

### Document -SNAPSHOT and Status Flags
**Target:** Add help text / README section explaining version output

**-SNAPSHOT semantics:**
- Appended when HEAD is not yet tagged with the calculated version
- After `tagger tag`: subsequent `calculate-version` returns bare version
- `--strip-snapshot` removes suffix for CI use cases that need release version

**Status flags (on stderr):**
- `DIRTY` - uncommitted changes in working directory
- `AHEAD` - local branch ahead of remote
- `BEHIND` - local branch behind remote  
- `NOT_RELEASE_BRANCH` - not on configured release branch
- `NO_NEW_VERSION` - no new commits since last tag
- `FORCED` - `--force-snapshot=true` was used

Document what triggers each flag and how to resolve it.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] Improve --quiet and --format help text to document stdout/stderr split
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add --strip-snapshot flag for CI extraction
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add Output section to help text explaining stdout/stderr usage patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Document -SNAPSHOT semantics and status flags in help/README
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass (code style, patterns, efficiency)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

**Design decision:** After reviewing Issue #310 and existing behavior, determined that:
- Current behavior is correct: version on stdout, diagnostics on stderr
- Tests confirm snapshot reasons already go to stderr with `err = true`
- Problem is discoverability: agents/users don't know about the stdout/stderr split
- Solution: improve help text so agents notice the pattern, add `--strip-snapshot` for CI use case
- No behavior changes needed, only documentation improvements

**Background context from Issue #310:**
- AI agent using tagger in PowerShell didn't understand stdout/stderr separation
- Used `2>/dev/null` to suppress npm warnings, which also hid real tagger errors
- Ended up with fragile regex parsing instead of simple command substitution
- Root cause: help text doesn't explain that `VERSION=$(tagger -q ...)` safely captures stdout
- `-SNAPSHOT` suffix semantics are undocumented
- Status flags meanings are undocumented

**Current behavior (confirmed correct):**
- Version goes to stdout: `echo "0.0.1-SNAPSHOT"`
- Snapshot reasons go to stderr: `echo "[DIRTY, NO_NEW_VERSION]" >&2`
- Command substitution captures stdout only: `VERSION=$(tagger -q calculate-version ...)`
- Existing tests verify this with `result.stdout` vs `result.stderr`

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
