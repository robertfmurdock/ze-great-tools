# Tagger vs Axion Release Plugin: Choosing the Right Tool

Both tagger and Axion Release Plugin solve the same fundamental problem: deriving project versions from Git history
rather than hardcoding them in build files. They share core values but differ in their approach to version derivation
and workflow design. This guide helps you choose the right tool for your Gradle projects.

## Philosophical Alignment

**What Both Tools Believe:**

- **SCM as source of truth**: Version information lives in Git, not in build files
- **Automated versioning from Git**: Remove manual version string management from build files
- **Gradle-native integration**: First-class Gradle plugin support with idiomatic task integration
- **Semantic Versioning**: Adherence to SemVer principles
- **Automation over manual bumping**: Remove manual version string updates from build files
- **SNAPSHOT semantics**: Clear distinction between release and development versions

**Where They Diverge:**

- **Tagger**: Deterministic and commit-driven. Commit messages contain increment instructions (`[major]`, `[minor]`, `[patch]`) that determine *how to bump* the version from the last tag. Given the same Git state, always produces the same version. Two-phase workflow separates calculation from tagging. Opinionated about annotated tags, branch context, and policy enforcement.

- **Axion**: Event-driven and tag-based. Each `release` command automatically increments the patch version from the last tag, regardless of commit content. Manual version specification via `markNextVersion` for major/minor bumps. Single-command release workflow creates tags directly. Does not parse commits for version decisions.

**In other words:** Tagger calculates version from Git state (deterministic); Axion increments version on each release command (event-driven).

## Quick Decision Guide

### Choose Tagger if:

1. You want **commit messages to declare increment instructions** explicitly (`[major]`, `[minor]`, `[patch]`)
2. You need **two-phase workflow** (calculate version → validate build → tag after success)
3. You want **strict policy enforcement** (annotated tags only, branch context required, detached HEAD blocked)
4. You need **detailed snapshot diagnostics** (six explicit reasons why a version is/isn't a release candidate)
5. You want **platform neutrality** beyond Gradle (tagger also has standalone CLI for multi-language repos)

### Choose Axion if:

1. You prefer **auto-increment from last tag** (each release increments patch, no commit parsing)
2. You want **simpler workflow** (single `release` task creates tag immediately)
3. You're comfortable with **event-driven versioning** (each release command bumps version)
4. You prefer **less prescriptive** approach (fewer constraints on Git workflow)
5. You want **minimal configuration** (works out of the box with basic setup)

## Feature Comparison

| Aspect                   | Tagger                                                                                         | Axion Release Plugin                                                |
|--------------------------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| **Version Source**       | Last tag + increment instructions from commits                                                 | Last tag + auto-increment or manual specification                   |
| **Commit Convention**    | Regex-based tokens (default: `[major]`, `[minor]`, `[patch]`, `[none]`) — fully customizable   | None required (doesn't parse commits for versioning)                |
| **Workflow**             | Two-phase: `calculateVersion` → [validate] → `tag`                                             | Single-phase: `release` task tags immediately                       |
| **Version Increment**    | Determined by commit content (`[minor]` in any commit → minor bump)                            | Automatic patch increment; manual `markNextVersion` for major/minor |
| **Tag Format**           | Annotated tags only (enforced)                                                                 | Annotated tags by default; configurable                             |
| **Tag Pattern**          | `v{version}` by default (e.g., `v1.2.3`)                                                       | `v{version}` by default (e.g., `v0.1.0`)                            |
| **Snapshot Suffix**      | `-SNAPSHOT` with six diagnostic reasons                                                        | `-SNAPSHOT` for commits after last tag                              |
| **Snapshot Diagnostics** | Explicit reasons: `DIRTY`, `AHEAD`, `BEHIND`, `NOT_RELEASE_BRANCH`, `FORCED`, `NO_NEW_VERSION` | Implicit (commits exist after tag = snapshot)                       |
| **Policy Enforcement**   | Strict (annotated tags, full history, branch context, detached HEAD blocked)                   | Flexible (works in most Git states)                                 |
| **Configuration**        | `.tagger` JSON file or CLI args                                                                | Gradle `scmVersion` DSL block                                       |
| **Gradle Tasks**         | `calculateVersion`, `versionMessage`, `tag`                                                    | `currentVersion` (alias: `cV`), `release`, `markNextVersion`        |
| **Platform Scope**       | CLI + Gradle plugin (multi-language support)                                                   | Gradle plugin only                                                  |
| **Scriptability**        | Highly scriptable (shell-friendly JSON output)                                                 | Gradle-native (less shell scripting needed)                         |
| **Error Handling**       | Actionable error messages with remediation commands                                            | Standard Gradle error output                                        |
| **Default Behavior**     | Requires explicit version tokens in commits                                                    | Auto-increments patch version by default                            |

## Workflow Comparison

### Tagger Workflow

```bash
# Phase 1: Calculate version (read-only, deterministic)
./gradlew calculateVersion -q
# Output: 1.2.3-SNAPSHOT

# Phase 2: Validate build with calculated version
VERSION=$(./gradlew calculateVersion -q --format=json | jq -r '.data.version')
./gradlew build test -Pversion=$VERSION

# Phase 3: Publish artifacts (optional - your choice when to tag)
./gradlew publish -Pversion=$VERSION

# Phase 4: Tag the release (after successful validation/publishing)
./gradlew tag --version=$VERSION --release-branch=main
```

**Key characteristics:**

- **Explicit increment instructions**: Commits contain tokens that determine how to bump from the last tag
- **Phases are decoupled**: can validate/publish before tagging
- **Inspectable**: examine calculated version without side effects
- **Policy-enforced**: blocks tagging if branch/state is incorrect

### Axion Workflow

```bash
# Check current version
./gradlew currentVersion
# Output: 0.1.0-SNAPSHOT (if commits exist after last tag)

# Create release (tags immediately)
./gradlew release
# Creates tag v0.1.1, updates version

# Publish artifacts
./gradlew publish
# Uses newly tagged version

# For major/minor bumps (optional)
./gradlew markNextVersion -Prelease.version=1.0.0
```

**Key characteristics:**

- **Tag-based derivation**: reads nearest tag, doesn't parse commits
- **Auto-increment**: automatically bumps patch version
- **Single-command release**: `release` task creates tag immediately
- **Manual major/minor**: use `markNextVersion` to jump versions

## When to Choose Tagger

### 1. Commit-Driven Version Increment Instructions

**The Differentiator:** Developers explicitly declare how to increment the version at commit time using tokens in commit
messages.

**Example:**

```bash
git commit -m "[minor] add user authentication feature"
git commit -m "[patch] fix login redirect bug"
git commit -m "[major] remove deprecated API endpoints"
git commit -m "[none] update documentation"
```

Tagger parses these tokens to determine how to bump from the last tag. If any commit contains `[major]`, tagger
increments the major version. If only `[minor]` commits exist, it increments minor. The actual version number comes from
reading the previous tag and applying the increment.

**Choose tagger if:** You want developers to explicitly signal how to increment the version when they write code, not
when they create releases.

### 2. Two-Phase Workflow for Safety

**The Differentiator:** Separating calculation from tagging enables validation before creating release tags.

**Automated CI Example:**

```bash
# Calculate version first
VERSION=$(./gradlew calculateVersion -q --format=json | jq -r '.data.version')

# Run full validation suite
./gradlew build check integrationTest -Pversion=$VERSION

# Publish artifacts
./gradlew publish -Pversion=$VERSION

# Only tag if everything succeeded
./gradlew tag --version=$VERSION --release-branch=main
```

**Choose tagger if:** You want to validate builds and publish artifacts before creating the release tag, ensuring the
tag only exists if the release succeeded.

### 3. Deterministic Versioning

**The Differentiator:** Given the same Git state (commits, tags, branch), tagger always produces the same version number.

**Why this matters:**

- **Reproducible builds**: Run `calculateVersion` multiple times on the same commit → same result every time
- **CI/local parity**: Developer's local machine and CI see the same version for the same Git state
- **Debugging**: Can reconstruct exactly what version would have been calculated at any point in history

**Example:**

```bash
# On commit abc123, always produces the same result
./gradlew calculateVersion -q
# Output: 1.2.3-SNAPSHOT (every time, on any machine)

# Version only changes when Git state changes (new commits, new tags, branch changes)
```

**Contrast with event-driven approach:** Axion's version depends on when you run `release`, not just what commits exist.
Running `release` three times creates three different versions (0.1.1, 0.1.2, 0.1.3) even with no new commits.

**Choose tagger if:** You value reproducibility and want version calculation to be a pure function of Git state.

### 4. Detailed Snapshot Diagnostics

**The Differentiator:** Explicit reasons why a version is marked as SNAPSHOT.

**Example:**

```bash
./gradlew calculateVersion -q --format=json
```

```json
{
  "status": "success",
  "data": {
    "version": "1.2.3-SNAPSHOT",
    "snapshot": true,
    "snapshotReasons": [
      "DIRTY",
      "NOT_RELEASE_BRANCH"
    ]
  }
}
```

**Six Snapshot Reasons:**

- `FORCED`: `--force-snapshot` flag used
- `DIRTY`: uncommitted changes exist
- `AHEAD`: local commits not pushed to remote
- `BEHIND`: remote has commits not pulled locally
- `NOT_RELEASE_BRANCH`: current branch is not the release branch
- `NO_NEW_VERSION`: no version-affecting commits since last tag

**Choose tagger if:** You need transparent diagnostics for why a build is/isn't a release candidate, helpful for
debugging CI version issues.

### 5. Strict Policy Enforcement

**The Differentiator:** Explicit guardrails against common Git tagging mistakes.

**What Tagger Enforces:**

- ✓ Annotated tags only (rejects lightweight tags with remediation commands)
- ✓ Full Git history required (catches shallow clone issues)
- ✓ Blocks detached HEAD by default (opt-in override available)
- ✓ Prevents tagging snapshots
- ✓ Prevents tagging already-tagged commits
- ✓ Warning system with strict mode (`warningsAsErrors`)

**Example Error:**

```
Error: found 1 tag(s) (1.0.0) but it is lightweight.

Remediation:
  git tag -d 1.0.0
  git tag -a 1.0.0 abc123 -m "1.0.0"
  git push --force origin 1.0.0
```

**Choose tagger if:** You want explicit policy enforcement at the tool level with clear, actionable error messages.

### 6. Flexible Commit Message Patterns

**The Differentiator:** Regex-based patterns adapt to your existing commit conventions.

**Default: bracket-style tokens**

```json
{
  "majorRegex": "\\[major\\]",
  "minorRegex": "\\[minor\\]|\\[feature\\]",
  "patchRegex": "\\[patch\\]|\\[fix\\]",
  "noneRegex": "\\[none\\]"
}
```

**Custom: Conventional Commits style**

```json
{
  "majorRegex": "BREAKING CHANGE:",
  "minorRegex": "feat:",
  "patchRegex": "fix:"
}
```

**Choose tagger if:** You have existing commit conventions or want to customize how increment instructions are signaled.

### 7. Platform Neutrality Beyond Gradle

**The Differentiator:** Tagger includes both a standalone CLI and Gradle plugin, enabling multi-language/multi-platform
repos.

**Multi-Language Monorepo Example:**

```bash
VERSION=$(tagger calculate-version --format=json | jq -r '.data.version')

# Java library (Gradle)
cd java-lib && ./gradlew publish -Pversion=$VERSION

# Python package
cd python-lib && poetry version $VERSION && poetry publish

# Go binary
cd go-app && goreleaser release --version=$VERSION

# Tag once for the whole repo
tagger tag --version $VERSION --release-branch main
```

**Choose tagger if:** You need a single versioning tool across multiple languages or want the option to use versioning
outside Gradle.

## When to Choose Axion

### 1. Tag-Based Version Derivation

**The Differentiator:** Version comes from the nearest Git tag, no commit message parsing required.

**How it works:**

- If current commit is tagged → release version (e.g., `v0.1.0`)
- If commits exist after last tag → snapshot version (e.g., `v0.1.1-SNAPSHOT`)
- If no tags exist → default version (e.g., `0.1.0-SNAPSHOT`)

**Choose Axion if:** You're comfortable with automatic patch increments on each release, or manually specifying major/minor bumps via `markNextVersion`, without parsing commit messages for increment instructions.

### 2. Simpler Single-Command Workflow

**The Differentiator:** The `release` task creates the Git tag immediately—no separate tagging step.

**Example:**

```bash
./gradlew release  # Creates tag, updates version
./gradlew publish  # Publishes with tagged version
```

**Choose Axion if:** You want a streamlined workflow where releasing and tagging happen atomically.

### 3. No Commit Convention Required

**The Differentiator:** Since increment decisions are automatic (patch) or manual (`markNextVersion`), developers don't
need to annotate commit messages with increment instructions.

**Choose Axion if:** You don't want to train developers on commit message conventions or enforce increment signaling in
commits.

### 4. Less Prescriptive Git Workflow

**The Differentiator:** Axion works in most Git states without strict branch/history requirements.

**Choose Axion if:** You want flexibility in your Git workflow without policy constraints on branches or tag formats.

### 5. Minimal Configuration for Basic Use Cases

**The Differentiator:** Works out of the box with minimal setup—just apply the plugin and use `scmVersion.version`.

**Basic Setup:**

```gradle
plugins {
    id 'pl.allegro.tech.build.axion-release' version '1.21.1'
}

project.version = scmVersion.version
```

**Choose Axion if:** You want minimal configuration to get started and don't need extensive customization.

## Migration Considerations

### From Axion to Tagger

**What you gain:**

- Explicit commit-driven increment instructions
- Two-phase workflow with pre-tag validation
- Detailed snapshot diagnostics
- Strict policy enforcement
- Platform neutrality (CLI available)

**What you'll need to adapt:**

- Adopt commit message convention (`[major]`, `[minor]`, `[patch]`)
- Change workflow to calculate → validate → tag pattern
- Configure `.tagger` file and adjust CI scripts

**Migration path:**

1. Add `.tagger` configuration file to repository root
2. Train team on commit message tokens
3. Update CI pipeline:
   ```bash
   VERSION=$(./gradlew calculateVersion -q)
   ./gradlew build test -Pversion=$VERSION
   ./gradlew tag --version=$VERSION --release-branch=main
   ```

### From Tagger to Axion

**What you gain:**

- Simpler workflow (single `release` command)
- No commit convention required
- Less prescriptive Git requirements
- Gradle-native DSL configuration

**What you'll need to adapt:**

- Increment decisions move from commits to automatic patch bumps or manual `markNextVersion` calls
- Single-phase workflow (tag created immediately)
- Less diagnostic visibility for snapshot reasons

**Migration path:**

1. Add Axion plugin to `build.gradle`
2. Replace `calculateVersion`/`tag` workflow with `release` task
3. Use `markNextVersion` for major/minor version jumps
4. Remove commit message token requirements

## Summary

Both tools eliminate hardcoded versions and derive version numbers from Git tags. Choose based on your workflow
preferences:

- **Tagger**: Commit messages contain increment instructions that determine how to bump from the last tag. Two-phase
  workflow enables validation before tagging. Strict policy enforcement and detailed diagnostics.
- **Axion**: Automatic patch increment on each release, or manual major/minor specification. Single-command release
  workflow. Less prescriptive about Git workflow and commit conventions.

Neither is universally better—they solve the same problem with different philosophies about where increment decisions
should live (commit content vs. automatic/manual).

## Further Reading

- [Why Tagger?](why-tagger.md) — Tagger principles and philosophy
- [Tagger CLI README](../command-line-tools/tagger-cli/README.md) — CLI usage and configuration
- [Tagger Plugin README](../tools/tagger-plugin/README.md) — Gradle plugin usage
- [Tagger vs Semantic-Release](tagger-vs-semantic-release.md) — Comparison with full-lifecycle release orchestration
- [Axion Release Plugin Documentation](https://axion-release-plugin.readthedocs.io/) — Official Axion docs
- [Axion Release Plugin GitHub](https://github.com/allegro/axion-release-plugin) — Source repository
