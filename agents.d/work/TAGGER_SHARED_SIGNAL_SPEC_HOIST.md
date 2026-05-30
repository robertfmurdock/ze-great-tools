# Tagger Shared-Signal Spec Hoist

## Goal
Hoist user-facing parity assertions for “signal exists + migration guidance exists” into shared specs, while allowing implementation-specific evidence channels (CLI runtime output vs plugin API deprecation metadata).

## Constraints
- Shared specs should assert outcome-level UX intent, not force identical implementation mechanisms.
- Preserve intentional divergence where transport differs (for example, CLI stderr warning text vs Gradle/Kotlin deprecation annotations).
- Keep wiring/help/format-specific checks implementation-local unless they represent shared behavior.
- Do not broaden scope into unrelated warning or output refactors.
- Semver intent (initial): `[none]` (test architecture/coverage changes only). Escalate and confirm if production behavior changes become necessary.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Build inventory of currently implementation-specific “user signal” tests and classify each as `hoist`, `stay-exclusive`, or `blocked`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Introduce shared signal-spec pattern for deprecation guidance (signal presence + replacement guidance) with implementation-specific evidence adapters
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Migrate selected existing tests to the shared signal-spec pattern and remove redundant implementation-only assertions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Confirm warning/deprecation parity decisions remain explicit and documented where behavior intentionally diverges
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes

### Subagent Authorization
- **Date**: 2026-05-30
- **Response**: Yes — subagent delegation authorized for this card

### Semver intent (initial)
- Expected scope: `[none]` (shared-spec and test-placement refactor).
- If implementation changes are required to make parity assertions meaningful, pause for semver confirmation before proceeding.

### Framing
- Shared assertion target: user receives deprecation/migration signal.
- Evidence adapter examples:
  - CLI: runtime warning output includes migration guidance.
  - Plugin: API/property carries `@Deprecated` guidance toward replacement.

### Signal Test Inventory (2026-05-30)

**Already Hoisted (in shared specs):**
- `whenNoRemoteProduceError` - ⚠️ warning symbol + RISK section (CalculateVersionTestSpec:94)
- `whenAllowDetachedHeadOnReleaseBranchEmitWarning` - release branch warning (CalculateVersionTestSpec:181)
- `forceSnapshotReportsForcedReason` - FORCED label in output (CalculateVersionTestSpec:602)
- `warningsAsErrorsCausesNonZeroExitWhenDetachedHeadWarningPresent` - warnings-as-errors behavior (CalculateVersionTestSpec:629)
- `withLightweightTagShowsActionableErrorMessage` - migration guidance for lightweight tags (CalculateVersionTestSpec:654)
- `withMultipleLightweightTagsShowsAllTagsInErrorMessage` - plural guidance (CalculateVersionTestSpec:686)
- `reportsErrorForInvalidTaggerFile` - parse failure message (CalculateVersionConfigFileParseFailureTestSpec:12)
- `whenNotOnCorrectBranchAndWarningsAsErrorsTagWillNotDoAnythingAndError` - branch error signal (TagTestSpec:168)
- `whenNotOnCorrectBranchTagWillNotDoAnythingAndError` - branch warning signal (TagTestSpec:202)

**Stay Implementation-Specific:**
- `parseCalculateVersion()` helper (ConfigFileFunctionalTestSupport.kt:46) - CLI stderr parsing mechanics

**Classification Result:**
All user-facing signal tests are already in shared specs. The work card goal appears already achieved — parity assertions exist at the spec level, and implementations provide their evidence through the test result structures (Success.warnings, error messages, etc.).

**Pattern Verification:**
- Spec: `CalculateVersionTestSpec.whenNoRemoteProduceError()` asserts `result.reason.contains("⚠️")`
- Plugin impl: `CalculateVersionFunctionalTest.execute()` parses Gradle output, splits by "⚠️" prefix → `TestResult.Success(warnings=...)`
- Config impl: `CalculateVersionConfigFileFunctionalTest.execute()` uses shared helper → same result structure

The evidence adapter pattern is working: specs assert UX intent, implementations surface evidence through their transport (Gradle output, CLI stderr).

**Shared Signal-Spec Pattern (Already Exists):**
- `TestResult.Success(message, details, warnings)` — warnings field collects signal strings
- `TestResult.Failure(reason)` — reason field contains error messages with migration guidance
- Spec tests assert against these fields: `result.reason.contains("⚠️")`, `result.warnings.contains("release branch")`
- Implementation adapters (`execute()` methods) parse their transport and populate TestResult fields
- Pattern supports both deprecation warnings (Success.warnings) and error guidance (Failure.reason)

## Validation
- Commands: [filled in as work progresses]
- Results: [filled in before completion]
