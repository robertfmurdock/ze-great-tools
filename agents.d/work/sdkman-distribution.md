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
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Configure tagger-cli Gradle application plugin
  - Apply application plugin explicitly
  - Set mainClass to tagger entry point
  - Configure distZip/distTar tasks for SDKMAN-compatible archives
  - Verify archive structure includes bin/ scripts and dependencies
  - Update plan if constraints discovered
- [ ] Configure digger-cli Gradle application plugin
  - Apply application plugin explicitly
  - Set mainClass to digger entry point
  - Configure distZip/distTar tasks for SDKMAN-compatible archives
  - Verify archive structure includes bin/ scripts and dependencies
  - Update plan if constraints discovered
- [ ] Create SDKMAN vendor configuration
  - Prepare candidate submission for tagger-cli
  - Prepare candidate submission for digger-cli
  - Document release artifact URLs (GitHub Releases or Maven Central)
  - Include SHA-256 checksums generation in build
  - Update plan if constraints discovered
- [ ] Submit to SDKMAN candidates repository
  - Fork sdkman/sdkman-candidates on GitHub
  - Create candidate descriptor for tagger-cli
  - Create candidate descriptor for digger-cli
  - Submit PR with both tools
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
