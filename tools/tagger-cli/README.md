# Tagger CLI

![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/robertfmurdock/ze-great-tools?label=Release)
![NPM Version](https://img.shields.io/npm/v/git-semver-tagger?label=npm%20git-semver-tagger)

An opinionated program for automatic semantic versioning via git tags and information in commits. 

## Installation

You can install the tool using any NPM-like system.

### Local Example

```bash
npm i -D git-semver-tagger # this will install it into a project as a dev dependency

npx tagger calculate-version # You can use npx to run a project's programs easily
```

### Global Example

```bash
npm i -g git-semver-tagger # this will install it globally into npm

tagger calculate-version # Now it should be available via NPM's path on your shell.
```

## Commands

### Calculate Version

The `calculate-version` command will generate a new version number based on all of the commits since the last tag, and output as a string.

### Tag

The `tag` command will create a tag with the given version and push it back to the repository.

We recommend this command only is run after the build is validated. Use discernment to decide if it should happen before publication of artifacts, or afterward.

g## Help

For a full listing of the available options in the program, please use the built-in help command.

```bash
digger --help
```
