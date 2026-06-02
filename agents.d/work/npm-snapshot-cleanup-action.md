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
- [ ] Review this work card for compliance with template and update to conform
- [ ] Create Gradle task to identify and unpublish snapshot versions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Task should list snapshot versions for given package(s)
  - Task should unpublish snapshot versions via `npm unpublish` or `npm deprecate`
  - Task should accept package names as parameters
  - Task should fail explicitly if npm credentials missing/invalid
  - Task should log all actions (which versions identified, which removed)
  - Task must validate version contains snapshot marker before deletion
  - Verify local execution with `./gradlew <task-name>`
- [ ] Create GitHub Actions workflow for scheduled cleanup
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Schedule: daily execution via `cron`
  - Thin YAML: checkout, setup-node, auth, call Gradle task
  - Use `workflow_dispatch` for manual triggering/testing
  - Target packages: `@continuous-excellence/tagger`, `@continuous-excellence/digger`
  - Configure npm authentication (NODE_AUTH_TOKEN)
  - Set appropriate permissions (id-token: write for OIDC)
- [ ] Document manual cleanup procedure
  - Agent cycle: test → implement → refactor-light → verify pushable
  - README or workflow comments with local command
  - Explain when/why to run manually
  - Document how to verify cleanup succeeded
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
- CRITICAL: Must validate version is snapshot before unpublish
- Consider: should snapshots be deprecated instead of unpublished?
  - Unpublish: removes entirely, can break dependents
  - Deprecate: marks with warning, still installable
- Decide approach before implementation

### Technical Questions to Resolve
1. What is the snapshot version format? (e.g., `1.2.3-SNAPSHOT`, `1.2.3-dev.123`)
2. Should we unpublish or deprecate snapshots?
3. How to list published versions for a package? (`npm view @org/pkg versions`)
4. Rate limits or throttling needed for bulk operations?

## Validation
- Commands:
  - `./gradlew <cleanup-task-name> --dry-run` (verify detection logic)
  - `./gradlew <cleanup-task-name>` (execute locally, verify no release versions touched)
  - Workflow test: trigger manually via `workflow_dispatch`
  - `./gradlew check`
- Results: (to be filled during implementation)
