# Tagger

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

## Tasks

The tagger plugin adds a few tasks to your project.

### CalculateVersion

The `calculateVersion` task will generate a new version number based on all of the commits since the last tag, and output it.

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

If you use github, then there's a special argument that will automatically export it to a Github Actions environment variable that will survive multiple tasks:

```bash
      - name: Generate Version 🧮
        run: ./gradlew calculateVersion -PexportToGithub=true --scan
      - name: Build 🔨
        run: ./gradlew release check -Pversion=${{ env.TAGGER_VERSION }} --scan
```

As you can see, this will export to a Github Actions environment variable called "TAGGER_VERSION", which can be used to set the correct version number for subsequent builds.

By default, tagger will look for `[none]`, `[patch]`, `[minor]`, and `[major]` in commit messages in order to determine the correct next version.

If you'd like to change these tokens, you can configure whatever regex you like:

```kotlin
tagger {
    noneRegex.set(Regex(".*(nope).*"))
    patchRegex.set(Regex(".*(widdle).*"))
    minorRegex.set(Regex(".*(middle).*"))
    majorRegex.set(Regex(".*(big-boi).*"))
}
```

By default, tagger will use a 'patch' version if it does not match any of the regexes. This behavior can be changed if you prefer "none":

```kotlin
tagger {
    implicitPatch.set(false)
}
```

#### Keep in mind!

In order to correctly generate the version number, the local git repository must be able to see the last relevant tag. This means a shallow git clone that only includes new commits will not be able to generate the correct version numbers.

With github actions, this can be fixed by configuration of `checkout` action:

```yml
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
```

### Release

The `release` task will - on a successful build - create a tag and push it back to the repository.

This will only occur if the version is not a "snapshot", and must run after all 'check' and 'publish' tasks are complete (if they are scheduled).

Usage:

```bash
./gradlew release check -Pversion=${{ env.TAGGER_VERSION }} --scan
```

It can also be configured to publish a github release.

```kotlin
tagger {
    githubReleaseEnabled.set(true)
}
```

Naturally, all of the operations involving git and github will require appropriate permissions to be provided.

Currently tagger uses Grgit under the hood, so you can change the git username and password using its environment variables:

```bash
export GRGIT_USER=your-git-user
export GRGIT_PASS=your-git-password

./gradlew release -Pversion=${{ env.TAGGER_VERSION }}
```

If you use Github Actions, this can be done using the "github token" as follows:

```yml
permissions:
  contents: write
jobs:
  build:
    runs-on: ubuntu-latest
    env:
      GRGIT_USER: ${{ secrets.GITHUB_TOKEN }}
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2
      - name: Generate Version 🧮
        run: ./gradlew calculateVersion -PexportToGithub=true --scan
      - name: Build 🔨
        run: ./gradlew release check -Pversion=${{ env.TAGGER_VERSION }} --scan
```

Make note that it must be granted permission to push tags, which can currently be done using `contents: write`.

## FAQ

### Why two steps?

Every version of this tool I tried with a one-step process ended up violating [Gradle's configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) in regular use. Since local development builds should take priority, I decided on trading slightly more complicated build-server setup (aka, calculateVersion then release) for being able to use the configuration cache all the time.

That said, its entirely possible there's a way to do it I didn't find! Open to suggestions and pull requests.

