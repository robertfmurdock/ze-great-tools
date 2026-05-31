# Enum-Based Help Content Testing

## Goal
Ensure enum-based help content in digger and tagger CLIs is exhaustively tested such that adding a new enum element causes test failures, forcing documentation updates.

## Constraints
- Must follow TestMints pattern (setup/exercise/verify) per `.junie/guidelines.md`
- Tests must be Gradle-integrated (no standalone shell scripts) per project preference
- Declaration of semver intent: `[patch]` — enhances test coverage for existing help features without changing public API
- Applies to:
  - `digger-cli`: `OutputFormat`, `SemverType` (if documented in help)
  - `tagger-cli`: `OutputFormat`, `SnapshotReason`, `ChangeType` (if documented in help)

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] Audit digger and tagger help/guide content to identify all enum-based documentation
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Identify which enums appear in help text and how
  - Document findings in Implementation Notes
- [ ] Add exhaustive tests for SnapshotReason documentation in tagger calculate-version help
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Test must verify every enum element is documented
  - Test must fail if a new element is added without corresponding documentation
- [ ] Add exhaustive tests for OutputFormat documentation if present in help text
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Cover both digger and tagger if applicable
- [ ] Add exhaustive tests for ChangeType documentation if present in help text
  - Agent cycle: test → implement → refactor-light → verify pushable
- [ ] Add exhaustive tests for SemverType documentation if present in help text  
  - Agent cycle: test → implement → refactor-light → verify pushable
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes
[Semver intent (initial): [patch] — strengthens existing test coverage for help content, no API changes]

### Enum Inventory
- **digger-cli**:
  - `OutputFormat` (TEXT, JSON) — used in multiple commands
  - `SemverType` (None, Patch, Minor, Major) — core domain model
  
- **tagger-cli**:
  - `OutputFormat` (TEXT, JSON) — used in calculate-version
  - `SnapshotReason` (FORCED, DIRTY, AHEAD, BEHIND, NOT_RELEASE_BRANCH, NO_NEW_VERSION) — documented in calculate-version help table
  - `ChangeType` (Major, Minor, Patch, None) — core domain model

### Testing Strategy
- Follow existing `GuideTest` and `CalculateVersionCommandTest` patterns
- Use enum `.entries` to iterate over all values
- Assert each enum element appears in help output with appropriate context
- Tests will fail compilation or at runtime if enum changes but test doesn't

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
