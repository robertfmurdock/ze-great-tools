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

## Execution Norms
- Use `./gradlew` for all tasks.
- Express repository automation as Gradle tasks, not ad hoc shell scripts.
- Start with module-scoped validation, then broaden as needed.
- Before completing any task, run `./gradlew check`.
