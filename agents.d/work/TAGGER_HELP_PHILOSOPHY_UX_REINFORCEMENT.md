# Tagger Help Philosophy UX Reinforcement

## Goal
Strengthen Tagger in-app help so users understand both how to run commands and why Tagger enforces its release/version posture, reducing misuse and policy drift.

## Constraints
- Keep `stdout` as parseable API and `stderr` as diagnostic guidance.
- Preserve existing command semantics and backward compatibility; this card is UX/help-first.
- Maintain CLI and Gradle plugin parity where behavior overlaps.
- Semver intent (initial): `[none]` for analysis/work-card planning; implementation items in this card are expected to be `[patch]` unless they change machine-readable output contracts.

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Add a philosophy-first "fit check" layer to in-app help before option-level details
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add guided "correct usage path" help that operationalizes the two-step workflow (`calculate-version` then `tag`)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Improve failure and warning UX with direct remediation steps tied to Tagger principles
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add Gradle plugin in-app discoverability and help parity for tasks and extension settings
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add anti-pattern guardrails in help content to prevent common misuse patterns
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add focused help-text regression tests that assert philosophy and workflow cues stay present
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Review changes against applicable playbooks and verify compliance
- [ ] Move this file to agents.d/work_completed/

## Implementation Notes
Semver intent (initial): `[none]` for this planning artifact only.

### Source Inputs Reviewed
- `docs/why-tagger.md`
- CLI help surfaces in:
  - `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/Tagger.kt`
  - `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/CalculateVersion.kt`
  - `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/Tag.kt`
  - `command-line-tools/tagger-cli/src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/GenerateSettingsFile.kt`
- Help-oriented tests in:
  - `TaggerTest.kt`
  - `CalculateVersionCommandTest.kt`
  - `TagCommandTest.kt`
  - `GenerateSettingsFileCommandTest.kt`
- Gradle plugin entry/task surfaces in:
  - `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/TaggerPlugin.kt`
  - `tools/tagger-plugin/src/main/kotlin/com/zegreatrob/tools/tagger/*.kt`

### Current-State Gaps (Why Tagger vs In-App Help)
1. Philosophy is present in docs but only partially surfaced in command help.
   - Root help explains output and automation well, but fit boundaries ("not for you if...") and scope boundaries are not visible in the command UX.
2. Option help is strong, workflow help is thin.
   - Users can learn each flag, but the intended release flow is not emphasized as a first-class path with clear decision points.
3. Two-step posture can still be bypassed socially.
   - `tag --version` is required, but help does not strongly discourage manually overriding computed versions in normal flows.
4. Snapshot reasons are listed, but remediation flow is fragmented.
   - Reasons appear with messages, but there is no consolidated "if reason X, do Y next" shortcut in help output.
5. Gradle plugin in-app discoverability is weak.
   - Tasks are registered without user-facing descriptions/groups, reducing `gradle tasks`/`gradle help --task ...` guidance.
6. Anti-pattern callouts are mostly in docs/README, not in immediate CLI/task help touchpoints.

### Ideas and Adjustments

#### 1) Philosophy-First Fit Check in Root Help
- Add a short "Use Tagger when..." and "Do not use Tagger when..." section to `tagger --help`, distilled from `docs/why-tagger.md`.
- Keep this concise and non-marketing; 3 bullets each, focused on source-of-truth and workflow posture.
- Include one direct pointer to full rationale: `docs/why-tagger.md`.

#### 2) Workflow-Centric Help Mode
- Add a `Quick start` block in root help:
  1. `tagger calculate-version --format=json`
  2. Gate on `snapshot == false`
  3. `tagger tag --version <calculated>`
- Add an explicit "default safe path" note that tagging should consume computed version output, not an ad hoc value.

#### 3) Remediation Map for Snapshot Reasons
- In root help or `calculate-version --help`, include a compact map:
  - `DIRTY` -> commit/stash changes
  - `AHEAD`/`BEHIND` -> sync with remote
  - `NOT_RELEASE_BRANCH` -> switch/set release branch
  - `NO_NEW_VERSION` -> add explicit semver signal commit
  - `FORCED` -> remove force in release flows
- Keep output behavior unchanged; this is explanatory help only.

#### 4) Tag Command Guardrail Messaging
- Strengthen `tag --help` text around `--version`:
  - "Expected input is the output of `calculate-version`."
  - "Manual override is for controlled exceptions."
- Add an example pipeline snippet showing handoff between commands.

#### 5) Config UX: Policy as Code Framing
- Expand `.tagger` help suffix to state that config represents version/tag policy and should be code-reviewed.
- In `generate-settings-file --help`, add one-line guidance on when to use `--merge` (safe adoption into existing configs).

#### 6) Gradle Plugin In-App Help Upgrade
- Add `group` and `description` for `previousVersion`, `calculateVersion`, `tag`, `release`, `commitReport`, `githubRelease`.
- In task descriptions, mirror CLI intent language:
  - read-only calculation vs side-effectful tagging
  - release-branch policy
  - snapshot guardrails
- Ensure `gradle help --task calculateVersion` conveys release prerequisites and expected sequencing.

#### 7) Warnings-As-Errors Clarity Across Surfaces
- Normalize wording for warnings-as-errors in CLI and plugin help:
  - default behavior
  - strict mode behavior
  - recommended CI posture
- Add one concise example for migration periods vs enforcement periods.

#### 8) Anti-Pattern "Do/Don't" Micro-Patterns
- Add terse do/don't pairs in help:
  - Do: calculate then tag.
  - Don't: tag arbitrary versions from local assumptions.
  - Do: run with full history/tags in CI.
  - Don't: run with shallow/partial checkout when calculating release versions.
- Keep this to immediate user actions only.

#### 9) Progressive Disclosure for New Users vs Automation
- Keep `--format=json` and stdout/stderr contract prominent.
- Add a `See also` structure:
  - root help -> subcommand details
  - subcommands -> policy doc and detached-head doc where relevant
- Avoid duplicating full field/error catalogs in README; keep in-app help authoritative.

#### 10) Test Plan for Help UX Durability
- Add/expand tests to assert presence of:
  - fit-check wording
  - two-step workflow guidance
  - remediation map anchors
  - plugin task descriptions (functional tests where appropriate)
- Use regex-based assertions for wrapped help text.

### Proposed Delivery Order
1. Root CLI philosophy + workflow help updates.
2. Subcommand remediation and guardrail wording.
3. Gradle task descriptions/groups and parity wording.
4. Tests for all new guidance anchors.
5. README touch-ups only where links/examples must align with in-app help.

### Success Criteria
- A first-time user can answer from in-app help alone:
  - Is Tagger a fit for our workflow?
  - What is the safe default release path?
  - Why am I getting `-SNAPSHOT`, and what do I do next?
  - How do CLI and Gradle plugin flows map to each other?
- Existing automation output contracts remain unchanged.

## Validation
- Commands: N/A (planning/work card only)
- Results: N/A
