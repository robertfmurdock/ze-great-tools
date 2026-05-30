# Tagger CLI and Gradle Plugin Feature Parity

## Goal
Achieve feature parity between tagger CLI and Gradle plugin to ensure consistent behavior across invocation methods.

## Constraints
- Maintain backward compatibility for existing Gradle plugin users
- CLI-specific features (help text, format flags) are not in scope
- Deprecation warnings may be omitted from Gradle plugin if they would pollute build output
- Follow existing patterns: TaggerExtension properties → task properties → core behavior
- Bias toward spec-level tests for parity behavior; reserve implementation-level tests for wiring/format specifics
- Semver intent (initial): `[patch]` because expected outcome is parity alignment without intentional API expansion; re-evaluate if new behavior is introduced

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Audit CLI commands vs Gradle tasks to identify feature gaps
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Document intentional divergences (deprecation warnings, CLI-specific formatting)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Implement missing features with tests at spec level where appropriate
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [ ] Move to agents.d/work_completed/

## Implementation Notes

### Semver intent (initial)
- Expected scope: `[patch]` if only completing existing feature implementations, `[minor]` if new behavior surfaces
- Re-evaluate during implementation if changes expand scope
- If new findings indicate semver may increase, pause and ask the user to confirm direction; record the decision with date

### Why this card exists
During warningsAsErrors implementation (2026-05-30), discovered that:
- Gradle plugin lacked `warningsAsErrors` support initially (CLI had it)
- Gradle plugin doesn't emit deprecation warnings (CLI does)
- Test spec assumption of feature parity was incorrect

This suggests potential gaps in other areas. Need systematic audit to ensure users get consistent behavior regardless of invocation method.

### Discovered gaps (fill in during audit)
- `warningsAsErrors` support: ✅ completed (commit cf74f67)
- Deprecation warning emission: intentional divergence (pollutes build logs)
- `.tagger.disableDetached` was ignored by Gradle plugin extension while honored by CLI/config path: ✅ fixed
- Spec-level config-file parity coverage was missing for plugin calculate/tag flows: ✅ added via `CalculateVersionConfigFileFunctionalTest` and `TagConfigFileFunctionalTest` implementing shared test specs

### Design principles
- Feature behavior should match across implementations (exit codes, warning detection, version calculation)
- Output format differences are acceptable (CLI stderr vs Gradle logging)
- Deprecation warnings are CLI-specific since Gradle users see property deprecation via @Deprecated annotations
- New Gradle properties should follow existing convention: `TaggerExtension.property` → `Task.property` → core logic
- For parity assertions, prefer shared test specs first; add implementation tests only for source-specific behavior (e.g., Gradle wiring, CLI formatting)

### Session log
- 2026-05-30: Asked user for subagent authorization; no explicit "yes" provided. Continuing in single-agent mode.
- 2026-05-30: Audit confirmed intentional divergences are `--format`/structured output and `--dry-run` (CLI-only), and CLI deprecation warning emission; core version/tag behavior remains parity target.
- 2026-05-30: User explicitly authorized subagents (`yes`). Final refactor pass completed by subagent `Anscombe` (`019e79fc-7a15-7102-aa35-99095d63a163`), then recommendations were applied (shared config-file functional test support + reduced duplication/method complexity in new spec adapter tests).

## Validation
- Commands:
  - `./gradlew :tools-tests:tagger-plugin-test:functionalTest --tests com.zegreatrob.tools.tagger.TaggerFileConfigFunctionalTest.readsDisableDetachedFromTaggerFile --console=plain`
  - `./gradlew :tools-tests:tagger-plugin-test:functionalTest --tests com.zegreatrob.tools.tagger.CalculateVersionConfigFileFunctionalTest.whenNoRemoteButDisableDetachedIsFalseDoNotError --console=plain`
  - `./gradlew :tools-tests:tagger-plugin-test:functionalTest --tests com.zegreatrob.tools.tagger.TagConfigFileFunctionalTest.whenUserNameAndEmailAreParametersTagWillTagAndPush --tests com.zegreatrob.tools.tagger.TagConfigFileFunctionalTest.tagWillFailWhenUserEmailAndNameAreNotConfigured --console=plain`
  - `./gradlew :tools-tests:tagger-plugin-test:functionalTest --console=plain`
  - `./gradlew :tools-tests:tagger-plugin-test:check --console=plain`
  - `./gradlew check --console=plain`
- Results:
  - First run failed as expected (new test exposed gap): `readsDisableDetachedFromTaggerFile` failed before fix.
  - Subsequent targeted runs passed after fix.
  - Full `:tools-tests:tagger-plugin-test:functionalTest` passed.
  - `:tools-tests:tagger-plugin-test:check` passed after post-subagent refactor.
  - Initial `./gradlew check` failed on lint (`no-blank-line-before-rbrace`) in `TaggerFileConfigFunctionalTest`; fixed and reran.
  - Final `./gradlew check` passed.
