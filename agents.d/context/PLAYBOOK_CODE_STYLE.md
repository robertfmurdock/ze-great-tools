# Code Style Playbook

## Test-Driven Development
When adding new behavior or task explicitly requires TDD:

### Red-Green-Refactor
- **One test at a time: write, see it fail for the right reason, fix, see it pass, repeat**
- **This rule ALWAYS overrides task document instructions** - if a task says "add comprehensive test coverage", do it one test at a time with red-green-refactor
- **Only add tests that already pass if their absence would confuse readers** - don't write regression tests just for completeness
- Each test expresses one clear, focused objective
- If the test is well-conceived and focused, you don't need separate structure and content tests
- When a scenario produces multiple related outputs, verify them all in one test
- Only write multiple tests when they represent genuinely different scenarios or variations
- Extend existing tests with new assertions when they fit the same scenario
- "Chop down" call chains: break before `?.` and before `.assertIsEqualTo` so assertions visually descend

```kotlin
// Good: assertion stands out at bottom left
data["version"]
    ?.jsonPrimitive
    ?.content
    .assertIsEqualTo("1.2.4")

// Bad: hard to parse
data["version"]?.jsonPrimitive?.content.assertIsEqualTo("1.2.4")
```

### Assertions
- Prefer minassert's `assertIsEqualTo` over kotlin.test assertions
- Most assertions reduce to equality checks; use `assertIsEqualTo` as the default
- Provides clearer diffs when comparing data objects
- Always "chop down" chains leading to assertions (break before `?.` and before the assertion call)
- For nullable checks where the value matters, extract to a variable first rather than using `assertNotNull` inline
- **Test behavior and outcomes, not just structure**: Avoid symbolic tests that only verify presence (assertNotNull), type checks, or field existence without checking the actual effect or value those elements produce

```kotlin
// Good: clear what's being tested, clean diff on failure
json.jsonObject["status"]
    ?.jsonPrimitive
    ?.content
    .assertIsEqualTo("success")

// Bad: harder to read, worse failure messages
assertNotNull(json.jsonObject["status"])
assertEquals("success", json.jsonObject["status"]?.jsonPrimitive?.content)
```

### Backward Compatibility
- When introducing a new API or feature as an alternative to an existing one, **always test both the old and new APIs** to verify backward compatibility
- Write tests showing the old API continues to work as expected alongside the new implementation
- This applies even if existing tests covered the old API — explicitly verify no regression when alternatives are introduced

### Avoid
- Multiple tests before implementing any
- Feature first, tests second
- Batched `./gradlew check` without seeing individual failures
- Redundant tests verifying the same behavior

## Functions
- Target <10 lines; break only when clarity demands it
- Name intent, not implementation

## Comments
- Refactor comment content into code (names, structure, extracted functions)
- Keep only WHY that cannot be expressed in code

## Data Flow
- Prefer immutable structures and functional transforms (`map`, `filter`, `fold`)
- Avoid loops with `break`, `continue`, or mutable accumulators
- Make termination and output unambiguous

## Scope
- Keep edits minimal
- Feature/bugfix: scope is any function touching changed lines
- Refactor: scope is any file touching changed lines
- Preserve behavior unless task changes it
- Follow existing patterns

## API Deprecation
- **New feature must be fully functional and tested before deprecating the old one**
- Deprecation annotations must include:
  1. **Why** it's deprecated (reason)
  2. **What** to use instead (replacement API)
  3. **When** it may be removed (state "may be removed in next major version")
- Use `ReplaceWith` to provide IDE migration hints when possible
- Example: `@Deprecated("Use newApi() instead. May be removed in next major version", ReplaceWith("newApi()"))`
- **Removal timing**: Deprecated code may be removed at any major version boundary, but never in minor or patch releases

## Formatting
- Run `./gradlew formatKotlin` to fix linting issues
- Use the formatter instead of manual edits when possible
