# Improve Package Metadata for Discoverability

## Goal
Enhance NPM package metadata with specific, searchable keywords and comprehensive descriptions to improve discoverability for teams searching for semantic versioning and git analytics tools.

## Constraints
- Must not break existing package structure or dependencies
- Package names remain unchanged (`@continuous-excellence/tagger`, `@continuous-excellence/digger`)
- Semver intent: `[none]` - metadata-only changes, no behavioral impact
- Changes apply to build output, not source templates (metadata injected during build)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Identify where NPM package.json metadata is configured in the build (likely in Gradle Kotlin/JS plugin configuration)
  - Agent cycle: investigate only (no code changes)
  - Update plan if build configuration differs from expectation
- [ ] Add comprehensive `description` field for tagger package highlighting deterministic versioning and platform-neutrality
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update tagger keywords to include: `semantic-versioning`, `semver`, `git-tags`, `release-automation`, `version-management`, `gradle-plugin`, `ci-cd`, `devops`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add comprehensive `description` field for digger package highlighting privacy-controlled git analytics
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Update digger keywords to include: `git-analytics`, `contribution-tracking`, `team-metrics`, `git-statistics`, `commit-analysis`, `developer-metrics`, `code-statistics`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Verify build generates correct package.json with new metadata
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: (to be filled on start)
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Ready to start
- **Date**: 2026-06-06

## Implementation Notes
_(newest first)_

### 2026-06-06: Work card created
Semver intent: `[none]` - metadata changes only, no code or behavior changes.

Current package.json location: `command-line-tools/build/js/packages/command-line-tools-tagger-cli/package.json` and `command-line-tools/build/js/packages/command-line-tools-digger-cli/package.json`

These are build outputs. Need to find source configuration (likely in Gradle build files for the CLI modules).

Current tagger keywords: `git, contribution, pair, agile, coaching, statistics` (generic, not search-optimized)
Current digger keywords: likely similar

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] `./gradlew :command-line-tools:tagger-cli:build` - builds successfully
- [ ] `./gradlew :command-line-tools:digger-cli:build` - builds successfully
- [ ] Verify `command-line-tools/build/js/packages/command-line-tools-tagger-cli/package.json` contains new keywords and description
- [ ] Verify `command-line-tools/build/js/packages/command-line-tools-digger-cli/package.json` contains new keywords and description
