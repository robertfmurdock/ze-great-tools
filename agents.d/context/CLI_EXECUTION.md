# CLI Execution

## Modules
- Tagger: `:command-line-tools:tagger-cli`
- Digger: `:command-line-tools:digger-cli`
- Platform: JS/Node.js (JVM exists for testing, not productized)

## Primary: npx execution (recommended)

Build:
```bash
./gradlew :command-line-tools:tagger-cli:jsProductionExecutableCompileSync -q --console=plain
./gradlew :command-line-tools:digger-cli:jsProductionExecutableCompileSync -q --console=plain
```
- Output: `command-line-tools/build/js/packages/command-line-tools-{cli}/`

Execute:
```bash
npx --package=command-line-tools/build/js/packages/command-line-tools-tagger-cli tagger <args>
npx --package=command-line-tools/build/js/packages/command-line-tools-digger-cli digger <args>
```
- Accepts arbitrary arguments
- Works from any directory (adjust package path as needed)
- No global install required
- Requires node/npm in PATH
- npm local package warning is harmless

## Alternative: npm link (global install)

```bash
./gradlew :command-line-tools:tagger-cli:jsLink -q --console=plain
./gradlew :command-line-tools:digger-cli:jsLink -q --console=plain
```
- Creates symlinks in global node_modules
- Execute: `tagger <args>` / `digger <args>` from anywhere
- Downsides: namespace pollution, stale symlinks on clean, permission issues, not project-scoped
- Use: interactive work with short commands

## Smoke test (no arguments)

```bash
./gradlew :command-line-tools:tagger-cli:jsNodeProductionRun -q --console=plain
./gradlew :command-line-tools:digger-cli:jsNodeProductionRun -q --console=plain
```
- Builds + runs (displays help only)
- No argument passing
- Use: quick verification

Tagger CI check:
```bash
./gradlew :command-line-tools:tagger-cli:confirmTaggerCanRun -q --console=plain
```
- Runs `tagger calculate-version` with current git state
- Part of `check` task

## CLI Commands

**Tagger:**
```
tagger [--version] <command>
  calculate-version [--format=json] [<git-repo>]  # next semver from git history
  tag --version <VERSION> [<git-repo>]            # create + push annotated tag
  guide                                           # fit assessment + workflow
  generate-settings-file                          # create .tagger.yml template
```

**Digger:**
```
digger [--version] <command>
  current-contribution-data [<options>] <git-repo>  # data since latest tag → currentContributionData.json
  all-contribution-data [<options>] <git-repo>      # data across all tags → allContributionData.json
  guide                                             # fit assessment + workflow
```

## Build artifacts

Source:
- Bin scripts: `src/jsMain/resources/bin/{tagger|digger}`
- Entry: `src/commonMain/kotlin/.../Main.kt` with `fun main(args: Array<String>)`

Output (after `assemble` or `jsNodeProductionRun`):
- Processed resources: `build/processedResources/js/main/bin/{cli}`
- Compiled JS: `build/compileSync/js/main/productionExecutable/kotlin/*.js`
- npm package: `jsCliTar` → `build/distributions/{cli}-js.tgz`

## Environment

Working directory:
- Must be project root for Gradle execution
- CLIs require git repository context
- Run from `/path/to/ze-great-tools`

Git state:
- Valid git repo required
- Reads: git history, tags, commits
- `tagger tag` writes: requires clean working tree + release branch

Environment variables: None required

## Diagnostics

"DIRTY - Uncommitted changes" / "NOT_RELEASE_BRANCH":
- Normal tagger output during version calculation (not errors)
- Expected diagnostic behavior

Rebuild requirement:
- Source changes require rebuild
- `jsNodeProductionRun` auto-compiles
- Manual: `./gradlew :command-line-tools:{cli}:assemble -q --console=plain`
