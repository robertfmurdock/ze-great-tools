# JVM CLI Distribution Options Research

## Goal
Research and evaluate distribution options for tagger-cli and digger-cli as standalone JVM executables.

## Constraints
- Must provide user-friendly installation experience (no "download JAR and run java -jar")
- Should minimize JVM installation burden on end users
- Must work across Linux, macOS, Windows
- Should integrate with existing Gradle build tooling
- Must support both amd64 and arm64 architectures where feasible
- Semver intent: `[none]` - research only, no implementation

## Checklist
- [ ] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Research option: jlink + jpackage (bundled JVM, native installers)
  - Pros/cons: distribution size, installation UX, platform support
  - Build integration: Gradle plugins available, CI requirements
  - Update plan if constraints discovered
- [ ] Research option: GraalVM native-image
  - Pros/cons: startup time, binary size, build complexity, reflection/dynamic features
  - Build integration: Gradle native-image plugin, CI build time
  - Update plan if constraints discovered
- [ ] Research option: jpackage without jlink (full JVM bundle)
  - Pros/cons: vs jlink approach, distribution size tradeoff
  - Build integration: simplicity vs size
  - Update plan if constraints discovered
- [ ] Research option: Gradle application plugin + wrapper scripts (requires user JVM)
  - Pros/cons: simplest build, but JVM installation burden
  - Distribution: zip/tar with shell/batch launchers
  - Update plan if constraints discovered
- [ ] Research option: Homebrew formula (macOS), apt/rpm packages (Linux), Chocolatey/Scoop (Windows)
  - Pros/cons: native package manager integration vs maintenance burden
  - Can bundle JVM or depend on system JVM
  - Update plan if constraints discovered
- [ ] Survey existing Kotlin CLI tools: how do kotlinc, ktlint, detekt, etc. distribute?
  - Identify common patterns in Kotlin ecosystem
  - Update plan if constraints discovered
- [ ] Document findings: comparison matrix (UX, size, build complexity, platform coverage, maintenance)
  - Include recommendation with rationale
  - Update plan if constraints discovered
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: 7e361e4e
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Ready to start research
- **Date**: 2026-06-05

## Implementation Notes
_(newest first)_

### 2026-06-05: Work card created
Research work card for evaluating JVM distribution strategies for tagger-cli and digger-cli. No code changes planned - output will be documented findings in Implementation Notes section with recommendation.

Current distribution: JAR artifacts published to Maven Central, users must have JVM installed and run via `java -jar` or Gradle execution.

## Validation
Commands to run before marking complete:
- [ ] Work card moved to work_completed/ with findings documented in Implementation Notes
