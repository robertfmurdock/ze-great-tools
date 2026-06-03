# Automated npm Snapshot Version Cleanup

## Goal
Create a GitHub Action workflow that runs daily to automatically remove snapshot versions from npm packages, keeping the npm registry clean while preserving release versions.

## Constraints
- Follow GitHub Actions Playbook: thin YAML, logic in Gradle tasks
- Must have local equivalence for testing and manual execution
- Must NOT delete non-snapshot versions (safety critical)
- Must run `./gradlew check` before completion
- All checklist items must result in pushable, non-failing state
- Requires `NODE_AUTH_TOKEN` secret with organization permissions
- Should handle missing credentials gracefully (fail workflow with clear message)
- Semver: `[none]` (build infrastructure only, no output changes)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Create Gradle task to identify and deprecate snapshot versions
- [x] Create GitHub Actions workflow for scheduled cleanup
- [x] Document manual cleanup procedure
- [ ] Final refactor pass (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
### Context (as of 2026-06-02)
- npm packages: `@continuous-excellence/tagger`, `@continuous-excellence/digger`
- Current publishing: happens on every push when `!isSnapshot()` is false
- Snapshot detection: version format contains snapshot marker (verify format)
- Authentication: `NODE_AUTH_TOKEN` secret (must have org permissions)
- Existing workflows in `.github/workflows/`

### Safety Considerations
- CRITICAL: Must validate version is snapshot before deprecation
- Decision: Use `npm deprecate` instead of `npm unpublish`
  - Unpublish: removes entirely, can break dependents
  - Deprecate: marks with warning, still installable (safer)
- Snapshot format: version string contains "SNAPSHOT" (e.g., `1.2.3-SNAPSHOT`)

### Technical Decisions (2026-06-02)
1. Snapshot version format: version.toString().contains("SNAPSHOT") (from build.gradle.kts:154)
2. Operation: Use `npm deprecate` for safety (not `npm unpublish`)
3. List versions: `npm view @org/pkg versions --json`
4. Safety: Validate version contains "SNAPSHOT" before deprecating

### Implementation Progress (2026-06-02)
**Gradle Task** (build.gradle.kts)
- Created `cleanupNpmSnapshots` task in root build.gradle.kts
- Task uses `npm view` to list versions and `npm deprecate` for snapshots
- Validates NODE_AUTH_TOKEN before execution
- Accepts configurable package list via `-PnpmPackages=...`
- Default packages: @continuous-excellence/tagger, @continuous-excellence/digger
- Uses shell script with jq for JSON parsing (npm CLI tool, acceptable for infrastructure)
- Check passed successfully

**GitHub Workflow** (.github/workflows/npm-snapshot-cleanup.yml)
- Daily schedule: cron at 2 AM UTC
- Manual trigger: workflow_dispatch enabled
- Thin YAML: checkout, setup-node, setup-gradle, call task
- Uses NODE_AUTH_TOKEN from secrets (mapped to NPM_TOKEN)
- Includes jq installation step

**Refactor Pass (2026-06-02)**
- Subagent authorization: explicit user approval granted
- Reviewed commit 1bde27f across 3 files (114 total lines)
- Fixed 2 MAJOR violations: removed header comments and shell script comments per PLAYBOOK_CODE_STYLE.md
- All quality checks passed: function length, duplication, unused code, data flow, naming
- ./gradlew check passed successfully

## Validation
- Commands:
  - `./gradlew help --task cleanupNpmSnapshots` ✓ (task registered, shows description)
  - `./gradlew check` ✓ (passed with new task and workflow)
  - Manual dry-run: `npm view @continuous-excellence/tagger versions --json | jq '.[] | select(contains("SNAPSHOT"))'`
  - Workflow validation: Available via workflow_dispatch for manual testing
- Results:
  - Task successfully registered in publishing group
  - Validates NODE_AUTH_TOKEN before execution
  - Shell script includes safety check (version must contain "SNAPSHOT")
  - Check passed on all included builds
  - Workflow file syntax valid (standard GitHub Actions patterns)
