# Contributing

Thanks for your interest in improving ze-great-tools. Contributions of all sizes are welcome, including bug reports,
documentation improvements, and code changes.

## Set up your environment

1. Install a JDK 21 distribution.
2. Clone this repository and change into its directory.
3. Run the full check to confirm the project builds in your environment:

   ```shell
   ./gradlew check
   ```

The Gradle wrapper downloads the required Gradle version. No separate Gradle installation is needed.

## Tests and changes

Use a test-first approach for behavior changes: add or update a test that demonstrates the intended behavior, confirm it
fails for the expected reason, then implement the change and confirm the test passes. Run the relevant module checks
while iterating and run `./gradlew check` before submitting.

Keep changes focused and explain why they are useful. Update documentation when behavior or setup changes. Avoid
including unrelated formatting or generated files.

## Issues and pull requests

- Search existing issues before opening a new one. Use the bug report or feature request form so maintainers have the
  context needed to respond.
- For a substantial change, open an issue first to discuss the approach.
- Open pull requests against `main`. Describe the problem, the change, and how you verified it. Link related issues and
  include examples or screenshots when they help explain the change.
- Keep pull requests focused, and be ready to discuss feedback or make revisions.

Please follow the [Code of Conduct](CODE_OF_CONDUCT.md) in project spaces. Repository-specific instructions for
automated coding agents are in [`CLAUDE.md`](CLAUDE.md).
