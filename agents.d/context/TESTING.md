---
load_when: BEFORE implementing any behavior change, writing/modifying tests, before calling submit tool
cost: ~500 tokens
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

### TDD Cycle
1. Load TESTING.md
2. Locate/create test file
3. Write ONE failing test proving feature doesn't exist
4. Run test, verify fails with expected reason (not syntax error)
5. Implement simplest code to pass
6. Refactor: clean names, duplication, structure
7. Run `./gradlew check -q --console=plain`
8. Commit with semver annotation and co-authorship
9. Repeat

**Red-Green-Refactor is mandatory**. One test → fail → pass → repeat.

### Exception Documentation
If testing genuinely not applicable: Document in Implementation Notes what you attempted and why BEFORE making code changes. Legitimate exceptions: pure documentation, build config with no output impact, testing requires disproportionate infrastructure (with justification).

### TestMints Pattern
- Use [TestMints](https://github.com/robertfmurdock/testmints) with anonymous-object setup
- Reference: `tools/digger-plugin/src/test/kotlin/com/zegreatrob/tools/digger/DiggerPluginTest.kt`

### Setup / Exercise / Verify
- **Setup object**: Stable data and constructed objects; inline short values if clearer
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

## Constraints
- Run `./gradlew check -q --console=plain` before calling submit tool
- Every work card checklist item = pushable state
- No failing tests committed
- Java Toolchain 21
- All tests via `./gradlew check -q --console=plain`
- Kotlin with TestMints, not shell scripts
- One test per scenario

## Key Files
- `tools/digger-plugin/src/test/kotlin/com/zegreatrob/tools/digger/DiggerPluginTest.kt` (setup pattern)
- `tools/tagger-plugin/src/test/kotlin/com/zegreatrob/tools/tagger/TaggerPluginTest.kt` (setup callbacks)
- `tools-tests/tagger-plugin-test/src/functionalTest/kotlin/com/zegreatrob/tools/tagger/AdditionalTasksFunctionalTest.kt` (cleanup)
- `CalculateVersionTestSpec` (shared specs)

## Decisions
- For multi-test refactors: batch edits, run targeted compile tasks (`:module:compileKotlinJvm -q --console=plain`), then `./gradlew :tools-tests:check -q --console=plain` per file or at end
- Default flags: `-q --console=plain` minimize token usage (errors only, no ANSI)
- Test both old and new APIs for backward compatibility

## Common Mistakes
- **Implementing code before writing test** (violates TDD)
- **Pattern-matching work as "just metadata/configuration" and skipping test attempt** (attempt first, document exception if genuinely untestable)
- **Not loading TESTING.md before implementation** (mandatory before any behavior change)
- Multiple tests before implementing (write ONE, implement, repeat)
- Batched check without seeing individual failures
- Shell scripts instead of Kotlin tests
- Complex assertion chains without "chopping down"
- Testing structure/presence instead of behavior
- Committing failing tests
- Skipping `./gradlew check -q --console=plain` before submission
