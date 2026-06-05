# SDKMAN! Distribution for tagger-cli and digger-cli

## Goal
Enable SDKMAN! distribution for tagger-cli and digger-cli JVM versions to reach JVM ecosystem developers.

## Constraints
- Must work with existing Gradle application plugin infrastructure
- Distribution archives must include all dependencies (fat JAR or lib/ folder)
- Must support versioned installation and `sdk upgrade` workflow
- Should leverage existing Maven Central or GitHub Releases as artifact source
- Must provide installation documentation for users
- Semver intent: `[minor]` - new backward-compatible distribution channel

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [x] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [x] Configure tagger-cli Gradle application plugin
  - Apply application plugin explicitly
  - Set mainClass to tagger entry point
  - Configure distZip/distTar tasks for SDKMAN-compatible archives
  - Verify archive structure includes bin/ scripts and dependencies
  - Update plan if constraints discovered
- [x] Configure digger-cli Gradle application plugin
  - Apply application plugin explicitly
  - Set mainClass to digger entry point
  - Configure distZip/distTar tasks for SDKMAN-compatible archives
  - Verify archive structure includes bin/ scripts and dependencies
  - Update plan if constraints discovered
- [x] Add io.sdkman.vendors plugin to build
  - Add plugin to version catalog
  - Apply plugin to tagger-cli module
  - Apply plugin to digger-cli module
  - Configure plugin with candidate names and distribution URLs
  - Update plan if constraints discovered
- [x] Configure SHA-256 checksum generation
  - Add checksum task for tagger-cli jvmDistZip
  - Add checksum task for digger-cli jvmDistZip
  - Verify checksums are generated correctly
  - Update plan if constraints discovered
- [ ] Document SDKMAN installation method
  - Add SDKMAN installation instructions to README or docs
  - Document `sdk install tagger` and `sdk install digger` commands
  - Document version management (`sdk list`, `sdk upgrade`)
  - Update plan if constraints discovered
- [ ] Test SDKMAN installation workflow
  - Verify archive format works with SDKMAN
  - Test installation from candidate URL
  - Verify executable permissions on bin/ scripts
  - Test commands execute correctly
  - Update plan if constraints discovered
- [ ] Update CI/CD for release automation
  - Configure GitHub Actions to build distZip/distTar on release
  - Upload distribution archives as release artifacts
  - Generate and publish SHA-256 checksums
  - Document release process
  - Update plan if constraints discovered
- [ ] Final refactor: MANDATORY subagent (REFACTOR_AGENT.md), reviews ALL commits/files
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: 8ce9f3cb
- **Uncommitted work**: Research work card updates only
- **Blockers**: None
- **Status**: Ready to start implementation
- **Date**: 2026-06-05

## Implementation Notes
_(newest first)_

### 2026-06-05: SHA-256 checksum generation configured
Added `org.gradle.crypto.checksum:1.4.0` plugin and created `jvmDistZipChecksum` tasks for both CLIs. Tasks generate `.sha256` files alongside distribution zips. Checksums verified working correctly (64-character hex strings).

Output files:
- `build/distributions/tagger-cli-jvm.zip.sha256`
- `build/distributions/digger-cli-jvm.zip.sha256`

### 2026-06-05: Plugin applied and configured
Added `io.sdkman.vendors:3.0.0` to version catalog and applied to both CLI modules. Plugin provides tasks:
- `sdkReleaseVersion` - Release new version to SDKMAN
- `sdkAnnounceVersion` - Announce release
- `sdkDefaultVersion` - Set as default version
- `sdkMinorRelease` - Release + announce (convenience)
- `sdkMajorRelease` - Release + announce + default (convenience)

Configuration via Gradle properties or CLI args (plugin reads automatically):
- `sdkman.candidate` - candidate name ("tagger" or "digger")
- `sdkman.version` - version string  
- `sdkman.url` - download URL for distribution zip
- `sdkman.hashtag` - hashtag for announcements
- `SDKMAN_KEY` / `SDKMAN_TOKEN` - credentials (from environment or properties)

No explicit DSL configuration needed - property-based approach cleaner for CI/CD.

### 2026-06-05: Discovered io.sdkman.vendors Gradle plugin
Found official SDKMAN vendor plugin `io.sdkman.vendors` (v3.0.0 latest) on Gradle Plugin Portal. This plugin can handle SDKMAN candidate announcements programmatically, eliminating need for manual PR to sdkman-candidates repository.

Plan adjustment: Use `io.sdkman.vendors` plugin instead of manual candidate submission. This changes the implementation from "fork + PR workflow" to "configure + announce workflow".

For SHA-256 checksums: Gradle provides built-in checksum support via `org.gradle.crypto.checksum` tasks or manual task implementation.

### 2026-06-05: Application plugin already configured
Verified that both tagger-cli and digger-cli already have correct Gradle application plugin configuration via Kotlin Multiplatform's `jvm { binaries { executable } }` DSL:
- `mainClass` already set correctly for both CLIs
- `jvmDistZip` and `jvmDistTar` tasks available and functional
- Archive structure verified SDKMAN-compatible: `bin/` folder with executable scripts + `lib/` folder with dependencies
- Tested extraction and execution: both CLIs work correctly from extracted archives

No subagent authorization requested as final refactor is the only planned subagent usage (REFACTOR_AGENT.md).

### 2026-06-05: Work card created
Implementation work card for SDKMAN! distribution based on jvm-cli-distribution-research findings. SDKMAN! identified as low-effort, high-value Phase 1 distribution channel for JVM developers.

Assumptions:
- JVM versions of tagger-cli and digger-cli are fully functional
- Gradle application plugin output is correct format for SDKMAN (bin/ scripts + dependencies)
- Will use GitHub Releases as artifact source (simpler than Maven Central for binary archives)

Research context: SDKMAN! requires:
1. Candidate submission to sdkman-candidates repository
2. Archive format: zip/tar with bin/ folder containing executable scripts
3. SHA-256 checksums for verification
4. Public URL for artifact download (GitHub Releases recommended)
5. Community approval process (days to weeks)

## Validation
Commands to run before marking complete:
- [ ] `./gradlew :command-line-tools:tagger-cli:distZip` produces valid archive
- [ ] `./gradlew :command-line-tools:digger-cli:distZip` produces valid archive
- [ ] Extracted archive contains bin/tagger or bin/digger executable script
- [ ] Script executes `--version` successfully after extraction
- [ ] `./gradlew check -q --console=plain` passes
- [ ] Work card moved to work_completed/
