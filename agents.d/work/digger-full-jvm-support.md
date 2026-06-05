# Full JVM Support for digger-cli

## Goal
Enable standalone JVM distribution support for digger-cli with proper distribution archives and JAR execution.

## Constraints
- Must configure application plugin with correct mainClass (`com.zegreatrob.tools.digger.cli.MainKt`)
- Distribution archives must include all dependencies and work standalone
- Must support direct JAR execution (`java -jar`) with proper manifest
- Script name must be `digger` (not `digger-cli-jvm`) for consistency with JS distribution
- Must maintain compatibility with existing Gradle build infrastructure
- Follow patterns established in tagger-cli JVM distribution implementation
- Semver intent: `[minor]` - adds new JVM distribution capability

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Configure application plugin in digger-cli/build.gradle.kts
  - Add application plugin to plugins block
  - Set mainClass to `com.zegreatrob.tools.digger.cli.MainKt`
  - Configure application name as "digger"
  - Verify with `./gradlew :command-line-tools:digger-cli:installJvmDist`
  - Update plan if constraints discovered
- [ ] Configure distribution tasks
  - Ensure jvmDistZip and jvmDistTar tasks are available
  - Verify distribution creates bin/digger and bin/digger.bat scripts
  - Test that lib/ folder includes all dependencies
  - Update plan if constraints discovered
- [ ] Configure JAR manifest for standalone execution
  - Ensure Main-Class attribute is set in JAR manifest
  - Verify `java -jar digger-cli-jvm.jar --version` works
  - Update plan if constraints discovered
- [ ] Test distribution archive structure
  - Run `./gradlew :command-line-tools:digger-cli:jvmDistZip`
  - Extract and verify archive contains bin/digger and lib/ with all dependencies
  - Test extracted distribution executes correctly (version, help, basic commands)
  - Update plan if constraints discovered
- [ ] Add JVM distribution validation to check task
  - Create test similar to tagger-cli's `confirmTaggerCanRun` for JVM
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
- **Commit SHA**: 027731fe
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Ready to start implementation
- **Date**: 2026-06-05

## Implementation Notes
_(newest first)_

### 2026-06-05: Work card created
digger-cli currently only has JS distribution support (npm package). The JVM target is configured but lacks:
- Application plugin configuration
- mainClass configuration
- Distribution tasks (installJvmDist, jvmDistZip, jvmDistTar)
- JAR manifest with Main-Class

Current state:
- Main entry point exists at correct location (src/commonMain/kotlin/com/zegreatrob/tools/digger/cli/Main.kt)
- JS distribution is fully functional with npm publishing
- JVM compilation works but no distribution mechanism exists
- Package is: `com.zegreatrob.tools.digger.cli`

This work will:
1. Enable standalone JVM distribution for users who prefer JVM over Node.js
2. Mirror the tagger-cli JVM distribution pattern
3. Enable potential SDKMAN! distribution (separate work card)
4. Enable Maven Central binary distribution (future consideration)

## Validation
Commands to run before marking complete:
- [ ] `./gradlew :command-line-tools:digger-cli:installJvmDist -q --console=plain` succeeds
- [ ] `command-line-tools/digger-cli/build/install/digger-cli-jvm/bin/digger --version` outputs version correctly
- [ ] `./gradlew :command-line-tools:digger-cli:jvmDistZip -q --console=plain` creates valid archive
- [ ] Extracted zip contains bin/digger executable with correct permissions
- [ ] `./gradlew check -q --console=plain` passes
- [ ] Work card moved to work_completed/
