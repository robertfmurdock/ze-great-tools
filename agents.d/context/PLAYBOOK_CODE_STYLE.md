# Code Style Playbook

## Purpose

Enforce TDD discipline, test consistency, and maintainable code patterns across the codebase.

## When To Use

- Adding new behavior requiring tests
- Modifying existing code
- Deprecating APIs
- Writing assertions

## Critical Facts

### Test-Driven Development
- **Red-Green-Refactor is mandatory**: Write one test, see it fail, make it pass, repeat
- This overrides any task document saying "add comprehensive coverage"
- All tests run via `./gradlew check` (Gradle integration required)
- Use Kotlin with TestMints structure (setup/exercise/verify), not shell scripts
- One test per distinct scenario; extend existing tests when fitting same scenario
- Only write passing tests if their absence confuses readers

### Assertions
- Default to minassert's `assertIsEqualTo` over kotlin.test assertions
- "Chop down" chains: break before `?.` and before assertions
- Test behavior/outcomes, not just structure (avoid symbolic `assertNotNull`/type checks)
- Extract nullable values to variables rather than inline `assertNotNull`

```kotlin
// Correct
data["version"]
    ?.jsonPrimitive
    ?.content
    .assertIsEqualTo("1.2.4")
```

### Functions
- Target <10 lines; break only when clarity demands
- Name intent, not implementation

### Data Flow
- Use immutable structures and functional transforms (`map`, `filter`, `fold`)
- Avoid loops with `break`, `continue`, or mutable accumulators

## Constraints

### Scope
- Feature/bugfix: scope is any function touching changed lines
- Refactor: scope is any file touching changed lines
- Keep edits minimal; preserve behavior unless task changes it

### Comments
- Refactor into code (names, structure, extracted functions)
- Keep only WHY that cannot be expressed in code

### Backward Compatibility
- When introducing alternatives, test both old and new APIs
- Verify no regression explicitly

### API Deprecation
- New feature must be fully functional and tested before deprecating
- Deprecation annotation must include:
  1. Why (reason)
  2. What to use (replacement API)
  3. When removal (state "may be removed in next major version")
- Use `ReplaceWith` for IDE migration hints
- Remove only at major version boundaries

Example:
```kotlin
@Deprecated("Use newApi() instead. May be removed in next major version", ReplaceWith("newApi()"))
```

### Formatting
- Run `./gradlew formatKotlin` to fix linting issues
- Use formatter over manual edits

## Common Mistakes

- Writing multiple tests before implementing any
- Feature first, tests second
- Batched `./gradlew check` without seeing individual failures
- Redundant tests verifying same behavior
- Using shell scripts instead of Kotlin tests
- Not following existing test patterns in codebase
- Complex assertion chains without "chopping down"
- Testing structure/presence instead of actual behavior
