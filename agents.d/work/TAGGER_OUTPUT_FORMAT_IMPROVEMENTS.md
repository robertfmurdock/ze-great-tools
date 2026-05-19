# Task: Fix tagger quiet mode and document output format

## Goal
Improve calculate-version output handling for CI/scripting use cases, making quiet mode truly quiet and documenting output semantics clearly.

## Background
Issue #310 identified output format issues that complicate CI integration:
- Quiet mode (`-q`) still outputs multiple lines including status flags
- `-SNAPSHOT` suffix semantics are undocumented
- Status flags (`DIRTY`, `AHEAD`, etc.) are undocumented
- No clear stdout vs stderr separation, making parsing fragile

These gaps force CI users to write brittle extraction logic with regex and `sed` pipelines.

## Hard constraints
- Maintain backwards compatibility for text output (default behavior)
- Don't break existing CI scripts that parse current format
- Ensure errors always go to stderr, never stdout

## What to change

### 1. Make quiet mode truly quiet (Issue #310 item #7)

**Current behavior:**
```
$ tagger -q calculate-version --release-branch=main
0.0.1-SNAPSHOT
[DIRTY, AHEAD, BEHIND, NOT_RELEASE_BRANCH]
```

**Target behavior:**
```
$ tagger -q calculate-version --release-branch=main
0.0.1-SNAPSHOT
```
(Status flags go to stderr or are hidden entirely)

**Implementation:**
- In quiet mode, output exactly one line to stdout: the version
- Move status flags to stderr or suppress them entirely in quiet mode
- Keep "Welcome to Tagger CLI." suppressed (already working)
- Ensure all diagnostic messages go to stderr, not stdout
- Consider: should quiet mode strip `-SNAPSHOT`? (Probably not, but add flag for it)

### 2. Document -SNAPSHOT semantics (Issue #310 item #8)

**Current behavior:**
`-SNAPSHOT` appears in output but when/why is unclear.

**Findings from experimentation:**
- Local with untracked files: `0.0.1-SNAPSHOT [DIRTY, NO_NEW_VERSION]`
- Local on release branch, clean: `0.0.1-SNAPSHOT` (still snapshot)
- Detached HEAD: `0.0.1-SNAPSHOT [AHEAD, BEHIND, NOT_RELEASE_BRANCH]`
- After tagging: `0.0.1` (no snapshot)

**Meaning:**
`-SNAPSHOT` means "this version doesn't have an annotated tag at HEAD yet"

**Documentation needed:**
- README section explaining when `-SNAPSHOT` is appended
- Explanation: "appended whenever current HEAD is not yet tagged with calculated version"
- Note: "after `tagger tag` runs, subsequent `calculate-version` returns bare version"
- Add to help text for `calculate-version` command

### 3. Document status flags (Issue #310 item #8)

**Status flags observed:**
- `DIRTY` - working directory has uncommitted changes
- `AHEAD` - local branch ahead of remote
- `BEHIND` - local branch behind remote
- `NOT_RELEASE_BRANCH` - not on configured release branch
- `NO_NEW_VERSION` - no new commits since last tag

**Documentation needed:**
- Document each flag: what triggers it, whether it blocks tagging, how to clear it
- Add to README with examples
- Consider adding `--explain-flags` option that describes each flag in detail
- Update help text to mention flag meanings

### 4. Add --strip-snapshot flag (Issue #310 item #8)

**Use case:**
CI flows often want just the bare release version without `-SNAPSHOT`.

**Current workaround:**
```bash
VERSION=$(tagger -q calculate-version ... | sed 's/-SNAPSHOT$//')
```

**Target behavior:**
```bash
VERSION=$(tagger -q calculate-version --strip-snapshot ...)
# Returns: 0.0.1 (no SNAPSHOT suffix)
```

**Implementation:**
- Add `--strip-snapshot` flag to `calculate-version` command
- When enabled, remove `-SNAPSHOT` suffix from output
- Works orthogonally with `-q` flag
- Document as the recommended CI pattern

### 5. Consider structured output format (Issue #310 item #7)

**Optional enhancement:**
Add `--format` option for stable machine-readable output.

**Options:**
- `--format=plain` (default) - current text output
- `--format=version-only` - just the version string
- `--format=json` - structured JSON

**JSON example:**
```json
{
  "version": "0.0.1",
  "snapshot": true,
  "flags": ["DIRTY", "AHEAD"]
}
```

**Implementation:**
- Add `--format` flag to `calculate-version`
- Implement format handlers
- JSON output should be valid, parseable
- Document JSON schema in README

### 6. Ensure stdout/stderr separation

**Requirement:**
- Data (versions) → stdout
- Diagnostics (errors, warnings, status flags) → stderr

**Rationale:**
Allows CI users to safely redirect stderr to filter npm warnings without losing real errors:
```bash
VERSION=$(npx tagger -q calculate-version ... 2>/dev/null)
```

**Implementation:**
- Audit all output calls in tagger-cli
- Ensure errors always use stderr
- Ensure version data always uses stdout
- Test with redirections: `2>/dev/null`, `1>/dev/null`, etc.

## Checklist

### Code changes
- [ ] Fix quiet mode to output exactly one line to stdout
- [ ] Move status flags to stderr or suppress in quiet mode
- [ ] Add `--strip-snapshot` flag to `calculate-version`
- [ ] Implement snapshot stripping logic
- [ ] Add `--format` flag (optional, consider separately)
- [ ] Implement JSON/version-only formats (optional)
- [ ] Audit all output calls for stdout/stderr separation
- [ ] Fix any stdout/stderr violations

### Testing
- [ ] Test quiet mode outputs one line only
- [ ] Test status flags don't appear on stdout in quiet mode
- [ ] Test `--strip-snapshot` removes suffix correctly
- [ ] Test `--strip-snapshot` with non-snapshot versions (no-op)
- [ ] Test output redirection: `2>/dev/null`, `1>/dev/null`
- [ ] Test quiet + strip-snapshot combination
- [ ] Test format options (if implemented)
- [ ] Run `./gradlew check` and verify all tests pass

### Documentation
- [ ] Add README section: "Output Format"
- [ ] Document when `-SNAPSHOT` is appended
- [ ] Document each status flag with examples
- [ ] Document `--strip-snapshot` flag
- [ ] Document recommended CI extraction pattern
- [ ] Document `--format` options (if implemented)
- [ ] Document JSON schema (if implemented)
- [ ] Update help text for modified commands

### Validation
- [ ] Test CI extraction patterns (bash, PowerShell, etc.)
- [ ] Verify quiet mode is parseable with simple scripts
- [ ] Verify stderr redirection doesn't lose errors
- [ ] Test with npm warnings present (real-world scenario)
- [ ] Move this file to `agents.d/work_completed/`

## Definition of done
- Quiet mode outputs exactly the version to stdout, nothing else
- `-SNAPSHOT` semantics are clearly documented
- All status flags are documented with meanings
- `--strip-snapshot` provides clean CI extraction
- stdout contains only data, stderr contains only diagnostics
- Documentation includes recommended CI patterns
- CI users can parse output reliably without fragile regex
