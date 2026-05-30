package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

class ConfigCacheInvalidationTest {
    private lateinit var projectDir: String
    private val buildFile by lazy { "$projectDir/build.gradle.kts" }
    private val settingsFile by lazy { "$projectDir/settings.gradle" }
    private val ignoreFile by lazy { "$projectDir/.gitignore" }
    private val taggerFile by lazy { "$projectDir/.tagger" }
    private val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore", ".tagger")

    @BeforeTest
    fun setUpProjectDir() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun deleteProjectDir() {
        removeDirectory(projectDir)
    }

    @BeforeTest
    fun checkPrerequisites() {
        getEnvironmentVariable("GIT_CONFIG_GLOBAL").assertIsEqualTo(
            "/dev/null",
            "Ensure this is set for the test to work as intended",
        )
        getEnvironmentVariable("GIT_CONFIG_SYSTEM").assertIsEqualTo(
            "/dev/null",
            "Ensure this is set for the test to work as intended",
        )
    }

    private fun setupBuildFiles() {
        File(settingsFile).writeText("""includeBuild("${System.getProperty("user.dir")}/../../tools")""")
        File(ignoreFile).writeText(".gradle")
        File(buildFile).writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            """.trimIndent(),
        )
    }

    private fun executeWithConfigCache(): TestResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withArguments("calculateVersion", "-q", "--configuration-cache")
        runner.withProjectDir(File(projectDir))
        return try {
            val result = runner.build()
            val lines = result.output
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()

            val version = lines.firstOrNull().orEmpty()
            val remainingLines = lines.drop(1)
            val (warningLines, detailLines) = remainingLines.partition { it.startsWith("⚠️") }

            TestResult.Success(version, detailLines.joinToString("\n"), warningLines)
        } catch (e: Exception) {
            TestResult.Failure(e.message!!)
        }
    }

    @Test
    fun invalidatesConfigurationCacheWhenTaggerFileChanges() = setup(object {
        val initialConfig = """
            {
              "releaseBranch": "master",
              "implicitPatch": false
            }
        """.trimIndent()
        val updatedConfig = """
            {
              "releaseBranch": "master",
              "implicitPatch": true
            }
        """.trimIndent()
    }) {
        setupBuildFiles()
        File(taggerFile).writeText(initialConfig)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            commits = listOf("init", "commit without semver tag"),
            initialTag = "1.0.0",
        )
    } exercise {
        val firstResult = executeWithConfigCache()
        File(taggerFile).writeText(updatedConfig)
        val secondResult = executeWithConfigCache()
        firstResult to secondResult
    } verify { (firstResult, secondResult) ->
        println("First result: $firstResult")
        println("Second result: $secondResult")
        when (firstResult) {
            is TestResult.Success -> firstResult.message.contains("1.0.0")
                .assertIsEqualTo(true, "First run with implicitPatch=false should be 1.0.0, got ${firstResult.message}")

            is TestResult.Failure -> fail("First run failed: ${firstResult.reason}")
        }
        when (secondResult) {
            is TestResult.Success -> secondResult.message.contains("1.0.1")
                .assertIsEqualTo(
                    true,
                    "Second run with implicitPatch=true should be 1.0.1 (cache should invalidate), got ${secondResult.message}",
                )

            is TestResult.Failure -> fail("Second run failed: ${secondResult.reason}")
        }
    }
}
