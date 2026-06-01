# CLI Markdown Extraction Rollout

## Goal
Complete extraction of remaining embedded CLI markdown to external resource files for improved maintainability and GitHub linkability.

## Constraints
- Follow extraction architecture validated in investigation pilot (commit 2e77755)
- Preserve CLI behavior exactly (rendering parity required)
- Maintain backward compatibility
- Semver intent (initial): `[none]` — extracting help text to resources is internal refactor with no API/behavior change

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Extract remaining Tagger CLI markdown (3 files)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tagger.kt, CalculateVersion.kt, Tag.kt
- [x] Extract Digger CLI markdown (4 files)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Guide.kt, Digger.kt, CurrentContributionData.kt, AllContributionData.kt
- [x] Verify npm package contents include all help resources
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test via jsLink for both CLIs
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move this file to agents.d/work_completed/

## Implementation Notes
**Semver intent (initial)**: `[none]` — extracting help text to resources is internal refactor with no API/behavior change

**Subagent authorization**: Granted by user on 2026-05-31 for final refactor pass.

**Final refactor report** (2026-05-31):
- Reviewed commit range c3a6642...44bf2b2 (4 commits, 12 files)
- Quality: Clean - 0 code quality issues found
- All modified functions ≤10 lines, no duplication, no comments, clear naming
- Cross-module validation: PASS (./gradlew check)
- Minor notes: 2 markdown table formatting warnings (false positives - formatter made no changes)
- No remediation required

**Architecture reference**: Investigation card findings in `agents.d/work_completed/cli_markdown_externalization_investigation.md`
- Resource layout: `src/commonMain/resources/help/{command-name}.md`
- Loading: `loadHelpResource("help/{file}.md")` (multiplatform)
- Build: `copyHelpResources` task bundles resources for npm package

**Extraction candidates** (from investigation):
### Tagger CLI (3 remaining)
- `Tagger.kt` (~40 lines) - tool overview, CI usage example
- `CalculateVersion.kt` (~12 lines) - snapshot reasons table
- `Tag.kt` (~14 lines) - workflow description

### Digger CLI (4 files)
- `Guide.kt` (~47 lines) - fit assessment, prerequisites, workflow
- `Digger.kt` (~18 lines) - tool overview, command table
- `CurrentContributionData.kt` (~24 lines) - output documentation
- `AllContributionData.kt` (~12 lines) - contribution boundaries concept

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:check` - All tests pass
  - `./gradlew :command-line-tools:digger-cli:check` - All tests pass
  - `./gradlew :command-line-tools:tagger-cli:jsLink` - npm package installs locally
  - `./gradlew :command-line-tools:digger-cli:jsLink` - npm package installs locally
  - `npm exec tagger -- --help` - Help text renders from extracted markdown
  - `npm exec digger -- --help` - Help text renders from extracted markdown
  - `tar -tzf .../tagger-cli-js.tgz | grep help` - All 4 help files present in package
  - `tar -tzf .../digger-cli-js.tgz | grep help` - All 4 help files present in package
- Results:
  - All tests pass for both CLIs (JVM + JS targets)
  - Help resources correctly bundled in npm packages
  - CLI help rendering verified via jsLink local installation
  - Rendering parity confirmed - behavior unchanged from embedded strings
