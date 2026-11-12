# Create Working Examples Directory

**Work card protocol: 2**

## Goal
Create complete, working examples demonstrating tagger and digger usage in realistic scenarios so users can evaluate fit and get started quickly.

## Constraints and acceptance evidence
- Semver intent: `[none]` — examples and documentation only.
- Keep examples minimal, focused, self-contained, and runnable with published tool versions rather than local builds. Each has a README describing its scenario and expected outcomes and follows current tool guidance.
- Add an overview at `examples/README.md`, a simple Gradle/tagger project, an NPM/tagger project, and GitHub Actions examples for tagger and digger.
- Follow each README from a clean temporary directory and confirm the examples work. Validate workflow YAML, links, and documentation quality using [DOCUMENTATION.md](../context/DOCUMENTATION.md).

## Decisions
- Use published versions and copy-pasteable examples. Include realistic commit messages and expected output.
- Keep each example independently usable; add `.gitignore` files where running an example would otherwise leave build artifacts in the example tree.

## Progress
- Added the overview, Gradle and NPM tagger projects, and separate GitHub Actions workflows for tagger and digger. The Gradle example includes a wrapper; both local version examples use a temporary bare remote so a pushed release-branch commit produces a stable version.
- Verified the Gradle and NPM walkthroughs from clean temporary copies; both printed `1.2.4`. Ran the tagger workflow command with `GITHUB_ENV` set and confirmed it exported `TAGGER_VERSION=1.2.4`. Ran the Digger workflow task against a tagged sample repository and confirmed it wrote `build/digger/current.json` with the sample contribution.
- Parsed both workflow files as YAML, verified all local Markdown links, ran IDEA reformat and grammar checks on the five READMEs, and confirmed `git diff --check` passes.

## References
- Tagger Gradle plugin: `tools/tagger-plugin/README.md`
- Tagger CLI: `command-line-tools/tagger-cli/README.md`
- Digger: `tools/digger-plugin/README.md`
