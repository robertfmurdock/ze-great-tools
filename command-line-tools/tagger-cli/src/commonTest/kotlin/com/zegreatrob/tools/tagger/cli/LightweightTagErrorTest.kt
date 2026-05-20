package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.runProcess
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.initializeGitRepo
import kotlin.test.AfterTest
import kotlin.test.Test

class LightweightTagErrorTest {
    private lateinit var projectDir: String

    @AfterTest
    fun cleanup() {
        if (::projectDir.isInitialized) {
            com.zegreatrob.tools.test.git.removeDirectory(projectDir)
        }
    }

    @Test
    fun `calculate-version with lightweight tag shows helpful error message`() = setup(object {
        val lightweightTagName = "1.0.0"
    }) {
        projectDir = createTempDirectory()
        initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test User")
            config("user.email", "test@example.com")
        }
        runProcess(listOf("git", "tag", lightweightTagName), projectDir)
    } exercise {
        cli()
            .test(
                listOf(
                    "calculate-version",
                    "--release-branch=master",
                    projectDir,
                ),
            )
    } verify { result ->
        result.statusCode
            .assertIsEqualTo(1)
        result.output.contains("lightweight")
            .assertIsEqualTo(true)
        result.output.contains(lightweightTagName)
            .assertIsEqualTo(true)
        result.output.contains("git tag -d $lightweightTagName")
            .assertIsEqualTo(true)
        result.output.contains("git tag -a $lightweightTagName")
            .assertIsEqualTo(true)
        result.output.contains("git push --force origin $lightweightTagName")
            .assertIsEqualTo(true)
    }

    @Test
    fun `calculate-version with multiple lightweight tags shows all in error message`() = setup(object {
        val tag1 = "1.0.0"
        val tag2 = "1.1.0"
    }) {
        projectDir = createTempDirectory()
        initializeGitRepo(
            directory = projectDir,
            addFileNames = emptySet(),
            commits = listOf("initial commit"),
        ).apply {
            config("user.name", "Test User")
            config("user.email", "test@example.com")
        }
        runProcess(listOf("git", "tag", tag1), projectDir)
        runProcess(listOf("git", "tag", tag2), projectDir)
    } exercise {
        cli()
            .test(
                listOf(
                    "calculate-version",
                    "--release-branch=master",
                    projectDir,
                ),
            )
    } verify { result ->
        result.statusCode
            .assertIsEqualTo(1)
        result.output.contains("2 tags")
            .assertIsEqualTo(true)
        result.output.contains(tag1)
            .assertIsEqualTo(true)
        result.output.contains(tag2)
            .assertIsEqualTo(true)
        result.output.contains("they are lightweight")
            .assertIsEqualTo(true)
    }
}
