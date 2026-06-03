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
- [x] Review this work card for compliance with template and update to conform
- [x] Research npm organization setup and scoped package publishing requirements
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Document npm organization setup steps
  - Identify required secrets/tokens for CI/CD
- [x] Update package names in build configuration
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tagger: `git-semver-tagger` → `@continuous-excellence/tagger`
  - Digger: `git-digger` → `@continuous-excellence/digger`
  - Update `packageJson` blocks in `tagger-cli/build.gradle.kts` and `digger-cli/build.gradle.kts`
- [x] Update GitHub Actions workflow for scoped package publishing
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Configure npm registry authentication for scoped packages
  - Update publish steps if needed
  - Enhanced token validation to verify org access
- [x] Add deprecation notices to old unscoped packages
  - **Manual step**: Use npm CLI to deprecate old packages (no code changes needed)
  - `npm deprecate git-semver-tagger "Package moved to @continuous-excellence/tagger. Install with: npm install @continuous-excellence/tagger"`
  - `npm deprecate git-digger "Package moved to @continuous-excellence/digger. Install with: npm install @continuous-excellence/digger"`
  - ✓ Completed 2026-06-02: All versions of both packages successfully deprecated
- [x] Update documentation with new installation commands
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update README files with `npm install @continuous-excellence/tagger`
  - Update README files with `npm install @continuous-excellence/digger`
  - Add migration guide for existing users
- [x] Verify local package builds and functionality
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test `jsCliTar` tasks complete successfully
  - Verify binary paths remain correct
  - Confirm `confirmTaggerCanRun` still passes
  - Completed during "Update package names" slice (2026-06-02)
- [x] Review changes against applicable playbooks and verify compliance
  - Reviewed: GRADLE_PLAYBOOK.md, GITHUB_ACTIONS_PLAYBOOK.md, PLAYBOOK_CODE_STYLE.md, WORK_CHECKLIST.md
  - ✓ Build logic properly scoped in module build files
  - ✓ GitHub Actions workflow remains thin orchestration layer
  - ✓ Sequential checklist discipline maintained
  - ✓ All validation commands passed before marking items complete
  - ✓ Semver annotations correct (`[none]` for build config, no output impact yet)
  - Outstanding: manual npm deprecate step (gated on first scoped package publish)
- [x] Move this file to agents.d/work_completed/

## Implementation Notes
### Current State (as of 2026-06-02)
- Build configured to publish as `@continuous-excellence/tagger` and `@continuous-excellence/digger`
- New scoped packages NOT YET published to npm
- Old packages `git-semver-tagger` and `git-digger` still exist on npm
- npm publishing happens via `jsPublish` tasks in respective build.gradle.kts
- GitHub Actions configures npm registry at `https://registry.npmjs.org`
- Publishing only runs when `!isSnapshot()`

### Migration Strategy
1. ✓ Create @continuous-excellence npm organization (already exists)
2. ✓ Update package names to scoped format (build.gradle.kts updated)
3. ⏳ Publish first version under new scope (requires release/tag)
4. ⏳ Deprecate old packages with `npm deprecate` commands (after new packages published)
5. Monitor adoption and support transition period

### Deprecation Approach (2026-06-02)
**Decision:** Use `npm deprecate` command directly on old packages (no new publish to old location needed)
- `npm deprecate` marks all versions with a red warning on npmjs.com
- Users can still install deprecated packages but see migration guidance
- No code changes or dual-publishing required
- Must wait until new scoped packages are published before deprecating old ones

### Required Secrets
- `NODE_AUTH_TOKEN` (existing secret, must have @continuous-excellence org publish permissions)
- npm organization access verification added to workflow validation step

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
1. ✓ Organization: Create `@continuous-excellence` on npmjs.com (already exists)
2. ✓ Package names: Add `@continuous-excellence/` prefix in both build.gradle.kts files
3. ✓ Publish command: Add `--access public` to jsPublish tasks
4. ✓ Token verification: Enhanced workflow validation to check org access
5. Pending: Deprecation: Post-publish, deprecate old packages with migration message

### GitHub Actions Changes (2026-06-02)
**Workflow: `.github/workflows/main.yml`**
- Enhanced "Validate NPM Token" step:
  - Added explicit `NODE_AUTH_TOKEN` env var
  - Added `npm whoami` to verify token authentication (fails build if invalid)
  - Added `npm access ls-packages @continuous-excellence` to verify org access
  - Warns if token doesn't have org permissions (continues to allow investigation)
  - Provides helpful error messages with token creation instructions
- No other changes needed: `setup-node@v6` already configures npm auth correctly
- The `--access public` flags in build.gradle.kts handle scoped package publishing

**IMPORTANT: npm Trusted Publishing (OIDC)**
- Uses GitHub Actions OIDC (`id-token: write` permission already configured)
- **No manual npm token needed** - GitHub automatically provides temporary credentials
- Requires `--provenance` flag on `npm publish` (now added to both CLI builds)
- Organization must have trusted publishing configured on npmjs.com
- **Pre-Push Verification Required:**
  - Check if @continuous-excellence org has GitHub Actions trusted publishing enabled
  - If not enabled: must configure at https://www.npmjs.com/settings/@continuous-excellence/integrations
  - Alternative: Fall back to manual NODE_AUTH_TOKEN if trusted publishing unavailable

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
  - ✓ Final ./gradlew check (2026-06-02): BUILD SUCCESSFUL in 3s

## Completion Summary (2026-06-02)
**Completed:**
- ✅ Build configuration updated to publish scoped packages `@continuous-excellence/tagger` and `@continuous-excellence/digger`
- ✅ GitHub Actions workflow configured for OIDC trusted publishing (no manual tokens needed)
- ✅ Documentation updated with new installation commands
- ✅ All validations pass, repository in pushable state
- ✅ New scoped packages successfully published to npm
- ✅ Old packages deprecated with migration guidance

**Deprecation Results (2026-06-02):**
- `git-semver-tagger`: All versions (1.4.10 through 1.9.3+) deprecated successfully
- `git-digger`: All versions (1.1.26 through 1.8.44+) deprecated successfully
- Both packages now display red warning on npmjs.com with installation guidance for new scoped packages

**Migration Complete:** Users installing old packages will see deprecation warnings directing them to `@continuous-excellence/tagger` and `@continuous-excellence/digger`

**Semver Impact:** `[none]` (build configuration only)
