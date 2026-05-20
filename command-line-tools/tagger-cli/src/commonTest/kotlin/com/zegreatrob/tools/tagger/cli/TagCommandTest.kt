package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.tagger.TagTestSpec
import com.zegreatrob.tools.tagger.TestResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertNotNull

class TagCommandTest : TagTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var baseArguments: List<String>

    override fun configureWithDefaults() {
        baseArguments = listOf(
            "-q",
            "tag",
            "--release-branch=master",
            projectDir,
        )
    }

    override fun configureWithOverrides(
        releaseBranch: String?,
        userName: String?,
        userEmail: String?,
        warningsAsErrors: Boolean?,
    ) {
        baseArguments = listOf("-q", "tag") +
            listOfNotNull(
                releaseBranch?.let { "--release-branch=$it" },
                userName?.let { "--user-name=$it" },
                userEmail?.let { "--user-email=$it" },
                warningsAsErrors?.let { "--warnings-as-errors=$it" },
            ) +
            listOf(projectDir)
    }

    override fun execute(version: String): TestResult {
        val test = cli()
            .test(baseArguments + "--version=$version")
        return if (test.statusCode == 0) {
            test
                .output
                .trim()
                .let { TestResult.Success(it) }
        } else {
            TestResult.Failure(test.output.trim())
        }
    }

    @Test
    fun withFormatJsonOutputsValidJson() = setup(object {
        val commits = listOf("init", "[patch] commit 1", "[patch] commit 2")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "tag",
            "--release-branch=master",
            "--format=json",
            "--user-name=Test User",
            "--user-email=test@example.com",
            "--version=1.2.4",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0, "Command failed. Stdout: ${result.stdout}\nStderr: ${result.stderr}\nOutput: ${result.output}")

        val json = Json.parseToJsonElement(result.stdout)
        json.jsonObject["status"]?.jsonPrimitive?.content.assertIsEqualTo("success", "Full output: ${result.stdout}")

        val data = json.jsonObject["data"]?.jsonObject ?: error("Expected data object in JSON. Output: ${result.stdout}")
        data["tag"]?.jsonPrimitive?.content.assertIsEqualTo("1.2.4")
    }

    @Test
    fun whenVersionIsOmittedAutoCalculatesFromHistory() = setup(object {
        val commits = listOf("init", "[patch] commit 1", "[patch] commit 2")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "tag",
            "--release-branch=master",
            "--user-name=Test User",
            "--user-email=test@example.com",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode
            .assertIsEqualTo(0, "Command failed. Stdout: ${result.stdout}\nStderr: ${result.stderr}\nOutput: ${result.output}")
        result.stdout
            .trim()
            .assertIsEqualTo("Success!")
        val gitAdapter = com.zegreatrob.tools.adapter.git.GitAdapter(projectDir)
        gitAdapter.showTag("HEAD")
            ?.name
            .assertIsEqualTo("1.2.4")
    }
}
