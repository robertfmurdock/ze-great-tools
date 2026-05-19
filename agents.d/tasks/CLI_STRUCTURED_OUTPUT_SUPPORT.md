# Task: Add structured output support to all CLIs

## Goal
Implement structured JSON output for all CLI tools in this repository, making them easier to integrate into CI/CD pipelines and automation scripts. Follow TDD approach: write failing tests first, verify failure reasons, then implement.

## Background
CLIs benefit from machine-readable structured output. Currently, tools output text that requires brittle parsing with `grep`, `sed`, and regex. Adding `--format=json` option provides:
- Stable parsing contracts for automation
- Clear separation of data vs diagnostics
- Better error handling in scripts
- Standard patterns across all repo CLIs

This complements the tagger output improvements in issue #310 but extends to all CLIs.

## Hard constraints
- Maintain backwards compatibility: text format remains default
- Follow TDD: test → verify failure → implement → verify success
- Consistent JSON structure across all CLIs
- Errors in JSON mode must return valid JSON (not stack traces)
- All CLIs must support the same format flag conventions

## CLIs in scope
- `tagger-cli` - semantic version tagging
- `digger-cli` - git contribution data extraction

## What to change

### 1. Design: Common output structure

**Success response:**
```json
{
  "status": "success",
  "data": {
    ...command-specific data...
  }
}
```

**Error response:**
```json
{
  "status": "error",
  "error": "Human-readable error message",
  "code": "ERROR_CODE_CONSTANT",
  "details": {
    ...optional additional context...
  }
}
```

**Optional metadata (consider per-command):**
```json
{
  "status": "success",
  "data": {...},
  "meta": {
    "version": "1.9.20",
    "timestamp": "2026-05-19T18:30:00Z"
  }
}
```

### 2. Flag design

**Add to all commands:**
- `--format=<text|json>` (default: `text`)
- Alternative names considered: `--output-format`, `--output`
- Short form: `-f json` (optional)

**Behavior:**
- `--format=text`: current behavior (default, backwards compatible)
- `--format=json`: structured JSON output
- Invalid format values: error with helpful message listing valid options
- Works orthogonally with other flags (`-q`, `--verbose`, etc.)

**Interaction with quiet mode:**
- In JSON mode, `-q` could suppress metadata/verbose fields
- Or: `-q` affects text mode only, JSON is always "quiet"
- Decision: JSON mode is inherently structured; `-q` is text-mode concept

### 3. Stdout/stderr separation

**Requirements:**
- JSON mode: only valid JSON goes to stdout
- Errors in JSON mode: JSON error object to stdout (or consider stderr?)
- Diagnostics, warnings, progress: always stderr
- Text mode: preserve current behavior

**Rationale:**
Allows scripts to safely parse stdout without stderr contamination:
```bash
result=$(tagger calculate-version --format=json 2>/dev/null)
```

### 4. Implementation plan: tagger-cli

#### Commands to update:
- `calculate-version`
- `tag`
- Others as needed

#### calculate-version JSON output:
```json
{
  "status": "success",
  "data": {
    "version": "0.0.1",
    "snapshot": true,
    "flags": [
      {
        "name": "DIRTY",
        "description": "Working directory has uncommitted changes"
      },
      {
        "name": "AHEAD",
        "description": "Local branch is ahead of remote"
      }
    ],
    "commits_since_tag": 3,
    "last_tag": "0.0.0"
  }
}
```

Or simpler:
```json
{
  "status": "success",
  "data": {
    "version": "0.0.1",
    "snapshot": true,
    "flags": ["DIRTY", "AHEAD"]
  }
}
```

#### tag JSON output:
```json
{
  "status": "success",
  "data": {
    "tag_name": "0.0.1",
    "commit": "abc123def456",
    "branch": "main",
    "pushed": true,
    "remote": "origin"
  }
}
```

#### Error example:
```json
{
  "status": "error",
  "error": "HEAD is detached (not pointing at any branch)",
  "code": "DETACHED_HEAD",
  "details": {
    "release_branch": "main",
    "flag_to_allow": "--allow-detached-head=true"
  }
}
```

### 5. Implementation plan: digger-cli

#### Commands to update:
- `current-contribution-data`
- `all-contribution-data`

#### Notes:
- These commands already output JSON (to files)
- Need to wrap in status envelope for consistency
- Or: keep raw data output, add `--format` for envelope option

#### current-contribution-data JSON output:

**Current (raw):**
```json
{
  "storyId": "123",
  "contributors": [...],
  "commits": [...]
}
```

**Option A: Wrapped (consistent with tagger):**
```json
{
  "status": "success",
  "data": {
    "storyId": "123",
    "contributors": [...],
    "commits": [...]
  }
}
```

**Option B: Keep raw, document as JSON-by-default**
- digger already outputs JSON
- Adding envelope might break existing users
- Consider: `--format=json` (raw, default), `--format=envelope` (wrapped)

**Decision needed:** Discuss tradeoff between consistency and breaking changes.

### 6. TDD workflow (REQUIRED)

For each CLI and each command, follow strictly:

#### Step 1: Write failing test
```kotlin
@Test
fun `calculate-version with format json outputs valid json`() {
    val result = runCommand("calculate-version", "--format=json", "--release-branch=main")
    
    val json = Json.parseToJsonElement(result.stdout)
    assertEquals("success", json.jsonObject["status"]?.jsonPrimitive?.content)
    assertNotNull(json.jsonObject["data"])
}
```

#### Step 2: Run test, verify it fails
- Run `./gradlew :command-line-tools:tagger-cli:test`
- Expected failure: "Unknown option --format" or "Invalid JSON output"
- Document the failure reason: proves we're testing the right thing

#### Step 3: Implement feature
- Add `--format` flag to command class
- Implement JSON serialization
- Handle errors in JSON mode

#### Step 4: Run test, verify it passes
- Run `./gradlew :command-line-tools:tagger-cli:test`
- Test should pass
- No manual testing until test passes

#### Step 5: Write additional tests
- Test error cases return valid JSON
- Test invalid format values
- Test interaction with other flags
- Test stdout/stderr separation

### 7. Test coverage requirements

For each command:
- [ ] Test `--format=json` outputs valid, parseable JSON
- [ ] Test JSON success structure matches schema
- [ ] Test JSON error structure matches schema
- [ ] Test `--format=text` preserves current behavior (regression)
- [ ] Test `--format=invalid` returns helpful error
- [ ] Test JSON output goes to stdout
- [ ] Test errors in JSON mode return valid JSON
- [ ] Test flag interaction: `--format=json -q` (if applicable)
- [ ] Test real-world scenarios (CI extraction patterns)

## Checklist

### Phase 1: Design and planning
- [x] Review and finalize JSON schema design
- [x] Decide on digger-cli wrapping approach (envelope vs raw)
- [x] Document error code constants to use
- [x] Review interaction with existing flags

### Phase 2: tagger-cli implementation (TDD)
- [x] Write failing test: `calculate-version --format=json`
- [x] Verify test fails correctly (error: "no such option --format")
- [x] Implement `--format` flag for `calculate-version`
- [x] Add kotlinx.serialization plugin to tagger-cli build.gradle.kts
- [x] Create JSON response models (VersionData, ErrorResponse)
- [x] Implement enum OutputFormat with TEXT and JSON options
- [x] Implement JSON serialization for success case
- [x] Change snapshot field to Boolean (not String)
- [x] Include snapshotReasons in JSON output
- [x] Verify test passes
- [x] Write failing test: error cases return JSON
- [x] Verify test fails correctly
- [x] Implement JSON error handling
- [x] Verify test passes
- [x] Write test: snapshot reasons included when present
- [x] Write test: invalid format value shows available options
- [x] Verify all calculate-version tests pass
- [ ] Write failing test: `tag --format=json`
- [ ] Verify test fails correctly
- [ ] Implement `--format` flag for `tag`
- [ ] Implement JSON serialization for `tag`
- [ ] Verify test passes
- [ ] Add comprehensive test coverage per checklist above
- [ ] Run `./gradlew :command-line-tools:tagger-cli:check`

### Phase 3: digger-cli implementation (TDD)
- [ ] Write failing test: `current-contribution-data --format=json`
- [ ] Verify test fails correctly
- [ ] Implement `--format` flag (or envelope wrapping)
- [ ] Verify test passes
- [ ] Write failing test: `all-contribution-data --format=json`
- [ ] Verify test fails correctly
- [ ] Implement `--format` flag
- [ ] Verify test passes
- [ ] Add comprehensive test coverage
- [ ] Run `./gradlew :command-line-tools:digger-cli:check`

### Phase 4: Integration and validation
- [ ] Test all commands with `--format=json` manually
- [ ] Test in actual CI environment (bash, PowerShell)
- [ ] Verify JSON is parseable by common tools (`jq`, `node -pe`, Python)
- [ ] Test error cases produce valid JSON
- [ ] Run `./gradlew check` for full repo

### Phase 5: Documentation
- [ ] Update help text for all commands to show `--format` option
- [ ] Update README with JSON output examples
- [ ] Document JSON schema for each command
- [ ] Add "Structured Output" section to README
- [ ] Document error codes and meanings
- [ ] Add CI integration examples using JSON
- [ ] Update migration guide if needed (for digger changes)

### Final validation
- [ ] All tests pass: `./gradlew check`
- [ ] Manual testing in CI environment successful
- [ ] Documentation complete and accurate
- [ ] Backwards compatibility verified (text mode unchanged)
- [ ] Move this file to `agents.d/tasks_completed/`

## Definition of done
- All CLIs support `--format=json` flag
- JSON output follows consistent schema across CLIs
- All tests written TDD-style (test first, verify failure, implement, verify success)
- Error cases in JSON mode return valid JSON
- Text mode (default) maintains backwards compatibility
- Comprehensive test coverage for all commands and error cases
- Documentation includes JSON schemas and examples
- CI integration examples demonstrate JSON parsing
- Full test suite passes: `./gradlew check`

## Notes
- Consider future formats: `--format=yaml`, `--format=xml` (low priority)
- Consider structured output for non-JSON-native commands first (tagger has more text output)
- Keep JSON schemas stable once published (semantic versioning for schemas)
- Error codes should be constants (not strings) for stability
