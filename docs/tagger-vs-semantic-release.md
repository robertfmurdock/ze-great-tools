# Tagger vs Semantic-Release: Choosing the Right Tool

Both tagger and semantic-release solve the same fundamental problem: making software releases boring, mechanical, and unsentimental. They share core values but differ significantly in scope and integration approach. This guide helps you choose the right tool for your context.

## Philosophical Alignment

**What Both Tools Believe:**

- **Unromantic, unsentimental releases**: Version numbers are mechanical consequences of code changes, not special events requiring ceremony
- **Deterministic versioning**: Given the same Git history, always produce the same version—no human judgment, no manual bumping
- **Commit messages as version signals**: Developers declare version impact at commit time, not release time
- **Automation over ritual**: Remove manual version decision-making from the release process
- **Git as source of truth**: Version history lives in the VCS, not in external systems

**Where They Diverge:**

- **Tagger**: Narrowly-scoped, scriptable tool. Does exactly two things (calculate versions, create tags) and lets you compose it with your existing build tooling. Stays unopinionated about what happens between calculation and release. Avoids platform lock-in.

- **Semantic-release**: Full-lifecycle release orchestrator. Handles version calculation, tagging, changelog generation, artifact publishing, and team notifications in a single atomic pipeline. Opinionated defaults with extensive plugin ecosystem.

**In other words:** Both tools make releases boring and mechanical. Tagger does less and lets you orchestrate. Semantic-release does more and provides an integrated solution.

## Quick Decision Guide

### Choose Tagger if:

1. You need **platform/technology neutrality** (JVM, Python, Go, mixed stacks)
2. You want **fine-grained control** over release pipeline sequencing
3. You need to **integrate with existing build tooling** (Gradle, Maven, custom scripts)
4. You already have **established tooling** for changelogs, publishing, and notifications

### Choose Semantic-Release if:

1. Your stack is **Node/JavaScript-centric** with npm publishing
2. You want **integrated release orchestration** (version + changelog + publish + notify in one step)
3. You follow **Conventional Commits** and want automatic release notes
4. You need **multi-platform publishing** via plugins (npm + GitHub + Slack + Docker + etc.)

## Feature Comparison

| Aspect | Tagger | Semantic-Release |
|--------|--------|------------------|
| **Scope** | Version calculation + Git tagging only | Full release lifecycle (version + tag + changelog + publish + notify) |
| **Workflow** | Two-step: `calculate-version` → [your build process] → `tag` | Single atomic pipeline: analyze → tag → publish all at once |
| **Scriptability** | Highly scriptable; shell-friendly output (text/JSON) | Designed to run as complete pipeline step |
| **Build Integration** | Stays out of your build process entirely | Orchestrates build artifacts as part of release |
| **Commit Convention** | Regex-based tokens (default: `[major]`, `[minor]`, `[patch]`, `[none]`) — fully customizable | Conventional Commits (Angular format by default) |
| **Platform Focus** | Platform-neutral (works with any language/ecosystem) | Node/JavaScript-centric with broad plugin support |
| **Gradle Support** | Native Gradle plugin with idiomatic tasks | Requires Node.js runtime |
| **Changelog Generation** | Not included (use your own tools) | Built-in via commit analysis |
| **Artifact Publishing** | Not involved (use native tooling: `gradle publish`, `npm publish`, etc.) | Integrated publishing to npm, GitHub, Docker, etc. via plugins |
| **CI Integration** | Explicit version export to environment variables | Runs as automated pipeline step; handles everything |
| **Configuration** | `.tagger` JSON file or CLI args | `.releaserc`, `release.config.js`, or `package.json` |
| **Extensibility** | Focused core feature set | 50+ community plugins for various platforms |
| **Tag Format** | Git annotated tags exclusively (enforced) | Configurable tag format (default: `v${version}`) |
| **Snapshot Semantics** | Explicit `-SNAPSHOT` suffix with six distinct reasons | N/A (publishes only release versions) |
| **Policy Enforcement** | Strict (annotated tags, full history, branch context required) | Validates release preconditions via plugins |

## Workflow Comparison

### Tagger Workflow

```bash
# Phase 1: Calculate version (read-only, deterministic)
VERSION=$(tagger calculate-version --format=json | jq -r '.data.version')

# Phase 2: YOUR build process (full control)
./gradlew build test -Pversion=$VERSION
npm run build
docker build -t myimage:$VERSION .
./custom-security-scan.sh

# Phase 3: Publish artifacts (using native tooling)
./gradlew publishToMavenCentral -Pversion=$VERSION
npm publish --tag latest
helm package --version $VERSION

# Phase 4: Tag the release (after successful publishing)
tagger tag --version $VERSION --release-branch main

# Phase 5: Post-release actions (your choice of tooling)
gh release create $VERSION --generate-notes
./notify-slack.sh "Released $VERSION"
```

**Key characteristics:**
- **Phases are decoupled**: each step can be automated, manual, or hybrid
- **You control sequencing**: tag before or after publishing based on your policy
- **Tool-agnostic**: use any build/test/publish/notify tooling you want
- **Inspectable**: can examine calculated version without side effects

### Semantic-Release Workflow

```bash
# Single command runs entire pipeline atomically
npx semantic-release
```

**What happens internally:**
1. Verify CI conditions
2. Analyze commits (Conventional Commits format)
3. Calculate next version
4. Generate changelog
5. Create Git tag
6. Prepare release (update package.json, etc.)
7. Publish to npm/GitHub/etc. via plugins
8. Notify team via plugins (Slack, etc.)

**Key characteristics:**
- **Atomic operation**: all-or-nothing execution
- **Plugin-driven**: extend behavior via ecosystem plugins
- **Opinionated**: prescribes structure for version → publish flow
- **Integrated**: handles full release lifecycle

## When to Choose Tagger

### 1. Highly Scriptable and Build-System Agnostic

**The Differentiator:** Tagger does exactly two things (calculate versions, create tags) and stays entirely out of your build process. The gap between `calculate-version` and `tag` is yours to fill with any logic you need.

**Example Use Case:**
```bash
VERSION=$(tagger calculate-version --format=json | jq -r '.data.version')

# Multi-artifact, multi-registry publishing
./gradlew publishToMavenCentral -Pversion=$VERSION
npm publish --tag latest
docker build -t myimage:$VERSION . && docker push
helm package --version $VERSION && helm push

# Custom security gates
./run-security-scan.sh $VERSION || exit 1

# Tag only after everything succeeds
tagger tag --version $VERSION --release-branch main
```

**Choose tagger if:** You have complex or heterogeneous build pipelines and need a versioning tool that stays out of your orchestration.

### 2. JVM/Gradle Ecosystem Fit

**The Differentiator:** Native Gradle plugin with idiomatic task integration. No Node.js runtime required.

**Example Use Case:**
```kotlin
plugins {
    id("com.zegreatrob.tools.tagger") version "X.Y.Z"
}

tagger {
    releaseBranch = "main"
}
```

```bash
./gradlew calculateVersion -q  # Native Gradle task
./gradlew release check -Pversion=$VERSION  # Integrates with Gradle lifecycle
```

**Choose tagger if:** You're using Gradle build systems and want versioning as a first-class Gradle task, not an external CLI dependency.

### 3. Two-Phase Design Enables Flexibility

**The Differentiator:** Separating calculation from tagging enables BOTH automated AND manual workflows. This isn't about forcing review—it's about flexibility and safety.

**Automated CI Example:**
```bash
# Zero human intervention - fully scripted
VERSION=$(tagger calculate-version --format=json | jq -r '.data.version')
./gradlew build test publish -Pversion=$VERSION && \
  tagger tag --version $VERSION --release-branch main
```

**Manual Gate Example:**
```bash
# Calculate version first
VERSION=$(tagger calculate-version)
echo "Proposed version: $VERSION"

# [Human reviews/approves version]

# Proceed with release
./gradlew build -Pversion=$VERSION
tagger tag --version $VERSION --release-branch main
```

**Debugging Example:**
```bash
# Inspect version without side effects
tagger calculate-version --format=json | jq '.data.snapshotReasons'
# ["DIRTY", "AHEAD"]  <- diagnose why it's not a release candidate
```

**Choose tagger if:** You need control over the sequence of events in your release pipeline, whether automated or not.

### 4. Avoids Platform and Technology Lock-In

**The Differentiator:** Works with any language, any artifact registry, any CI system, any Git hosting. No ecosystem dependencies.

**What Tagger Explicitly Avoids:**
- ✗ Changelog generation (use your own tools)
- ✗ Release notes publishing (use GitHub CLI, GitLab API, etc.)
- ✗ Artifact publishing (use native tooling: `gradle publish`, `npm publish`, `poetry publish`, `cargo publish`)
- ✗ Team notifications (use your existing Slack/Discord/email automation)
- ✗ Issue tracker integration (use your own automation)

**Multi-Language Monorepo Example:**
```bash
VERSION=$(tagger calculate-version --format=json | jq -r '.data.version')

# JVM library
cd java-lib && ./gradlew publish -Pversion=$VERSION

# Python package
cd python-lib && poetry version $VERSION && poetry publish

# Go binary
cd go-app && goreleaser release --version=$VERSION

# Tag once for the whole repo
tagger tag --version $VERSION --release-branch main
```

**Choose tagger if:** You want to avoid vendor lock-in, or you already have established tooling for changelogs/publishing/notifications and just need deterministic versioning.

### 5. Flexible Commit Message Patterns

**The Differentiator:** Regex-based patterns adapt to your existing commit conventions. No need to retrain teams on Conventional Commits.

**Example Configurations:**

**Default: bracket-style tokens**
```json
{
  "majorRegex": "\\[major\\]",
  "minorRegex": "\\[minor\\]|\\[feature\\]",
  "patchRegex": "\\[patch\\]|\\[fix\\]",
  "noneRegex": "\\[none\\]"
}
```

**Custom: Jira-style**
```json
{
  "majorRegex": "BREAKING:",
  "minorRegex": "FEATURE-\\d+",
  "patchRegex": "BUGFIX-\\d+"
}
```

**Unified regex with named groups**
```json
{
  "versionRegex": "(?<major>BREAKING:)|(?<minor>feat:)|(?<patch>fix:)|(?<none>docs:)"
}
```

**Choose tagger if:** You have existing commit conventions and don't want to migrate to Conventional Commits format.

### 6. Structured Snapshot Semantics

**The Differentiator:** Explicit `-SNAPSHOT` suffix with transparent diagnostic reasons.

**Example:**
```bash
$ tagger calculate-version --format=json
{
  "status": "success",
  "data": {
    "version": "1.2.3-SNAPSHOT",
    "snapshot": true,
    "snapshotReasons": ["DIRTY", "NOT_RELEASE_BRANCH"]
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

**Choose tagger if:** You're using Maven/Gradle snapshot conventions and need clear visibility into why a version is/isn't a release candidate.

### 7. Lower Cognitive Load for Simple Use Cases

**The Differentiator:** Just two commands (`calculate-version`, `tag`) and minimal configuration (set `releaseBranch`). No plugin ecosystem to learn.

**Minimal Setup:**
```json
{
  "releaseBranch": "main"
}
```

```bash
tagger calculate-version  # that's it
tagger tag --version 1.2.3
```

**Choose tagger if:** You want simple, focused versioning without learning a plugin lifecycle or configuring release orchestration.

### 8. Strict Policy Enforcement

**The Differentiator:** Explicit guardrails against common Git tagging mistakes with actionable diagnostics.

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

## When to Choose Semantic-Release

### Multi-Platform Publishing

If you need to publish to multiple platforms (npm + GitHub releases + Slack notifications + Jira updates) in a single atomic operation, semantic-release's plugin ecosystem is purpose-built for this. Tagger requires you to script each step separately.

**Semantic-Release Example:**
```json
{
  "plugins": [
    "@semantic-release/commit-analyzer",
    "@semantic-release/release-notes-generator",
    "@semantic-release/changelog",
    "@semantic-release/npm",
    "@semantic-release/github",
    "@semantic-release/git",
    "semantic-release-slack-bot"
  ]
}
```

One command publishes everywhere.

### Conventional Commits Already Adopted

If your team already follows Conventional Commits strictly and wants automatic release notes generation from commit messages, semantic-release provides this out of the box.

**Commit Examples:**
```
feat: add user authentication
fix: resolve login redirect bug
BREAKING CHANGE: remove deprecated API endpoints
```

Semantic-release automatically generates categorized changelogs from these.

### Node/JavaScript Ecosystem Integration

For JavaScript/TypeScript projects using npm workspaces, semantic-release has better ecosystem integration, community tooling, and native support for npm-specific workflows (dist-tags, provenance, etc.).

### Integrated Release Orchestration

If you want a single tool to handle version calculation, changelog generation, artifact publishing, and team notifications in one atomic operation, semantic-release's integrated approach is purpose-built for this. Tagger requires you to orchestrate these steps separately.

### Release Notes Automation

Semantic-release automatically generates changelogs and release notes from commit messages. Tagger focuses only on version calculation and tagging—you provide your own changelog tooling.

### Multi-Channel Releases

If you need to maintain multiple release channels (stable, beta, alpha, next) with different versioning streams, semantic-release has built-in branch-based channel support.

## Migration Considerations

### From Semantic-Release to Tagger

**What you gain:**
- Platform neutrality and reduced ecosystem lock-in
- Fine-grained control over release pipeline sequencing
- Explicit two-phase workflow (calculate → tag)

**What you'll need to implement yourself:**
- Changelog generation (use `git-cliff`, `conventional-changelog-cli`, or custom scripts)
- Artifact publishing (use native tooling: `npm publish`, `gradle publish`, etc.)
- Team notifications (use Slack webhooks, GitHub CLI, or custom scripts)

**Migration path:**
1. Add `.tagger` configuration file
2. Replace `npx semantic-release` with:
   ```bash
   VERSION=$(tagger calculate-version --format=json | jq -r '.data.version')
   # your build/publish logic
   tagger tag --version $VERSION --release-branch main
   ```
3. Script changelog and notification steps separately

### From Tagger to Semantic-Release

**What you gain:**
- Integrated release orchestration (one command does everything)
- Automatic changelog generation from commits
- Plugin ecosystem for multi-platform publishing

**What you'll need to adapt:**
- Commit messages must follow Conventional Commits format
- Release pipeline becomes atomic (less granular control over sequencing)
- May need Node.js runtime in environments where it wasn't required before

**Migration path:**
1. Adopt Conventional Commits format for new commits
2. Create `.releaserc` configuration
3. Replace tagger commands with `npx semantic-release`
4. Remove manual changelog/notification scripts (handled by plugins)

## Summary

Both tools make releases mechanical and unsentimental. Choose based on your needs:

- **Tagger**: Focused, scriptable, platform-neutral versioning tool. Integrates with your existing build tooling.
- **Semantic-Release**: Integrated release orchestrator. Handles full lifecycle in one atomic operation.

Neither is universally better—they solve the same problem with different scopes and integration philosophies.

## Further Reading

- [Why Tagger?](why-tagger.md) — Tagger principles and philosophy
- [Tagger CLI README](../command-line-tools/tagger-cli/README.md) — CLI usage and configuration
- [Tagger Plugin README](../tools/tagger-plugin/README.md) — Gradle plugin usage
- [Semantic-Release Documentation](https://semantic-release.gitbook.io/) — Official semantic-release docs
