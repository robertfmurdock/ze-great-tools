# Gradle and Kotlin unstable APIs

Review these uses when upgrading Gradle or Kotlin.

- `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/jvm.gradle.kts` and `mp.gradle.kts` opt into `ExperimentalKotlinGradlePluginApi` for typed compiler options. Recheck the annotation and compiler-options API on every Kotlin upgrade.
- `tools/fingerprint-plugin/build.gradle.kts` suppresses `UnstableApiUsage` because the Gradle plugin-development and testing APIs expose unstable types. Re-run the plugin's unit and functional tests on every Gradle upgrade and remove the suppression when the APIs stabilize.
- `tools-plugins/src/main/kotlin/com/zegreatrob/tools/plugins/NpmCliPlugin.kt` uses explicit Kotlin/JS task names to preserve CLI packaging behavior. Recheck task names whenever Kotlin changes its JS target task model.
