# Digger

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/robertfmurdock/ze-great-tools?label=Release)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.zegreatrob.tools.digger?label=Digger%20Plugin)](https://plugins.gradle.org/plugin/com.zegreatrob.tools.digger)

A plugin for extracting 'contribution data' from git repositories into JSON files. 

## Setup

Adding the digger plugin to your Gradle project:

```kotlin
plugins {
    id("com.zegreatrob.tools.digger") version "1.1.1"
}

```

## Tasks

### CurrentContributionData

The `currentContributionData` task will collect the most recent contribution to the repository.

The most recent contribution is calculated by looking for the most recent, non-HEAD tag, and then including every commit after that until the current HEAD.

#### Output

The contribution data JSON is created at `./build/digger/current.json`.

It will include all fields listed [here](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt).

Any "Instant" in the specification is an ISO 8601 date-time. Any Duration is an ISO 8601 duration.

### AllContributionData

The `allContributionData` task will collect all the contributions in the git repository.

This is calculated by subdividing the repository by its tags, and each section becomes a contribution.

#### Output

The contribution data JSON is created at `./build/digger/all.json`, as a JSON array.

It will include all fields listed [here](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt).

Any "Instant" in the specification is an ISO 8601 date-time. Any Duration is an ISO 8601 duration.

## Fields of Interest

### Authors

This will include all authors listed on the commit, including committer, author, and co-authors.

### Story ID

This is parsed out of the commit message by looking for square bracketed text that does not match semver.

eg:
commit message: `[Cowdog-42] [patch] I did that thing`
produces: { storyId: "Cowdog-42" }

### Semver

This is parsed out of the commit message by looking for the strings "[major]", "[minor]", "[patch]", or "[none]".

eg:
commit message: `[Cowdog-42] [patch] I did that thing`
produces: { semver: "Patch" }

### Label

All contributions from one repository will share the same label. By default, this will be the Gradle project's name.

This can be overridden at plugin configuration time:

```kts
digger {
    label.set("SomethingMoreExciting")
}
```

### Ease

This is parsed out of the commit message by looking for a number between one and five, wrapped in dashes.

This field is inspired by https://www.scrumexpert.com/knowledge/measuring-joy-for-software-developers/

eg:
commit message: `-3- I did that thing`
produces: { ease: 3 }
