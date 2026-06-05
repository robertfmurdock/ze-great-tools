# Full JVM Support for tagger-cli

## Goal
Fix and complete JVM distribution support for tagger-cli to enable standalone JAR execution and proper distribution archives.

## Constraints
- Must correct mainClass configuration pointing to wrong package
- Distribution archives must include all dependencies and work standalone
- Must support direct JAR execution (`java -jar`) with proper manifest
- Script name must be `tagger` (not `tagger-cli-jvm`) for consistency with JS distribution
- Must maintain compatibility with existing Gradle build infrastructure
- Semver intent: `[patch]` - fixes broken JVM binary configuration

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Fix mainClass configuration in tagger-cli/build.gradle.kts
  - Change from `com.zegreatrob.coupling.cli.MainKt` to `com.zegreatrob.tools.tagger.cli.MainKt`
  - Verify with `./gradlew :command-line-tools:tagger-cli:installJvmDist`
  - Test that `bin/tagger --version` works correctly
  - Update plan if constraints discovered
- [ ] Configure JAR manifest for standalone execution
  - Ensure Main-Class attribute is set in JAR manifest
  - Verify `java -jar tagger-cli-jvm.jar --version` works
  - Update plan if constraints discovered
- [ ] Verify distribution archive structure
  - Run `./gradlew :command-line-tools:tagger-cli:jvmDistZip`
  - Extract and verify archive contains bin/tagger and lib/ with all dependencies
  - Test extracted distribution executes correctly
  - Update plan if constraints discovered
- [ ] Add JVM distribution validation to check task
  - Create test similar to `confirmTaggerCanRun` for JVM
  - Verify JVM distribution executes basic command
  - Ensure check task depends on JVM validation
  - Update plan if constraints discovered
- [ ] Document JVM distribution usage
  - Add instructions for running JVM version
  - Document distribution archive format
  - Include example for direct JAR execution if supported
  - Update plan if constraints discovered
- [ ] Final refactor: MANDATORY subagent (REFACTOR_AGENT.md), reviews ALL commits/files
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: 8ce9f3cb
- **Uncommitted work**: IDE config changes only
- **Blockers**: None
- **Status**: Ready to start implementation
- **Date**: 2026-06-05

## Implementation Notes
_(newest first)_

### 2026-06-05: Work card created
Investigation shows that tagger-cli already has JVM target configured with distribution tasks (installJvmDist, jvmDistZip, jvmDistTar) but has critical configuration error:
- mainClass is set to wrong package: `com.zegreatrob.coupling.cli.MainKt`
- Should be: `com.zegreatrob.tools.tagger.cli.MainKt`
- This causes "Could not find or load main class" error when running distribution

Current state:
- Distribution builds successfully and creates bin/tagger script
- Script structure is correct (bin/ folder with tagger and tagger.bat)
- lib/ folder includes all dependencies
- Main entry point exists at correct location (src/commonMain/kotlin/com/zegreatrob/tools/tagger/cli/Main.kt)
- Only blocker is incorrect mainClass configuration

This is a straightforward fix that will enable:
1. Standalone JVM distribution for users who prefer JVM over Node.js
2. Potential SDKMAN! distribution (separate work card)
3. Maven Central binary distribution (future consideration)

## Validation
Commands to run before marking complete:
- [ ] `./gradlew :command-line-tools:tagger-cli:installJvmDist -q --console=plain` succeeds
- [ ] `command-line-tools/tagger-cli/build/install/tagger-cli-jvm/bin/tagger --version` outputs version correctly
- [ ] `./gradlew :command-line-tools:tagger-cli:jvmDistZip -q --console=plain` creates valid archive
- [ ] Extracted zip contains bin/tagger executable with correct permissions
- [ ] `./gradlew check -q --console=plain` passes
- [ ] Work card moved to work_completed/
