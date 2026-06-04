# Gradle Standards Conformance

## Goal
Improve conformance with Gradle best practices across build files: add missing task groups/descriptions, eliminate eager APIs, consolidate duplicated publishing/signing config into convention plugins.

## Constraints
- Follow GRADLE_PLAYBOOK.md standards
- Maintain existing behavior (no functional changes)
- Keep build logic and consumer updates together
- Configuration-cache compatible
- Semver intent: `[none]` — internal build improvements with no API changes

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] Audit root build.gradle.kts for missing task group/description on registered tasks
  - Agent cycle: identify violations → test → fix → refactor-light → verify pushable
  - Tasks: versionCatalogUpdate, formatKotlin, kotlinUpgradeYarnLock, collectResults, resetYarnLock
- [x] Audit module build.gradle.kts files for missing task group/description
  - Agent cycle: identify violations → test → fix → refactor-light → verify pushable
  - Focus: command-line-tools/tagger-cli, command-line-tools/digger-cli
  - Fixed: jsLink, publish, copyGuideResources in both CLIs
- [ ] Extract duplicated publishing config to convention plugin
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Target: tools/digger-plugin, tools/tagger-plugin, tools/*-plugin
  - Pattern: afterEvaluate + pom configuration appears in 3+ files
- [ ] Extract duplicated signing config to convention plugin
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Target: tools/digger-plugin, tools/tagger-plugin
  - Pattern: signingKey + Base64 decoding + useInMemoryPgpKeys
- [ ] Verify all user-facing tasks appear in `./gradlew tasks` output
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test: tasks with descriptions but no group should have group assigned
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Current State
- Start commit: 73d8dcc88fb3e7f7c96d3eba8792afc3c231aa29
- Date: 2026-06-04
- Status: In progress - extracting duplicated publishing config
- Blockers: None

## Implementation Notes
**Semver intent**: `[none]` — internal build improvements with no API changes

**2026-06-04 - Testing approach for task metadata**:
Task group/description changes have user-facing impact (task visibility in `./gradlew tasks` output). Testing approach: manual verification via `./gradlew tasks` command before and after changes. Automated testing would require GradleRunner test infrastructure setup, which is disproportionate for metadata validation. Verification commands documented in Validation section.

### Known violations identified
**Root build.gradle.kts**:
- Lines 43-45: `versionCatalogUpdate` — has no group or description
- Lines 46-48: `formatKotlin` — has no group or description
- Lines 59-68: `kotlinUpgradeYarnLock` — has no group or description
- Lines 73-78: `collectResults` — has description but no group

**command-line-tools/tagger-cli/build.gradle.kts**:
- Lines 92-96: `copyReadme` — no group or description (internal task, acceptable)
- Lines 97-102: `copyGuideResources` — has description but no group
- Lines 106-110: `copyHelpResources` — no group or description (internal)
- Lines 111-125: `jsCliTar` — no group or description (internal)
- Lines 126-130: `jsLink` — no group or description (should have - used by humans)
- Lines 131-135: `confirmTaggerCanRun` — no group (internal validation)
- Lines 136-145: `jsPublish` — no group (internal, wrapped by publish)
- Lines 149-152: `publish` — no group or description (should have)
- Lines 153-160: `copyTemplates` — no group (internal)

**Duplication targets**:
- Publishing config: tools/digger-plugin/build.gradle.kts:67-97 duplicates publish.gradle.kts pattern
- Signing config: tools/digger-plugin/build.gradle.kts:53-65 duplicates similar pattern

## Validation
- Commands:
  - `./gradlew tasks` — all user-facing tasks appear with groups
  - `./gradlew :check -q --console=plain` — full validation passes
  - `./gradlew help --task <task-name>` — descriptions present for user tasks
- Results:
  - ✅ Root build.gradle.kts: All tasks (versionCatalogUpdate, formatKotlin, resetYarnLock, kotlinUpgradeYarnLock, collectResults) now appear in `./gradlew tasks` with proper groups and descriptions (commit: d10620a6)
  - ✅ CLI modules: jsLink, publish, copyGuideResources tasks now visible in tagger-cli and digger-cli (commit: acc2ed48)
  - ✅ ./gradlew check -q --console=plain passes
