# Tagger Test-Spec Hoist Parity Follow-up

## Goal
Hoist behavior-parity tests from CLI/plugin-exclusive suites into shared test specs where appropriate, while preserving intentional implementation-specific warning/output divergences.

## Constraints
- Keep behavior parity assertions at spec level; keep wiring/format/help/output-contract assertions implementation-specific.
- Use prior parity decisions as constraints: CLI deprecation-warning emission is intentionally divergent from Gradle plugin behavior.
- Before hoisting any warning-related test, classify the warning surface as: shared behavior vs intentional divergence, with evidence.
- Do not broaden scope into unrelated refactors; keep this card focused on test placement and any required minimal behavior alignment.
- Semver intent (initial): `[none]` (test-structure changes only). If implementation behavior changes are required to satisfy a spec hoist, escalate to `[patch]` and confirm with user.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Build a hoist candidate inventory across CLI/plugin-exclusive tests and classify each as `hoist`, `stay-exclusive`, or `blocked by divergence`
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Resolve warning-support consistency gate before hoisting warning-related tests (document evidence and decisions)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Hoist approved candidates into shared specs (starting with detached-head tag behavior and lightweight-tag calculate-version behavior) and wire both CLI + plugin implementations
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Add/adjust config-file parse failure parity coverage at shared-spec level (or documented spec-adapter equivalent) for both implementations
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Reconcile implementation-specific suites to remove redundant coverage and retain only invocation-specific assertions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move this file to agents.d/work_completed/

## Implementation Notes

### Semver intent (initial)
- Expected scope: `[none]` for test hoists/reorganization only.
- If any hoist exposes true behavior mismatch requiring production code change, pause for semver escalation confirmation (`[patch]`) before proceeding.

### Known prior context to respect
- Prior card documented pushback on hoisting due warning inconsistency and locked specific divergence decisions.
- Candidate warning surfaces must be explicitly checked against:
  - shared strict-mode behavior (`warningsAsErrors`) parity targets, and
  - intentional CLI-only warning emission (notably deprecation warning output).

### 2026-05-30 intake notes
- Reviewed against `agents.d/context/WORK_CHECKLIST.md`; checklist ordering and required items already conform.
- User explicitly authorized subagent delegation in-thread (`yes`).

### Candidate list (seed)
- `TagCommandTest.allowDetachedHeadPermitsTaggingDetachedHead` → likely hoist to `TagTestSpec`.
- `LightweightTagErrorTest` behavior assertions (not formatting) → likely hoist target to calculate-version shared spec.
- `.tagger` invalid parse failure parity coverage across CLI + plugin → likely hoist/spec-adapter parity target.

### 2026-05-30 hoist inventory/classification
- `TagCommandTest.allowDetachedHeadPermitsTaggingDetachedHead` → `hoist` (now moved to `TagTestSpec`).
- `LightweightTagErrorTest` scenario assertions → `hoist` (moved to `CalculateVersionTestSpec`; CLI-only file removed).
- CLI deprecation warning emission (`--disable-detached is deprecated`) → `stay-exclusive` (intentional CLI-only surface; plugin has no equivalent CLI flag UX).
- `.tagger` parse-failure parity across config-backed calculate-version implementations → `blocked by divergence` until CLI parse behavior aligns with plugin parse-failure contract.

### 2026-05-30 warning gate evidence/decision
- Shared warning behavior (detached-head/run-mode warnings + `warningsAsErrors`) remains covered in `CalculateVersionTestSpec` and applies to CLI + plugin.
- Intentional divergence retained: CLI deprecation warning messaging around `--disable-detached` remains CLI-specific (`CalculateVersionCommandTest`), not hoisted.
- No new warning divergence introduced by detached-head/lightweight hoists.

### 2026-05-30 semver update request (pending)
- While wiring shared parse-failure spec, discovered CLI `.tagger` parse errors currently throw raw `JsonDecodingException` instead of returning "Failed to parse .tagger file..." style failure.
- This is behavior-alignment work; semver intent likely escalates from `[none]` to `[patch]`.
- User explicitly approved semver escalation and patch-level behavior alignment in-thread (`yes`).

### Semver intent (updated)
- Updated scope: `[patch]` due CLI behavior fix for invalid `.tagger` parse handling (from unhandled exception to explicit parse failure message parity with plugin).

### 2026-05-30 hoist implementation status
- Detached-head `tag` success scenario moved from CLI-only `TagCommandTest` into shared `TagTestSpec` and verified in both CLI + plugin implementations.
- Lightweight-tag calculate-version failure scenarios moved from CLI-only `LightweightTagErrorTest` into shared `CalculateVersionTestSpec`; obsolete CLI-only test file removed.
- Plugin-specific config functional suite had redundant invalid-JSON parse test removed after adding shared config-parse spec adapter coverage path.
- Shared config-parse parity adapter now active in both CLI and plugin config-file calculate-version implementations.
- CLI `ConfigFileSource` now converts invalid `.tagger` parse exceptions into handled CLI error: `Failed to parse .tagger file: ...`.
- Post-refactor cleanup applied from subagent recommendations:
  - reduced branching complexity in `ConfigFileSource.findInvocations` by extracting invocation conversion and simplifying path traversal.
  - removed mutable config accumulator in CLI config-file test in favor of immutable `TaggerConfig(...)` construction.
  - removed duplicated calculate-version output parsing from `TaggerFileConfigFunctionalTest` in favor of shared `ConfigFileFunctionalTestSupport.parseCalculateVersion`.

### 2026-05-30 mandatory final refactor subagent
- Subagent run completed with structured report per `REFACTOR_AGENT.md`.
- Scope basis: no new commits in this card; effective scope used `HEAD` + working tree diff.
- Subagent cross-module validation (`./gradlew check`) passed.
- Minor issues identified (function length/duplication/mutable accumulator) were addressed in follow-up local refactor before final validation.

## Validation
- Commands:
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests 'com.zegreatrob.tools.tagger.cli.TagCommandTest.allowDetachedHeadPermitsTaggingDetachedHead' :tools-tests:tagger-plugin-test:functionalTest --tests 'com.zegreatrob.tools.tagger.TagFunctionalTest.allowDetachedHeadPermitsTaggingDetachedHead' --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.withLightweightTagShowsActionableErrorMessage' --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.withMultipleLightweightTagsShowsAllTagsInErrorMessage' --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandConfigFileTest.reportsErrorForInvalidTaggerFile' :tools-tests:tagger-plugin-test:functionalTest --tests 'com.zegreatrob.tools.tagger.CalculateVersionFunctionalTest.withLightweightTagShowsActionableErrorMessage' --tests 'com.zegreatrob.tools.tagger.CalculateVersionFunctionalTest.withMultipleLightweightTagsShowsAllTagsInErrorMessage' --tests 'com.zegreatrob.tools.tagger.CalculateVersionConfigFileFunctionalTest.reportsErrorForInvalidTaggerFile' --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests 'com.zegreatrob.tools.tagger.cli.TagCommandTest.allowDetachedHeadPermitsTaggingDetachedHead' --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.withLightweightTagShowsActionableErrorMessage' --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.withMultipleLightweightTagsShowsAllTagsInErrorMessage' :tools-tests:tagger-plugin-test:functionalTest --tests 'com.zegreatrob.tools.tagger.TagFunctionalTest.allowDetachedHeadPermitsTaggingDetachedHead' --tests 'com.zegreatrob.tools.tagger.CalculateVersionFunctionalTest.withLightweightTagShowsActionableErrorMessage' --tests 'com.zegreatrob.tools.tagger.CalculateVersionFunctionalTest.withMultipleLightweightTagsShowsAllTagsInErrorMessage' --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests 'com.zegreatrob.tools.tagger.cli.TagCommandTest.allowDetachedHeadPermitsTaggingDetachedHead' --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.withLightweightTagShowsActionableErrorMessage' --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandTest.withMultipleLightweightTagsShowsAllTagsInErrorMessage' :tools-tests:tagger-plugin-test:functionalTest --tests 'com.zegreatrob.tools.tagger.TaggerFileConfigFunctionalTest' --tests 'com.zegreatrob.tools.tagger.CalculateVersionFunctionalTest.withLightweightTagShowsActionableErrorMessage' --tests 'com.zegreatrob.tools.tagger.CalculateVersionFunctionalTest.withMultipleLightweightTagsShowsAllTagsInErrorMessage' --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandConfigFileTest.reportsErrorForInvalidTaggerFile' :tools-tests:tagger-plugin-test:functionalTest --tests 'com.zegreatrob.tools.tagger.CalculateVersionConfigFileFunctionalTest.reportsErrorForInvalidTaggerFile' --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest :tools-tests:tagger-plugin-test:functionalTest --console=plain`
  - `./gradlew :command-line-tools:tagger-cli:jvmTest --tests 'com.zegreatrob.tools.tagger.cli.CalculateVersionCommandConfigFileTest' :tools-tests:tagger-plugin-test:functionalTest --tests 'com.zegreatrob.tools.tagger.TaggerFileConfigFunctionalTest' --console=plain`
  - `./gradlew check --console=plain`
- Results:
  - First command passed.
  - Second command failed only on `CalculateVersionCommandConfigFileTest.reportsErrorForInvalidTaggerFile` due thrown `JsonDecodingException` in CLI config-file value source; plugin-side parity tests passed in same run.
  - Third command passed (shared detached-head + lightweight hoists verified across CLI and plugin).
  - Fourth command passed (including updated plugin config functional suite after redundant parse-test removal).
  - Fifth command passed after CLI parse-failure handling fix.
  - Sixth command passed (full relevant module suites).
  - Seventh command initially failed at compile due a fold inference refactor; fixed immediately and reran targeted suite successfully.
  - Eighth command passed (`./gradlew check` full project).
