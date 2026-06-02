# Migrate npm Packages to @continuous-excellence Organization

## Goal
Migrate `git-semver-tagger` and `git-digger` npm packages from unscoped to `@continuous-excellence` scoped packages with smooth transition experience.

## Constraints
- Must maintain backward compatibility during transition period
- Old unscoped packages must redirect/deprecate to new scoped packages
- Published packages must include clear migration guidance
- No breaking changes to CLI functionality (semver: `[none]` for npm scope only, `[major]` if any CLI behavior changes)
- Follow npm best practices for package migration
- Must use `./gradlew` for all automation
- Must run `./gradlew check` before completion
- All checklist items must result in pushable, non-failing state

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Research npm organization setup and scoped package publishing requirements
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Document npm organization setup steps
  - Identify required secrets/tokens for CI/CD
- [ ] Update package names in build configuration
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tagger: `git-semver-tagger` → `@continuous-excellence/git-semver-tagger`
  - Digger: `git-digger` → `@continuous-excellence/git-digger`
  - Update `packageJson` blocks in `tagger-cli/build.gradle.kts` and `digger-cli/build.gradle.kts`
- [ ] Update GitHub Actions workflow for scoped package publishing
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Configure npm registry authentication for scoped packages
  - Update publish steps if needed
- [ ] Add deprecation notices to old unscoped packages
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Create deprecation plan for `git-semver-tagger` and `git-digger`
  - Document migration instructions in package README files
- [ ] Update documentation with new installation commands
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update README files with `npm install @continuous-excellence/git-semver-tagger`
  - Update README files with `npm install @continuous-excellence/git-digger`
  - Add migration guide for existing users
- [ ] Verify local package builds and functionality
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test `jsCliTar` tasks complete successfully
  - Verify binary paths remain correct
  - Confirm `confirmTaggerCanRun` still passes
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
### Current State
- Tagger CLI publishes as `git-semver-tagger` (unscoped)
- Digger CLI publishes as `git-digger` (unscoped)
- npm publishing happens via `jsPublish` tasks in respective build.gradle.kts
- GitHub Actions configures npm registry at `https://registry.npmjs.org`
- Publishing only runs when `!isSnapshot()`

### Migration Strategy
1. Create @continuous-excellence npm organization (if not exists)
2. Update package names to scoped format
3. Publish first version under new scope
4. Deprecate old packages with clear migration message
5. Monitor adoption and support transition period

### Required Secrets
- `NODE_AUTH_TOKEN` (may need update for scoped package publishing)
- npm organization access verification

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:jsCliTar`
  - `./gradlew :command-line-tools:digger-cli:jsCliTar`
  - `./gradlew :command-line-tools:tagger-cli:confirmTaggerCanRun`
  - `./gradlew :command-line-tools:check`
  - `./gradlew check`
- Results: (to be filled in during implementation)
