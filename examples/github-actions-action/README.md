# GitHub Actions with the Tagger Action

This example calculates a version with the Tagger composite action and makes
both the version and snapshot flag available to later steps in the job. The
checkout must fetch full history so Tagger can inspect earlier tags.

```yaml
name: Build
on:
  push:
    branches: [main]

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
        with:
          fetch-depth: 0
      - id: tagger
        uses: robertfmurdock/ze-great-tools/action.yml@main
        with:
          release-branch: main
          tagger-version: latest
      - name: Build with calculated version
        env:
          VERSION: ${{ steps.tagger.outputs.version }}
          SNAPSHOT: ${{ steps.tagger.outputs.snapshot }}
        run: echo "Building $VERSION (snapshot=$SNAPSHOT)"
```

The default `command` is `calculate-version`. To create and push a tag after
your build succeeds, add a separate step with `command: tag` and the approved
version. Grant `contents: write` to that job and provide `release-branch`; the
tag command requires it. Use `tag-dry-run: true` to inspect the operation
without creating or pushing a tag.

See the [Tagger CLI guide](../../command-line-tools/tagger-cli/README.md) for
the supported inputs and the [Gradle Tagger action example](../github-actions-tagger/README.md)
for the separate Gradle plugin integration.
