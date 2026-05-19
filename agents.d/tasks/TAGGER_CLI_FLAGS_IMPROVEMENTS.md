# Task: Improve tagger CLI flags and options

## Goal
Enhance tagger command-line interface design based on CI friction points, making flags more intuitive and supporting common CI workflows.

## Background
Issue #310 identified several CLI design issues causing confusion:
- `--version` flag on `tag` command isn't clearly marked as required
- `--disable-detached` flag name is inverted from its actual behavior
- `tag` command lacks detached HEAD support that `calculate-version` has
- No `--dry-run` option to preview what would happen

These design gaps force users into workarounds and cause failed pipeline runs during setup.

## Hard constraints
- Maintain backwards compatibility where possible
- Deprecated flags should continue working for one release cycle
- Exit codes must remain consistent
- Flag behavior must be consistent across commands

## What to change

### 1. Make --version flag clearer (Issue #310 item #2)

**Current behavior:**
```
$ tagger tag --release-branch=main
Usage: tagger tag [<options>] [<git-repo>]
Error: missing option --version
```

**Option A (simpler):** Mark as required in help text
```
--version=<text>  [required]  The version to tag.
```

**Option B (preferred):** Make it optional and auto-calculate
- When `--version` omitted, internally run `calculate-version` logic
- Apply `--strip-snapshot` semantics automatically
- Allow `--version=X` as override for explicit cases
- Document this one-shot pattern in README

**Implementation:**
- Update `Tag.kt` command definition
- Either: add `[required]` marker to help text (Option A)
- Or: make flag optional and add auto-calculation logic (Option B)
- Update help text and error messages accordingly

### 2. Rename --disable-detached flag (Issue #310 item #4)

**Current behavior:**
Flag name `--disable-detached` is confusing:
- `--disable-detached=true` sounds like "disable the check" but actually enables it
- Error message doesn't show which value to use

**Target behavior:**
```
--allow-detached-head=<true|false>  (default: false)
    Allow tagging when HEAD is detached (common in CI)
```

**Implementation:**
- Add new flag `--allow-detached-head` with clear semantics
- Keep `--disable-detached` as deprecated alias for one release
- Print deprecation warning when old flag is used
- Update error message to show exact flag value:
  ```
  Inappropriate configuration: HEAD is detached.
  Re-run with --allow-detached-head=true to tolerate detached HEAD in CI.
  See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md
  ```
- Update all documentation and help text

### 3. Add detached HEAD support to tag command (Issue #310 item #7)

**Current behavior:**
```
Welcome to Tagger CLI.
skipping tag due to not on release branch alt-main - branch was (detached)
```
(exits 0, making pipeline appear successful even though no tag was created)

**Target behavior:**
- Add `--allow-detached-head` flag to `tag` command (matching `calculate-version`)
- When enabled, tag current HEAD as if on the named release branch
- Make skip exit non-zero by default when unintentional
- Update skip message:
  ```
  tagger tag: not tagging because HEAD is detached (expected to be on alt-main).
  To tag in CI, either:
    - run `git checkout -B alt-main HEAD` before `tagger tag`, or
    - re-run with --allow-detached-head=true
  ```

**Implementation:**
- Add `--allow-detached-head` flag to `Tag.kt`
- Implement detached HEAD tagging when flag is enabled
- Change exit code to non-zero when skipping unintentionally
- Format skip message with actionable guidance
- Consider auto-attach behavior: `git checkout -B <release-branch> HEAD`

### 4. Add --dry-run flag (Issue #310 item #12)

**Current behavior:**
No way to preview what `tag` would do without actually doing it.

**Target behavior:**
```
$ tagger tag --release-branch=main --version=0.0.1 --dry-run
Would create annotated tag '0.0.1' at abc123 on branch 'main'.
Would push to remote 'origin'.
(no changes made)
```

**Implementation:**
- Add `--dry-run` flag to `Tag.kt`
- When enabled, print what would happen without executing
- Include: tag name, commit SHA, branch, remote
- Exit 0 on successful dry-run
- Ensure flag works with `--allow-detached-head` and other options

## Checklist

### Code changes
- [ ] Update `--version` flag (Option A: mark required, or Option B: auto-calculate)
- [ ] Add `--allow-detached-head` flag to both commands
- [ ] Add deprecated `--disable-detached` alias with warning
- [ ] Update error messages to show correct flag values
- [ ] Add detached HEAD support to `tag` command
- [ ] Change skip exit code to non-zero when unintentional
- [ ] Add `--dry-run` flag to `tag` command
- [ ] Implement dry-run preview output

### Testing
- [ ] Test `--version` behavior (required check or auto-calculate)
- [ ] Test `--allow-detached-head=true` on both commands
- [ ] Test `--allow-detached-head=false` maintains current behavior
- [ ] Test deprecated `--disable-detached` shows warning
- [ ] Test `tag` command with detached HEAD (enabled/disabled)
- [ ] Test skip message and exit code
- [ ] Test `--dry-run` shows correct preview without executing
- [ ] Test `--dry-run` with various flag combinations
- [ ] Run `./gradlew check` and verify all tests pass

### Documentation
- [ ] Update help text for all modified flags
- [ ] Document flag deprecation in CHANGELOG
- [ ] Update README with new flag usage
- [ ] Add CI examples showing `--allow-detached-head` usage
- [ ] Document `--dry-run` flag with examples

### Validation
- [ ] Test in actual CI environment (GitHub Actions or ADO)
- [ ] Verify detached HEAD workflow works end-to-end
- [ ] Verify dry-run accurately predicts behavior
- [ ] Verify backwards compatibility with old flags
- [ ] Move this file to `agents.d/tasks_completed/`

## Definition of done
- All flags have intuitive names that match their behavior
- `--allow-detached-head` works consistently across commands
- Users can preview tag operations with `--dry-run`
- Error messages guide users to correct flag values
- Deprecated flags work but warn users
- Documentation and help text reflect all changes
- CI integration is smoother with better flag support
