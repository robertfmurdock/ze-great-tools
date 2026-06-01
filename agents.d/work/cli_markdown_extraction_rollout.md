# CLI Markdown Extraction Rollout

## Goal
Complete extraction of remaining embedded CLI markdown to external resource files for improved maintainability and GitHub linkability.

## Constraints
- Follow extraction architecture validated in investigation pilot (commit 2e77755)
- Preserve CLI behavior exactly (rendering parity required)
- Maintain backward compatibility
- Semver intent (initial): `[none]` — extracting help text to resources is internal refactor with no API/behavior change

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Extract remaining Tagger CLI markdown (3 files)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Tagger.kt, CalculateVersion.kt, Tag.kt
- [ ] Extract Digger CLI markdown (4 files)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Guide.kt, Digger.kt, CurrentContributionData.kt, AllContributionData.kt
- [ ] Verify npm package contents include all help resources
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test via jsLink for both CLIs
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
[Semver intent (initial): [none] — internal refactor, no behavior change]

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
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
