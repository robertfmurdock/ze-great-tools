# GitHub Actions with Digger

This workflow collects contribution data from a Gradle project's Git history
and uploads the resulting JSON file for download from the workflow run. The
project must apply the published Digger plugin. A full checkout lets Digger
inspect commits and tags.

Copy [`digger.yml`](digger.yml) to `.github/workflows/digger.yml` in the Gradle
project. The workflow runs `currentContributionData`, which writes
`build/digger/current.json` for the commits since the most recent tag.

Expected outcome: a successful run includes a `digger-contribution-data`
artifact containing `current.json`. No repository content is changed.

See the [Digger plugin guide](../../tools/digger-plugin/README.md) for the
contribution fields and the [Gradle Tagger example](../gradle-tagger/README.md)
for a minimal Gradle project structure.
