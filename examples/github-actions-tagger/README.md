# GitHub Actions with Tagger

This workflow calculates a version for a Gradle project on pushes to `main`
and exposes it as `TAGGER_VERSION` to later steps in the same job. It needs a
Gradle project that applies the published Tagger plugin and configures its
release branch as `main`. The workflow only calculates a version; it does not
create or push a tag.

Copy [`tagger.yml`](tagger.yml) to `.github/workflows/tagger.yml` in that
project. The checkout fetches full history so Tagger can see the previous
annotated version tag.

Expected outcome: the workflow log prints the calculated version, such as
`1.2.4`, and later steps can read it as `${{ env.TAGGER_VERSION }}`.

See the [Gradle Tagger example](../gradle-tagger/README.md) for a minimal
project and the [Tagger plugin guide](../../tools/tagger-plugin/README.md) for
release task guidance.
