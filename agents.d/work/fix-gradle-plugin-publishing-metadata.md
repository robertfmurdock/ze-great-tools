# Fix Gradle Plugin Publishing Metadata

## Goal
Restore missing Maven Central metadata (POM fields, javadocs) for Gradle plugin publications that was lost when consolidating to convention plugin, ensuring all 4 plugins pass Sonatype validation.

## Constraints
- Must preserve publishing/signing config consolidation benefits (DRY principle)
- All plugin publications must satisfy Maven Central requirements:
  - POM: name, description, url, license, scm, developers
  - Javadoc jars for all artifacts (including -guide-jvm artifacts)
- Semver intent: `[patch]` - restores previously working functionality
- Related commit: 675cb0da9d55bf28ffd3c2311212f79dea65cca9 (reverted by 76207f5c)
- Failure: https://github.com/robertfmurdock/ze-great-tools/actions/runs/26950344485/job/79513699638

## Root Cause Analysis
The reverted commit applied `com.zegreatrob.tools.plugins.publish` convention plugin to all 4 Gradle plugins, removing local pom configuration blocks. The convention plugin configures POM metadata via `afterEvaluate`, but:

**POM metadata not reaching Gradle plugin publications**: The convention plugin sets `pom.name`, `pom.description`, etc. on `publishing.publications.withType<MavenPublication>()`, but Gradle plugin publications may be created after the convention plugin's `afterEvaluate` block runs, or require the Gradle plugin DSL metadata to propagate to publications.

**Missing javadoc jars for `-guide-jvm` artifacts**: The `-guide-jvm` artifacts (e.g., `tagger-guide-jvm`, `digger-guide-jvm`) are library modules published as part of the plugin build, not Gradle plugin marker publications. Sonatype requires javadoc jars, but these modules may not be generating them.

## Checklist
- [x] Review this work card for compliance with template and update to conform
- [ ] If this card plans subagent delegation, ask user to explicitly authorize subagents for this card and record the response in Implementation Notes
- [ ] Investigate how Gradle plugin DSL metadata (displayName, description, website, vcsUrl) propagates to MavenPublication POM fields
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Compare working plugin build.gradle.kts (reverted state) vs. failing state to identify metadata gaps
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Determine why javadoc jars missing for `-guide-jvm` artifacts (check if java plugin applied, javadocJar task wired)
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Fix convention plugin to correctly populate POM metadata for Gradle plugin publications
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Add javadoc jar generation for `-guide-jvm` library modules
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Test fix: build and publish to local Maven repo, verify POM contents and javadoc jar presence
  - Agent cycle: test → implement → refactor-light → verify pushable
  - Update plan if guidelines revealed new constraints
- [ ] Final refactor pass via subagent (MANDATORY - see REFACTOR_AGENT.md)
- [ ] Move this file to agents.d/work_completed/

## Current State
- **Commit SHA**: 76207f5c (revert commit, clean state)
- **Uncommitted work**: None
- **Blockers**: None
- **Status**: Ready to investigate
- **Date**: 2026-06-04

## Implementation Notes
_(newest first)_

### 2026-06-04: Work card created
Semver intent: `[patch]` - restores previously working publishing functionality without behavior changes.

Failed artifacts from CI log:
- `pkg:maven/com.zegreatrob.tools/tagger-guide-jvm@3.0.2` - Javadocs must be provided
- `pkg:maven/com.zegreatrob.tools.digger/com.zegreatrob.tools.digger.gradle.plugin@3.0.2` - Project URL, License, SCM URL, Developers missing
- `pkg:maven/com.zegreatrob.tools/fingerprint-plugin@3.0.2` - Project name, description, URL, License, SCM URL, Developers missing
- `pkg:maven/com.zegreatrob.tools/digger-guide-jvm@3.0.2` - Javadocs must be provided
- `pkg:maven/com.zegreatrob.tools.fingerprint/com.zegreatrob.tools.fingerprint.gradle.plugin@3.0.2` - Project URL, License, SCM URL, Developers missing
- `pkg:maven/com.zegreatrob.tools/digger-plugin@3.0.2` - Project name, description, URL, License, SCM URL, Developers missing

Pattern: Gradle plugin marker publications (*.gradle.plugin) missing POM metadata; `-guide-jvm` library artifacts missing javadoc jars.

## Validation
Commands to run before marking complete:
- [ ] `./gradlew check -q --console=plain` - all checks pass
- [ ] `./gradlew publishToMavenLocal` - publishes without warnings
- [ ] Verify POM files in `~/.m2/repository/com/zegreatrob/tools/` contain all required fields
- [ ] Verify javadoc jars present for all artifacts in local Maven repo
