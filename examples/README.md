# Working Examples

These small projects show tagger and digger in common setups. Each directory has
its own instructions and can be copied into a project or tried independently.

| Example                                                       | What it demonstrates                                                          |
|---------------------------------------------------------------|-------------------------------------------------------------------------------|
| [Gradle with tagger](gradle-tagger/README.md)                 | Calculate a release version from annotated Git tags and commit messages.      |
| [NPM with tagger](npm-tagger/README.md)                       | Install and run the published tagger CLI as a project development dependency. |
| [GitHub Actions with tagger](github-actions-tagger/README.md) | Calculate and expose a version in a GitHub Actions job.                       |
| [GitHub Actions with digger](github-actions-digger/README.md) | Collect contribution data and upload the JSON output as an artifact.          |

The examples use published tool versions and read-only workflows. Version
calculation and contribution collection inspect Git history; neither workflow
creates or pushes tags.
