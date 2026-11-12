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

## References
- Tagger Gradle plugin: `tools/tagger-plugin/README.md`
- Tagger CLI: `command-line-tools/tagger-cli/README.md`
- Digger: `tools/digger-plugin/README.md`
