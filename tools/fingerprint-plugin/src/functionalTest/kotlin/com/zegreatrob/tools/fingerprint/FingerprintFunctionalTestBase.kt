package com.zegreatrob.tools.fingerprint

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.minassert.assertIsNotEqualTo
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class FingerprintFunctionalTestBase {

    @TempDir
    lateinit var testProjectDir: File

    protected val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    protected val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    protected fun writeSettings(name: String? = null) {
        if (name == null) {
            settingsFile.writeText("")
        } else {
            writeSettings(testProjectDir, name)
        }
    }

    protected fun writeSettings(dir: File, name: String, includeBuildName: String? = null) {
        val includeLine = includeBuildName?.let { "\nincludeBuild(\"../$it\")" }.orEmpty()
        dir.resolve("settings.gradle.kts").writeText("""rootProject.name = "$name"$includeLine""")
    }

    protected fun writeBuild(script: String) {
        buildFile.writeText(script.trimIndent())
    }

    protected fun writeBuildWith(base: String, extra: String) {
        writeBuild(
            """
            $base
            $extra
            """.trimIndent(),
        )
    }

    protected fun writeProject(name: String? = null, buildScript: String) {
        writeSettings(name)
        writeBuild(buildScript)
    }

    protected fun fileUnderProject(relativePath: String): File = testProjectDir.resolve(relativePath).also { it.parentFile?.mkdirs() }

    protected fun writeProjectFile(relativePath: String, content: String): File = fileUnderProject(relativePath).also { it.writeText(content.trimIndent()) }

    protected fun projectDir(name: String): File = testProjectDir.resolve(name).apply { mkdirs() }

    protected fun gradle(
        projectDir: File = testProjectDir,
        vararg arguments: String,
        forwardOutput: Boolean = false,
        expectFailure: Boolean = false,
    ): BuildResult = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*arguments)
        .withPluginClasspath()
        .apply { if (forwardOutput) forwardOutput() }
        .let { runner -> if (expectFailure) runner.buildAndFail() else runner.build() }

    protected fun fingerprintFile(dir: File = testProjectDir) = dir.resolve("build/fingerprint.txt")
    protected fun fingerprintManifestFile(dir: File = testProjectDir) = dir.resolve("build/fingerprint-manifest.log")

    protected fun assertFingerprintManifestGeneratedCorrectly(
        dir: File = testProjectDir,
        expectedPluginVersion: String? = null,
        vararg expectedSourcePaths: String,
    ) {
        val manifestFile = fingerprintManifestFile(dir)
        manifestFile.exists().assertIsEqualTo(
            true,
            "Fingerprint manifest file should be generated at ${manifestFile.path}",
        )

        val manifest = manifestFile.readText()

        val pluginVersionLine = manifest.lineSequence().firstOrNull { it.startsWith("pluginVersion|") }
        pluginVersionLine.assertIsNotEqualTo(
            null,
            "Manifest must contain a pluginVersion line. Content:\n$manifest",
        )

        val actualVersion = pluginVersionLine
            ?.substringAfter("pluginVersion|")
            ?.substringBefore('|')

        actualVersion
            ?.isNotBlank()
            .assertIsEqualTo(
                true,
                "Manifest pluginVersion value must not be blank. Line: $pluginVersionLine",
            )

        if (expectedPluginVersion != null) {
            actualVersion.assertIsEqualTo(
                expectedPluginVersion,
                "Manifest pluginVersion must match expected. Expected=$expectedPluginVersion Actual=$actualVersion",
            )
        }

        expectedSourcePaths.forEach { path ->
            manifest.contains("source|$path|").assertIsEqualTo(
                true,
                "Manifest should include source entry for '$path'. Content:\n$manifest",
            )
        }
    }

    protected fun assertManifestContainsDependencyIngredients(manifest: String, context: String) {
        manifest.lineSequence().any { it.startsWith("classpath|") }.assertIsEqualTo(
            true,
            "Manifest should include dependency/classpath ingredients ($context). Content:\n$manifest",
        )
    }

    protected fun runFingerprint(dir: File = testProjectDir, vararg extraArgs: String): String {
        gradle(dir, "generateFingerprint", "--configuration-cache", *extraArgs)
        return fingerprintFile(dir).readText()
    }

    protected data class FingerprintRun(
        val hash: String,
        val manifest: String,
    )

    protected fun runFingerprintWithManifest(dir: File = testProjectDir, vararg extraArgs: String): FingerprintRun {
        val hash = runFingerprint(dir, *extraArgs)
        val manifest = fingerprintManifestFile(dir).readText()
        return FingerprintRun(hash = hash, manifest = manifest)
    }

    protected fun runFingerprintTwiceAfter(
        change: () -> Unit,
        dir: File = testProjectDir,
        vararg extraArgs: String,
    ): Pair<String, String> {
        val firstHash = runFingerprint(dir, *extraArgs)
        change()
        val secondHash = runFingerprint(dir, *extraArgs)
        return Pair(firstHash, secondHash)
    }

    protected fun assertFingerprintChanged(before: String, after: String, message: String) {
        (before != after).assertIsEqualTo(true, "$message Old: $before, New: $after")
    }

    protected fun assertFingerprintUnchanged(before: String, after: String, message: String) {
        (before == after).assertIsEqualTo(true, "$message Old: $before, New: $after")
    }

    protected fun kmpBuild(
        kotlinBlock: String = "kotlin { jvm() }",
        repositoriesBlock: String = "repositories { mavenCentral() }",
    ) = """
        plugins {
            kotlin("multiplatform") version "2.3.0"
            id("com.zegreatrob.tools.fingerprint")
        }
        $repositoriesBlock
        $kotlinBlock
    """.trimIndent()

    protected fun aggregateFingerprintFile(dir: File) = dir.resolve("build/aggregate-fingerprint.txt")
    protected fun aggregateFingerprintManifestFile(dir: File) = dir.resolve("build/aggregate-fingerprint-manifest.log")
}
