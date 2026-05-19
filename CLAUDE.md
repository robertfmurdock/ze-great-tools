# CLAUDE Instructions

## Read First
- `agents.d/context/PERSONA.md`
- `agents.d/context/TASK_CHECKLIST.md`
- `agents.d/context/PLAYBOOK_CODE_STYLE.md`
- `.junie/guidelines.md` (project-specific testing and verification rules)

## Conditional Reads
Load the relevant playbook based on your task type:
- `agents.d/context/GRADLE_PLAYBOOK.md` — when modifying Gradle build logic or dependencies
- `agents.d/context/GITHUB_ACTIONS_PLAYBOOK.md` — when adding or changing GitHub Actions workflows

## Execution Norms
- Use `./gradlew` for all tasks.
- Express repository automation as Gradle tasks, not ad hoc shell scripts.
- Start with module-scoped validation, then broaden as needed.
- Before completing any task, run `./gradlew check`.
