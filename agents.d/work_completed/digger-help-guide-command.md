# Fit Assessment and Workflow Philosophy Guide

## Goal
Users can run `digger guide` to quickly assess whether digger fits their needs and understand its workflow philosophy.

## Constraints
- Users see clear "Use digger when..." criteria
- Users see clear "Do not use digger when..." anti-patterns
- Best practices for contribution tracking are explicit
- Workflow philosophy explains git-history-as-truth and tag-based boundaries
- Guide links to deeper documentation when available
- Content tested for key decision-making phrases
- Semver intent: `[minor]` - adds new subcommand functionality

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Users can assess fit for their use case from guide output
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Verify "use when" and "don't use when" criteria are clear
  - Verify workflow philosophy explains core principles
  - Verify best practices are actionable
  - Verify guide is accessible via `digger guide` command
- [x] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [x] Review changes against applicable playbooks and verify compliance
- [x] Move this file to agents.d/work_completed/

## Implementation Notes
Semver intent (initial): [minor] - adds new user-facing subcommand
[2026-05-31] User explicitly authorized subagent delegation for this card (yes, original thread).
[2026-05-31] User re-authorized subagent delegation for this resumed thread (yes).
[2026-05-31] Added `digger guide` subcommand with fit criteria, anti-patterns, workflow philosophy, and a link to deeper command docs.
[2026-05-31] Added `GuideTest` first; verified red (missing subcommand), then green after implementing command and wiring it in `Main.kt`.
[2026-05-31] No dedicated `why-digger.md` currently exists in `docs/`; guide points to digger CLI docs in repository for deeper reference.
[2026-05-31] Final refactor pass completed against scoped card files; simplified subcommand registration and made guide output assertions whitespace-tolerant for wrapped help text.

## Validation
- Commands:
  - `./gradlew :command-line-tools:digger-cli:jvmTest --tests com.zegreatrob.tools.digger.cli.GuideTest --console=plain`
  - `./gradlew :command-line-tools:digger-cli:check --console=plain`
  - `./gradlew check --console=plain`
- Results:
  - Guide test failed first (expected) before implementation, then passed after adding `Guide` command.
  - `:command-line-tools:digger-cli:check` passed.
  - `./gradlew check` passed.
