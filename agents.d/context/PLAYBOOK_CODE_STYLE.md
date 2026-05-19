# Code Style Playbook

## Test-Driven Development
When adding new behavior or task explicitly requires TDD:

### Red-Green-Refactor
- One test at a time: write, see it fail for the right reason, fix, see it pass, repeat
- Each test drives one objective change
- When a scenario produces multiple related outputs, check them all in one test
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
