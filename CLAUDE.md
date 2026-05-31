# CLAUDE Instructions

## Read First
- `agents.d/context/PERSONA.md` — working values, collaboration preferences, and decision heuristics
- `agents.d/context/WORK_CHECKLIST.md` — work card template and implementation cycle (required when creating, assessing, or working on work cards)
- `.junie/guidelines.md` — verification requirements, TestMints patterns, and test structure rules

## Conditional Reads
Load the relevant playbook based on your task type:
- `agents.d/context/PLAYBOOK_CODE_STYLE.md` — when modifying source code
- `agents.d/context/GRADLE_PLAYBOOK.md` — when modifying Gradle build logic or dependencies
- `agents.d/context/GITHUB_ACTIONS_PLAYBOOK.md` — when adding or changing GitHub Actions workflows
- `agents.d/context/REFACTOR_AGENT.md` — when performing final refactor pass on completed work

## Execution Norms
- Use `./gradlew` for all tasks.
- Express repository automation as Gradle tasks, not ad hoc shell scripts.
- Start with module-scoped validation, then broaden as needed.
- Before completing any task, run `./gradlew check`.

## Documentation Standards
- Before committing markdown files, use IDEA's grammar inspection (`mcp__idea__get_file_problems`) to check for issues.
- Apply IDEA's markdown formatter (`mcp__idea__reformat_file`) to ensure consistent formatting.
- This applies to all markdown files: documentation, READMEs, work cards, and planning documents.
