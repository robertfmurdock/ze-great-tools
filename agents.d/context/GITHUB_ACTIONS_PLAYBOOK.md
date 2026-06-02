# GitHub Actions Playbook

## Purpose
Orchestrate CI/CD via GitHub Actions while keeping logic in Gradle tasks.

## When To Use
- Adding or modifying GitHub Actions workflows
- Moving CI logic between YAML and Gradle

## Critical Facts
- Workflow YAML is thin and declarative
- Business logic lives in `./gradlew` tasks, not workflow YAML
- All automation must run locally with equivalent behavior
- Failures must terminate with non-zero exit codes

## Constraints
- Use `run: ./gradlew <task>` instead of multi-line shell scripts
- YAML contains only GitHub-specific concerns:
  - Triggers, permissions, checkout/setup/auth
  - Artifact and PR plumbing
- Business rules and gating logic belong in Gradle tasks
- No non-trivial parsing/branching in workflow shell blocks
- Configuration values defined once (in Gradle, not duplicated in YAML)
- Required preconditions must fail explicitly (no silent skips)
- `GITHUB_STEP_SUMMARY` for diagnostics only, not as failure substitute

## Key Files
- `.github/workflows/*.yml` — workflow orchestration only
- Gradle task definitions — actual automation logic

## Decisions
- GitHub Actions = orchestrator, not implementation surface
- Repository policy independent of GitHub event payload shape where possible

## Common Mistakes
- Duplicating configuration values in YAML and Gradle
- Embedding business logic in workflow shell blocks
- Silent failures when preconditions missing
- Using `GITHUB_STEP_SUMMARY` instead of failing the run

## Validation
- Test workflow syntax with `workflow_dispatch`
- Test automation logic locally via Gradle first
- Verify local command sequence produces equivalent results

## Change Reporting
Report for each workflow change:
- What moved out of YAML and destination
- Which Gradle tasks now own decision logic
- Local command for dry-run verification
