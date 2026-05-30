package com.zegreatrob.tools.tagger

import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal object ConfigFileFunctionalTestSupport {
    private fun file(projectDir: String, name: String) = File(projectDir, name)

    fun addFileNames(): Set<String> = setOf("build.gradle.kts", "settings.gradle", ".gitignore", ".tagger")

    fun setupConfigFileBuild(projectDir: String) {
        file(projectDir, "settings.gradle").writeText("""includeBuild("${System.getProperty("user.dir")}/../../tools")""")
        file(projectDir, ".gitignore").writeText(".gradle")
        file(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            """.trimIndent(),
        )
    }

    fun writeTaggerFile(projectDir: String, entries: List<String>) {
        file(projectDir, ".tagger").writeText(
            """
            {
              ${entries.joinToString(",\n  ")}
            }
            """.trimIndent(),
        )
    }

    fun gradleOutput(projectDir: String, vararg arguments: String): Result<String> = runCatching {
        GradleRunner.create()
            .forwardOutput()
            .withArguments(*arguments)
            .withProjectDir(File(projectDir))
            .build()
            .output
    }

    fun parseCalculateVersion(output: String): TestResult.Success {
        val lines = output.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        val version = lines.firstOrNull().orEmpty()
        val (warningLines, detailLines) = lines.drop(1).partition { it.startsWith("⚠️") }
        return TestResult.Success(version, detailLines.joinToString("\n"), warningLines)
    }

    fun quoted(name: String, value: String?) = value?.let { "\"$name\": \"$it\"" }

    fun bool(name: String, value: Boolean?) = value?.let { "\"$name\": $it" }

    fun escaped(name: String, value: String?) = value?.replace("\\", "\\\\")?.let { "\"$name\": \"$it\"" }
}
