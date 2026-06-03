---
load_when: BEFORE implementing any behavior change, writing/modifying tests, before calling submit tool
cost: ~1000 tokens
brief: TDD cycle (red-green-refactor), TestMints patterns, setup/exercise/verify, assertion style, test hierarchy
---

# Testing Guidelines

## Purpose
Enforce TDD discipline, TestMints patterns, and consistent test structure.

## When To Use
- **MANDATORY: BEFORE implementing any behavior change** (even if you think tests aren't needed)
- Before calling submit tool
- Writing or modifying tests

## Critical Facts

### TDD Cycle (Per Feature Slice)

**BEFORE any code changes**:
1. Load this file (TESTING.md)
2. Locate or create test file for the module/feature
3. Write ONE failing test that proves desired behavior doesn't exist yet
4. Run test, verify it fails with expected reason (missing feature, not syntax error)

**Then implement**:
5. Simplest implementation to pass the test
6. Refactor: clean names, duplication, structure
7. Verify: `./gradlew check` must pass
8. Commit with semver annotation and co-authorship

**Red-Green-Refactor is mandatory**. Write one test, see it fail correctly, make it pass, repeat.

**If you conclude testing isn't applicable**: Document in Implementation Notes what you attempted and why testing isn't needed BEFORE making any code changes. Legitimate exceptions: pure documentation (README, work cards), build config with no output impact, or testing requires disproportionate infrastructure (with specific justification).

### TestMints Pattern
- Use [TestMints](https://github.com/robertfmurdock/testmints) with anonymous-object setup
- Reference: `tools/digger-plugin/src/test/kotlin/com/zegreatrob/tools/digger/DiggerPluginTest.kt`

### Setup / Exercise / Verify
- **Setup object**: Holds stable data and constructed objects; inline short values if clearer
- **Setup callbacks**: Only for steps that cannot live in setup object (see `tools/tagger-plugin/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt`)
- No SUT calls in setup; pre-req checks use `?: error(...)` in setup, not verify
- **Exercise**: Single call or tight cluster
- **Verify**: All lookups and validation
- **Cleanup**: Use `verifyAnd { ... } teardown { ... }` together (see `tools-tests/tagger-plugin-test/src/functionalTest/kotlin/com/zegreatrob/tools/tagger/AdditionalTasksFunctionalTest.kt`)
- Return result object when exercise produces multiple outputs

### Assertions
- Use minassert's `assertIsEqualTo`
- Prefer data objects for clearer diffs
- "Chop down" chains: break before `?.` and assertions

```kotlin
data["version"]
    ?.jsonPrimitive
    ?.content
    .assertIsEqualTo("1.2.4")
```

- Reserve `error(...)` for setup pre-reqs only
- Inline expectation data if used once; variable if reused
- Extract nullable values to variables instead of inline `assertNotNull`

### CLI Output Testing
Use regex for help text that may wrap: `result.output.contains(Regex("\\(default:\\s*text\\)"))` handles both inline and wrapped output.

### Test Behavior, Not Structure
- Verify outcomes and effects, not presence
- Avoid symbolic tests (`assertNotNull`, type checks) without verifying actual behavior

### Test Spec Hierarchy
- **Spec-level tests**: Feature behavior across implementations (CLI, config, DSL)
- **Implementation tests**: Format, output details, help text, mechanics unique to that layer
- Go straight to spec level for simple features; start implementation-level for complex, then refactor to spec
- Example: `CalculateVersionTestSpec` defines shared behavior; implementations ensure parity

### Form-Factor Abstraction
- Keep behavior assertion in spec (WHAT to verify)
- Push verification strategy to adapters (HOW to verify)
- Create micro-API methods each adapter implements
- Example: `assertHasDeprecationWarning()` in spec; adapters handle kebab-case vs camelCase

### Test Integration
- All tests via `./gradlew check`
- Kotlin with TestMints, not shell scripts
- One test per scenario
- Test both old and new APIs for backward compatibility

## Constraints
- Run `./gradlew check` before calling submit tool
- Every work card checklist item = pushable state
- No failing tests committed
- Java Toolchain 21

## Key Files
- `tools/digger-plugin/src/test/kotlin/com/zegreatrob/tools/digger/DiggerPluginTest.kt` (setup pattern)
- `tools/tagger-plugin/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt` (setup callbacks)
- `tools-tests/tagger-plugin-test/src/functionalTest/kotlin/com/zegreatrob/tools/tagger/AdditionalTasksFunctionalTest.kt` (cleanup)
- `CalculateVersionTestSpec` (shared specs)

## Decisions
- For multi-test refactors: batch edits, run targeted compile tasks (`:module:compileKotlinJvm`), then `./gradlew :tools-tests:check` per file or at end
- Use `--quiet` or `--console=plain` for less verbose output

## Common Mistakes
- **Implementing code before writing test** (violates TDD — if behavior changes, test must come first)
- **Pattern-matching work as "just metadata" or "just configuration" and skipping test attempt** (attempt first, document exception if genuinely untestable)
- **Not loading TESTING.md before implementation** (mandatory before any behavior change)
- Multiple tests before implementing (write ONE, implement, repeat)
- Batched check without seeing individual failures
- Shell scripts instead of Kotlin tests
- Complex assertion chains without "chopping down"
- Testing structure/presence instead of behavior
- Committing failing tests
- Skipping `./gradlew check` before submission
