package com.zegreatrob.tools.fingerprint

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class FingerprintFunctionalTestBase {

    @TempDir
    lateinit var testProjectDir: File

    protected val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    protected val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    protected fun writeSettings(name: String? = null) {
        settingsFile.writeText(name?.let { """rootProject.name = "$it"""" } ?: "")
    }

    protected fun writeBuild(script: String) {
        buildFile.writeText(script.trimIndent())
    }

    protected fun fileUnderProject(relativePath: String): File =
        testProjectDir.resolve(relativePath).also { it.parentFile?.mkdirs() }

    protected fun writeProjectFile(relativePath: String, content: String): File =
        fileUnderProject(relativePath).also { it.writeText(content.trimIndent()) }

    protected fun gradle(
        projectDir: File = testProjectDir,
        vararg arguments: String,
        forwardOutput: Boolean = false,
        expectFailure: Boolean = false,
    ) = GradleRunner.create()
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
        assertTrue(
            manifestFile.exists(),
            "Fingerprint manifest file should be generated at ${manifestFile.path}",
        )

        val manifest = manifestFile.readText()

        val pluginVersionLine = manifest.lineSequence().firstOrNull { it.startsWith("pluginVersion|") }
        assertTrue(
            pluginVersionLine != null,
            "Manifest must contain a pluginVersion line. Content:\n$manifest",
        )

        val actualVersion = pluginVersionLine
            .substringAfter("pluginVersion|")
            .substringBefore('|')

        assertTrue(
            actualVersion.isNotBlank(),
            "Manifest pluginVersion value must not be blank. Line: $pluginVersionLine",
        )

        if (expectedPluginVersion != null) {
            assertEquals(
                expectedPluginVersion,
                actualVersion,
                "Manifest pluginVersion must match expected. Expected=$expectedPluginVersion Actual=$actualVersion",
            )
        }

        expectedSourcePaths.forEach { path ->
            assertTrue(
                manifest.contains("source|$path|"),
                "Manifest should include source entry for '$path'. Content:\n$manifest",
            )
        }
    }

    protected fun assertManifestContainsDependencyIngredients(manifest: String, context: String) {
        assertTrue(
            manifest.lineSequence().any { it.startsWith("classpath|") },
            "Manifest should include dependency/classpath ingredients ($context). Content:\n$manifest",
        )
    }

    protected fun runFingerprint(dir: File = testProjectDir, vararg extraArgs: String): String {
        gradle(dir, "generateFingerprint", "--configuration-cache", *extraArgs)
        assertFingerprintManifestGeneratedCorrectly(dir)
        return fingerprintFile(dir).readText()
    }

    protected fun assertFingerprintChanged(before: String, after: String, message: String) {
        assert(before != after) { "$message Old: $before, New: $after" }
    }

    protected fun assertFingerprintUnchanged(before: String, after: String, message: String) {
        assert(before == after) { "$message Old: $before, New: $after" }
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
