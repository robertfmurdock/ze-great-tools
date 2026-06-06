# Create GitHub Action Wrapper for Tagger

## Goal
Package tagger as a GitHub Action for easy integration in GitHub Actions workflows and publish to GitHub Actions Marketplace to increase discoverability.

## Constraints
- Action must wrap existing NPM package (no duplication of tagger logic)
- Must support both `calculate-version` and `tag` commands
- Should expose common options as action inputs
- Must output version for use in subsequent workflow steps
- Semver intent: `[minor]` - new distribution channel for existing functionality
- Action metadata must follow GitHub Actions standards

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Research GitHub Actions structure for JavaScript/TypeScript actions vs composite actions (composite likely simpler for NPM wrapper)
  - Agent cycle: investigate only
  - Update plan if different approach needed
- [ ] Create `action.yml` at repository root defining:
    - Action metadata (name, description, author, branding)
    - Inputs (command, release-branch, format, version, etc.)
    - Outputs (version, snapshot status)
    - Runs specification (composite action using npm)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Create `.github/workflows/test-action.yml` to test the action in CI
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add `examples/github-actions/tagger-action.yml` showing action usage (integrate with existing examples work)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Document GitHub Action in main README.md under a new "GitHub Actions" section
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Test action locally using `act` or in a test repository
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
Semver intent: `[minor]` - new distribution mechanism, no breaking changes to existing tools.

GitHub Action provides:
1. Easy discovery via GitHub Actions Marketplace
2. Simple integration in workflows (no manual NPM installation)
3. Standard action output format for version passing between steps

Composite action approach (recommended):
- Uses `runs.using: composite`
- Steps install NPM package and run tagger
- No custom JavaScript required
- Easier to maintain

Alternative: JavaScript action (more complex, requires build step)

Reference:
- GitHub Actions documentation: https://docs.github.com/en/actions/creating-actions
- Composite actions: https://docs.github.com/en/actions/creating-actions/creating-a-composite-action

Action should support marketplace search keywords: `semantic-versioning`, `versioning`, `release-automation`, `git-tags`

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] Validate `action.yml` syntax using GitHub's action validator or by pushing and checking workflow results
- [ ] Test action in `.github/workflows/test-action.yml` - verify it runs and produces expected output
- [ ] Verify action outputs can be consumed by subsequent workflow steps
- [ ] Run link verification on updated documentation (see DOCUMENTATION.md)
