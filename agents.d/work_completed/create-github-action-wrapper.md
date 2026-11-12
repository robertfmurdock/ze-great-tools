# Create GitHub Action Wrapper for Tagger

**Work card protocol: 2**

## Goal
Package Tagger as a GitHub Action for direct repository use in GitHub Actions workflows. Marketplace publication is deferred to a separate work card.

## Constraints and acceptance evidence
- Semver intent: `[minor]` — new distribution channel for existing functionality.
- Wrap the existing NPM package without duplicating tagger logic. Support `calculate-version` and `tag`, expose common options, and provide version and snapshot outputs for later workflow steps.
- Follow GitHub Actions metadata requirements.
- Add root `action.yml`, a CI workflow exercising the action, a usage example under `examples/github-actions-action/`, and README documentation.
- Verify the action through a direct repository reference in CI and that a following workflow step can consume its outputs. Validate action metadata and documentation links using the applicable repository guidance.
- Marketplace publication and its discovery metadata are deferred to a separate work card.

## Decisions
- Start with a composite action because it can invoke the existing NPM package without a custom build step. Reconsider a JavaScript action only if the composite approach cannot meet requirements.
- Direct-reference form for users: `owner/repository/action.yml@ref`.
- Marketplace publication is a separate follow-up after direct-reference use is proven.

## References
- [Creating GitHub Actions](https://docs.github.com/en/actions/creating-actions)
- [Creating a composite action](https://docs.github.com/en/actions/creating-actions/creating-a-composite-action)

## Tagger smoke test addendum

- Fixed the root composite action to pass an explicit value for `--force-snapshot`.
- Moved the snapshot calculation and output assertions into Build before the Gradle release step, and removed the standalone smoke-test workflow. Build now fails and skips publishing if the action fails or returns an empty version or a snapshot value other than `true`.
- Workflow lint and Gradle checks passed locally. The published Tagger CLI returned a nonempty snapshot version and `snapshot=true` with `--force-snapshot=true`.

- Hosted verification is pending. Confirm both Tagger steps pass in the first Build with this change before the Gradle release step runs.
