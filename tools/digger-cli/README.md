# Digger CLI

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/robertfmurdock/ze-great-tools?label=Release)
![NPM Version](https://img.shields.io/npm/v/git-digger?label=npm%20git-digger)

A program for extracting 'contribution data' from git repositories into JSON files. 

## Installation

You can install the tool using any NPM-like system.

### Local Example

```bash
npm i -D git-digger # this will install it into a project as a dev dependency

npx digger current-contribution-data $(pwd) # You can use npx to run a projects programs easily
```

### Global Example

```bash
npm i -g git-digger # this will install it globally into npm

digger current-contribution-data $(pwd) # Now it should be available via NPM's path on your shell.
```

## Commands

### CurrentContributionData

The `currentContributionData` task will collect the most recent contribution to the repository.

The most recent contribution is calculated by looking for the most recent, non-HEAD tag, and then including every commit after that until the current HEAD.

#### Output

The contribution data JSON is created at `./currentContributionData.json`.

It will include all fields listed [here](../digger-json/src/commonMain/kotlin/com/zegreatrob/tools/digger/json/ContributionDataJson.kt).

Any "Instant" in the specification is an ISO 8601 date-time. Any Duration is an ISO 8601 duration.

### AllContributionData

The `allContributionData` task will collect all the contributions in the git repository.

This is calculated by subdividing the repository by its tags, and each section becomes a contribution.

#### Output

The contribution data JSON is created at `./allContributionData.json`, as a JSON array.

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

This can be overridden by argument:

```bash
digger currentContributionData --label SomethingMoreExciting ${pwd}
```

### Ease

This is parsed out of the commit message by looking for a number between one and five, wrapped in dashes.

This field is inspired by https://www.scrumexpert.com/knowledge/measuring-joy-for-software-developers/

eg:
commit message: `-3- I did that thing`
produces: { ease: 3 }

## Help

For a full listing of the available options in the program, please use the built-in help command.

```bash
digger --help
```
