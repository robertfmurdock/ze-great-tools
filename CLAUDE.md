# CLAUDE Instructions

## Context System
Read `agents.d/context/index.md` for complete document index with load conditions and token costs.

## Execution Norms
- Use `./gradlew` for all tasks.
- Express repository automation as Gradle tasks, not ad hoc shell scripts.
- Start with module-scoped validation, then broaden as needed.
- Before completing any task, run `./gradlew check`.

## Documentation Standards
- Before committing markdown files, use IDEA's grammar inspection (`mcp__idea__get_file_problems`) to check for issues.
- Apply IDEA's markdown formatter (`mcp__idea__reformat_file`) to ensure consistent formatting.
- This applies to all markdown files: documentation, READMEs, work cards, and planning documents.
