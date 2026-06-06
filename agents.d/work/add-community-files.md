# Add Community Files to Repository

## Goal
Add standard community files (CONTRIBUTING.md, CODE_OF_CONDUCT.md, issue templates) to lower contribution barriers and signal a welcoming open source community.

## Constraints
- Follow GitHub's recommended structure for community health files
- Align with project values: clarity, simplicity, test-first discipline
- Keep contribution barriers low while maintaining quality standards
- Semver intent: `[none]` - documentation only, no code changes
- Files must be verified using DOCUMENTATION.md protocol (link verification, grammar, formatting)

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Create CONTRIBUTING.md at repository root covering:
    - How to set up development environment
    - How to run tests (`./gradlew check`)
    - TDD expectations (write failing test first)
    - How to submit issues and PRs
    - Link to CLAUDE.md for agent instructions
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Create CODE_OF_CONDUCT.md at repository root using standard template (Contributor Covenant recommended)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Create `.github/ISSUE_TEMPLATE/bug_report.md` with structured template for bug reports
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Create `.github/ISSUE_TEMPLATE/feature_request.md` with structured template for feature requests
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Verify all links in community files work (see DOCUMENTATION.md)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Run grammar and formatting checks on all new markdown files
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
Semver intent: `[none]` - documentation files only, no code or behavior changes.

Reference existing documentation style from README.md and docs/ directory.

CONTRIBUTING.md should emphasize:
- Test-first development (see agents.d/context/TESTING.md for internal philosophy)
- Running `./gradlew check` before submitting
- Clear, focused PRs
- Opening issues before large changes

CODE_OF_CONDUCT.md should use standard template to avoid reinventing the wheel.

Issue templates help guide contributors to provide useful information without being overly prescriptive.

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass (no impact expected, but verify)
- [ ] Verify CONTRIBUTING.md renders correctly on GitHub
- [ ] Verify CODE_OF_CONDUCT.md renders correctly on GitHub
- [ ] Verify issue templates appear in "New Issue" flow on GitHub
- [ ] Run link verification on all new files (see DOCUMENTATION.md)
- [ ] Run grammar check on all new files: `mcp__idea__get_file_problems` for each file
