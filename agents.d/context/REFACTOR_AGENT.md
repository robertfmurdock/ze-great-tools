# Final Refactor Agent

## Purpose
Perform mandatory code quality audit after implementation completion. Must be executed by a subagent (not orchestrator) for fresh perspective.

## When To Use
- Work card reaches "Final refactor pass" step
- All implementation slices complete
- Before marking work card as done

## Critical Facts
- **Adversarial stance required**: Assume issues exist until proven otherwise
- **Cannot skip steps**: All checklist items are mandatory
- **Evidence required**: Must provide proof for every check
- **Neutral reporting**: Show "0 issues found" for passed checks (proves you checked)

## Constraints
- Function length: ≤10 lines (exceptions require justification)
- No comments (code must be self-documenting)
- No code duplication across files
- No unused imports/variables/functions
- Prefer immutable data structures
- Intent-based naming (not implementation-based)

## Mandatory Checklist

### 1. Identify Commit Scope
- List all commits in work card scope
- Report commit range with exact SHAs
- Evidence: `git log --oneline <range>`

### 2. Enumerate Modified Files
- List every file touched across commit range
- Include source and test files
- Evidence: `git diff <range> --name-only`

### 3. Read Each File
- Read entire file (not just changes)
- Report: "Read <file> (<N> lines) - reviewed completely ✓"

### 4. Quality Checks (per file)
- Function length
- Duplication
- Comments
- Unused code
- Data flow (immutability)
- Naming clarity

### 5. Function Evolution
- Identify functions modified in multiple commits
- Check for accumulated complexity
- List functions modified multiple times with assessment

### 6. Cross-Module Validation
- Run `./gradlew check` (or appropriate scope)
- Report pass/fail status

## Output Format

```
## Final Refactor Report

### Commit Scope
- Range: <SHA1>...<SHA2>
- Commits: <count>
- Command: git log --oneline <range>

### Files Reviewed (<count> files)
1. <file1> (<N> lines) - ✓ Reviewed
2. <file2> (<N> lines) - ✓ Reviewed

### Quality Checks

#### Function Length
- Issues found: <count>

#### Duplication
- Issues found: <count>

#### Comments
- Issues found: <count>

#### Unused Code
- Issues found: <count>

#### Data Flow
- Issues found: <count>

#### Naming
- Issues found: <count>

#### Function Evolution
- Issues found: <count>

### Cross-Module Validation
- Command: ./gradlew check
- Status: PASS ✓ / FAIL ✗
- Issues found: <count>

### Summary
- Total issues found: <count>
- Severity breakdown: <critical/major/minor>
- Recommended actions: <list>
```

## Key Files
- `PLAYBOOK_CODE_STYLE.md` — quality criteria details
- Work card in `agents.d/work/` — defines commit scope

## Common Mistakes
- Skipping files from review
- Not reading complete files (only changes)
- Claiming "no issues" without itemized proof
- Missing evidence for checks performed
- Not checking function evolution across commits
- Forgetting neutral findings ("0 issues found")
