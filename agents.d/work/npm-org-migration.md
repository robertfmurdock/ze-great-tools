# Migrate npm Packages to @continuous-excellence Organization

## Goal
Migrate `git-semver-tagger` and `git-digger` npm packages to `@continuous-excellence` organization with simple names (`tagger` and `digger`) for smooth transition experience.

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
- [x] Research npm organization setup and scoped package publishing requirements
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Document npm organization setup steps
  - Identify required secrets/tokens for CI/CD
- [x] Update package names in build configuration
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tagger: `git-semver-tagger` → `@continuous-excellence/tagger`
  - Digger: `git-digger` → `@continuous-excellence/digger`
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
  - Update README files with `npm install @continuous-excellence/tagger`
  - Update README files with `npm install @continuous-excellence/digger`
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

## Research Findings (2026-06-02)

### npm Organization & Scoped Publishing
**Organization Status:**
- `@continuous-excellence` org already exists and owned by user ✓
- Ready for scoped package publishing

**Scoped Package Publishing:**
- Requires `--access public` flag (scoped packages default to private)
- Command: `npm publish --access public`
- package.json: `name` field must use scope (e.g., `"@continuous-excellence/tagger"`)
- Authentication: requires 2FA OR granular access token with bypass 2FA enabled

**CI/CD Token Requirements:**
- Current: `NODE_AUTH_TOKEN` secret (already in GitHub Actions via env var)
- For scoped packages: Same token works IF it has organization publish permissions
- Verify token has correct scope access before first publish
- GitHub Actions already configures npm registry at setup-node step

**Package Deprecation:**
- Command: `npm deprecate <package-name> "<message>"`
- Entire package removed from search results
- Red warning displays on package page
- Messages should direct to new scoped package
- Better than unpublishing (which breaks all dependents)

### Current Build Configuration Analysis
**Tagger CLI (`tagger-cli/build.gradle.kts`):**
- package.json name: `"git-semver-tagger"` (line 42)
- bin: `tagger` → `kotlin/bin/tagger`
- jsPublish task: `npm publish` (line 126) — needs `--access public`
- enabled: `!isSnapshot()`

**Digger CLI (`digger-cli/build.gradle.kts`):**
- package.json name: `"git-digger"` (line 35)
- bin: `digger` → `kotlin/bin/digger`
- jsPublish task: `npm publish` (line 113) — needs `--access public`
- enabled: `!isSnapshot()`

**GitHub Actions (`.github/workflows/main.yml`):**
- Node setup: line 40-43, registry `https://registry.npmjs.org`
- Validates NPM token: line 44-47
- No explicit publish step in workflow — relies on Gradle `release` task (line 79)

### Required Changes Summary
1. Organization: Create `@continuous-excellence` on npmjs.com
2. Package names: Add `@continuous-excellence/` prefix in both build.gradle.kts files
3. Publish command: Add `--access public` to jsPublish tasks
4. Token verification: Confirm `NODE_AUTH_TOKEN` has org publish permissions
5. Deprecation: Post-publish, deprecate old packages with migration message

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:jsCliTar`
  - `./gradlew :command-line-tools:digger-cli:jsCliTar`
  - `./gradlew :command-line-tools:tagger-cli:confirmTaggerCanRun`
  - `./gradlew :command-line-tools:check`
  - `./gradlew check`
- Results:
  - ✓ tagger-cli:jsCliTar (2026-06-02): SUCCESS, package.json now uses `@continuous-excellence/tagger`
  - ✓ digger-cli:jsCliTar (2026-06-02): SUCCESS, package.json now uses `@continuous-excellence/digger`
  - ✓ tagger-cli:confirmTaggerCanRun (2026-06-02): SUCCESS, tagger CLI executes correctly
  - ✓ command-line-tools:check (2026-06-02): SUCCESS, all tests pass
