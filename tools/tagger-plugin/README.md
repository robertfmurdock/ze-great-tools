# Tagger

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/robertfmurdock/ze-great-tools?label=Release)

An opinionated plugin for automatic semantic versioning via git tags and information in commits.

This plugin is available on the Gradle Plugin Portal and on Maven Central.

## Setup

For the simplest use, add the tagger plugin to your Gradle root project as so:

```kotlin
plugins {
    id("com.zegreatrob.tools.tagger") version "1.0.0"
}

tagger {
    releaseBranch = "main"
}
```

## Getting Help

For a comprehensive usage guide with best practices and workflow guidance:

```bash
./gradlew taggerGuide
```

This displays fit assessment, typical usage patterns, and recommendations for using the plugin effectively.

To see all available tasks:

```bash
./gradlew tasks --group versioning
./gradlew tasks --group help
```

## Tasks

The tagger plugin adds a few tasks to your project.

### CalculateVersion

The `calculateVersion` task will generate a new version number based on all the commits since the last tag, and output
it.

For example:

```bash
./gradlew calculateVersion -q                                                                                                                

0.0.0
```

You can use the Gradle -q argument to suppress other output, and then consume output for use in versions.

For example, this will output the version to an environment variable:

```bash
export NEW_VERSION=$(./gradlew calculateVersion -q)
```

If you use GitHub, then there's a special argument that will automatically export it to a GitHub Actions environment
variable that will survive multiple tasks:

```bash
      - name: Generate Version 🧮
        run: ./gradlew calculateVersion -PexportToGithub=true --scan
      - name: Build 🔨
        run: ./gradlew release check -Pversion=${{ env.TAGGER_VERSION }} --scan
```

As you can see, this will export to a GitHub Actions environment variable called "TAGGER_VERSION", which can be used to
set the correct version number for subsequent builds.

By default, tagger will look for `[none]`, `[patch]`, `[minor]`, and `[major]` in commit messages in order to determine
the correct next version.

If you'd like to change these tokens, you can configure whatever regex you like:

```kotlin
tagger {
    noneRegex.set(Regex(".*(nope).*"))
    patchRegex.set(Regex(".*(widdle).*"))
    minorRegex.set(Regex(".*(middle).*"))
    majorRegex.set(Regex(".*(big-boi).*"))
}
```

By default, tagger will use a 'patch' version if it does not match any of the regexes. This behavior can be changed if
you prefer "none":

```kotlin
tagger {
    implicitPatch.set(false)
}
```

### Strict Mode (warningsAsErrors)

By default, tagger warnings (like using deprecated options or risky configurations) don't fail the build. Enable strict
mode to treat warnings as build failures:

```kotlin
tagger {
    warningsAsErrors.set(true)
}
```

This is useful in CI/CD pipelines where you want to enforce clean configuration and catch issues early. When enabled,
the `calculateVersion` task will fail if any warnings are detected.

#### Keep in mind!

In order to correctly generate the version number, the local git repository must be able to see the last relevant tag.
This means a shallow git clone that only includes new commits will not be able to generate the correct version numbers.

With GitHub actions, this can be fixed by configuration of `checkout` action:

```yml
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
```

### Release

The `release` task will - on a successful build - create a tag and push it back to the repository.

This will only occur if the version is not a "snapshot", and must run after all 'check' and 'publish' tasks are
complete (if they are scheduled).

Usage:

```bash
./gradlew release check -Pversion=${{ env.TAGGER_VERSION }} --scan
```

It can also be configured to publish a GitHub release.

```kotlin
tagger {
    githubReleaseEnabled.set(true)
}
```

By default, GitHub releases are **published immediately**. For enhanced supply chain security, you can configure
releases to be created as drafts first using the `githubReleaseDraft` property:

```kotlin
tagger {
    githubReleaseEnabled.set(true)
    
    // Publish immediately (default) - simpler workflow
    githubReleaseDraft.set(false)  // Can be omitted, this is the default
    
    // OR: Draft-first - recommended for supply chain security
    githubReleaseDraft.set(true)
}
```

When `githubReleaseDraft` is `false` (the default), releases are published immediately without requiring an extra step.

When `githubReleaseDraft` is `true`, releases are created as drafts. Your workflow must include an additional step to 
publish the release:

```bash
gh release edit $version --draft=false
```

### Supply Chain Security

When `githubReleaseEnabled` is true and `githubReleaseDraft` is true, tagger creates **immutable releases**
following GitHub's supply chain security best practices:

- **Tag immutability**: Git tags cannot be deleted or moved once the release is published
- **Asset protection**: Release assets cannot be modified or deleted after publication
- **Release attestations**: GitHub automatically generates cryptographic verification records
- **Draft-first workflow**: All assets are uploaded to a draft release before atomic publication

This prevents supply chain attacks where an attacker might modify published artifacts. Once published, your release is
permanently locked.

When `githubReleaseDraft` is false (the default), releases are published immediately upon creation. While simpler, this 
workflow loses the immutability protection benefits of the draft-first pattern since the tag and release become public 
before all assets are uploaded.

For more information,
see [GitHub's Immutable Releases documentation](https://docs.github.com/en/code-security/concepts/supply-chain-security/immutable-releases).

Naturally, all the operations involving git and GitHub will require appropriate permissions to be provided.

Tagger use the repository's git settings, so be sure to configure the username and email, and that the repository has
sufficient permissions to push tags.

```bash
git config user.name "bot"
git config user.email "bot@your-company.io"

./gradlew release -Pversion=${{ env.TAGGER_VERSION }}
```

Publishing a github-release will require the GH_TOKEN environment variable to be set as well. See:

```yml
jobs:
  build:
    runs-on: ubuntu-latest
    env:
      GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## FAQ

### Why two steps?

Every version of this tool I tried with a one-step process ended up
violating [Gradle's configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) in regular
use. Since local development builds should take priority, I decided on trading slightly more complicated build-server
setup (aka, calculateVersion then release) for being able to use the configuration cache all the time.

That said, its entirely possible there's a way to do it I didn't find! Open to suggestions and pull requests.

