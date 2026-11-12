# Fix Gradle Plugin Publishing Metadata

**Work card protocol: 2**

## Goal
Restore Maven Central metadata and Javadoc artifacts for Gradle plugin publications that were lost during convention-plugin consolidation, so all four plugins pass Sonatype validation.

## Constraints and acceptance evidence
- Semver intent: `[patch]` — restore previously working publishing functionality.
- Preserve the publishing/signing configuration consolidation benefits and avoid reintroducing unnecessary duplication.
- All plugin publications need POM name, description, URL, license, SCM, and developer metadata. All artifacts, including `-guide-jvm` libraries, need Javadoc jars.
- Determine how Gradle Plugin DSL metadata reaches marker publications and why guide JVM artifacts lack Javadoc jars. Compare the current setup with the working state before proposing changes.
- Validate by publishing to Maven Local and checking every relevant POM field and Javadoc artifact. Follow shared testing and validation guidance; tests for behavior changes must demonstrate the expected failure before implementation.

## Context and evidence
The convention plugin `com.zegreatrob.tools.plugins.publish` was applied to all four plugins in commit `675cb0da9d55bf28ffd3c2311212f79dea65cca9`, then reverted by `76207f5c` after Sonatype validation failed. The [CI failure](https://github.com/robertfmurdock/ze-great-tools/actions/runs/26950344485/job/79513699638) reported missing POM metadata on plugin marker publications and missing Javadocs for `tagger-guide-jvm` and `digger-guide-jvm`.

## Hypotheses to verify
- POM metadata configured by the convention plugin may be applied before Gradle plugin publications exist, or may need to be sourced from Gradle Plugin DSL metadata.
- The `-guide-jvm` library modules may not apply/configure the Java documentation artifact generation required for publication.

## Resume note
Ready to investigate. No implementation work is recorded on this card; use Git for current repository state.
