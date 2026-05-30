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

class TaggerFileConfigFunctionalTest {
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

    private fun setupBuildFiles(includeDslConfig: Boolean = false) {
        File(settingsFile).writeText("""includeBuild("${System.getProperty("user.dir")}/../../tools")""")
        File(ignoreFile).writeText(".gradle")
        File(buildFile).writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            ${
                if (includeDslConfig) {
                    """
                    tagger {
                        releaseBranch = "develop"
                    }
                    """.trimIndent()
                } else {
                    ""
                }
            }
            """.trimIndent(),
        )
    }

    private fun execute(): TestResult {
        val output = ConfigFileFunctionalTestSupport.gradleOutput(projectDir, "calculateVersion", "-q")
        return output.fold(
            onSuccess = ConfigFileFunctionalTestSupport::parseCalculateVersion,
            onFailure = { TestResult.Failure(it.message!!) },
        )
    }

    @Test
    fun readsReleaseBranchFromTaggerFile() = setup(object {
        val taggerConfig = """
            {
              "releaseBranch": "master"
            }
        """.trimIndent()
    }) {
        setupBuildFiles(includeDslConfig = false)
        File(taggerFile).writeText(taggerConfig)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            commits = listOf("init", "[patch] commit 1"),
            initialTag = "1.0.0",
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.0.1")
    }

    @Test
    fun dslConfigOverridesTaggerFile() = setup(object {
        val taggerConfig = """
            {
              "releaseBranch": "main"
            }
        """.trimIndent()
    }) {
        setupBuildFiles(includeDslConfig = true)
        File(taggerFile).writeText(taggerConfig)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            commits = listOf("init", "[patch] commit 1"),
            initialTag = "1.0.0",
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.0.1-SNAPSHOT")
    }

    @Test
    fun worksWithoutTaggerFile() = setup(object {
    }) {
        setupBuildFiles(includeDslConfig = true)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore"),
            commits = listOf("init", "[patch] commit 1"),
            initialTag = "1.0.0",
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.0.1-SNAPSHOT")
    }

    @Test
    fun readsImplicitPatchFromTaggerFile() = setup(object {
        val taggerConfigWithImplicitFalse = """
            {
              "releaseBranch": "master",
              "implicitPatch": false
            }
        """.trimIndent()
    }) {
        setupBuildFiles(includeDslConfig = false)
        File(taggerFile).writeText(taggerConfigWithImplicitFalse)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            commits = listOf("init", "commit without semver tag"),
            initialTag = "1.0.0",
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().run {
            message.contains("1.0.0")
                .assertIsEqualTo(true, "Expected version 1.0.0 with implicitPatch=false, got $message")
        }
    }
}
