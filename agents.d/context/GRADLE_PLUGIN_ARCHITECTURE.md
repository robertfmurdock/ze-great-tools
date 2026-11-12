---
load_when: designing, adding, moving, or publishing a Gradle plugin in this repository
cost: ~800 tokens
brief: plugin types, commit-triggered release wiring, stable coordinates, and publication checks
---

# Gradle Plugin Architecture

Plugins here have different consumers and publication requirements. Choose the plugin type and build boundary for its actual use; do not force every plugin into one convention.

## Choose the right shape

- `tools/` contains the Digger, Fingerprint, Tagger, and Certifier public build plugins. They publish to the Gradle Plugin Portal and Maven Central.
- `tools-plugins/` contains internal precompiled convention plugins and the public Dashboard Test Summary project plugin. Internal build tooling and public Portal plugins in this build do not all have the same publication targets.
- A project plugin is applied after settings evaluation. Use a settings plugin only when settings-time behavior is required; its lifecycle and classpath requirements differ.

## Release on the commit workflow

The root build workflow runs on repository commits and calculates a version for each run. Every public plugin must be included in the root release when that commit produces a stable version. Snapshot commits still build and run checks, but must not attempt Plugin Portal publication because the Portal rejects `SNAPSHOT` versions.

- Give each included build with public Portal plugins a build-level `release` task that discovers its `publishPlugins` tasks. Wire that aggregate into the root stable release. Do not add a one-off project task path to the root for each new plugin.
- Keep Portal publication (`publishPlugins`) distinct from Maven Central publication (`publish` and Sonatype staging). Wire every required channel; success in one does not establish success in another.
- Verify the root release task graph for both stable and snapshot versions. A dry run proves task selection only, not remote publication.

## Keep public coordinates stable

- `gradlePlugin { plugins { ... } }` declares plugin IDs and implementation classes. Gradle publishes a marker artifact for each ID plus the implementation artifact. The marker POM must point to the intended implementation group, artifact, and version.
- Maven group is a project property. Setting `group` on an included-build root does not automatically set it on child projects. Set it on each publishing project or explicitly propagate it, then inspect that project's generated implementation POM and marker POM.
- Treat plugin ID and Maven group as stable public coordinates. Changing either can trigger Plugin Portal review. If a successful upload is not visible, check pending review notices and compare the attempted coordinates with the last visible version.
- The root release passes `-Pversion` to included builds. Confirm the version on the actual plugin project and in its generated POM; do not infer it from the root project alone.

## Before considering a plugin release complete

1. Confirm a stable commit's root release graph includes the plugin's `publishPlugins` task and any required Maven publication tasks.
2. Generate and inspect the implementation and marker POMs at the same intended version. Check group propagation as well as version propagation.
3. Inspect the actual CI release step and its outcome.
4. Verify the version appears in Plugin Portal metadata and that the marker POM resolves to the expected implementation coordinates. A green task or CI run alone is not proof of public availability.

Do not publish or backfill versions as a validation step. For the Dashboard incident timeline and evidence, see [the completed work card](../work_completed/make-plugin-releases-consistent.md). For existing Sonatype convention-plugin constraints, see `GRADLE_PLUGIN_CONSTRAINTS.md`; for general Gradle build rules, see `GRADLE_PLAYBOOK.md`.
