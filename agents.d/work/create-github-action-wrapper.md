# Create GitHub Action Wrapper for Tagger

**Work card protocol: 2**

## Goal
Package tagger as a GitHub Action for easy integration in GitHub Actions workflows and publish to GitHub Actions Marketplace to increase discoverability.

## Constraints and acceptance evidence
- Semver intent: `[minor]` — new distribution channel for existing functionality.
- Wrap the existing NPM package without duplicating tagger logic. Support `calculate-version` and `tag`, expose common options, and provide version and snapshot outputs for later workflow steps.
- Follow GitHub Actions metadata and Marketplace requirements.
- Add root `action.yml`, a CI workflow exercising the action, a usage example under `examples/github-actions/`, and README documentation.
- Verify the action runs in CI and that a following workflow step can consume its outputs. Validate action metadata and documentation links using the applicable repository guidance.

## Decisions
- Start with a composite action because it can invoke the existing NPM package without a custom build step. Reconsider a JavaScript action only if the composite approach cannot meet requirements.
- Marketplace search terms to consider: `semantic-versioning`, `versioning`, `release-automation`, `git-tags`.

## References
- [Creating GitHub Actions](https://docs.github.com/en/actions/creating-actions)
- [Creating a composite action](https://docs.github.com/en/actions/creating-actions/creating-a-composite-action)
