package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.tagger.TagTestSpec
import com.zegreatrob.tools.tagger.TestResult
import com.zegreatrob.tools.test.git.addCommitWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.fail

class TagCommandTest : TagTestSpec {
    @Test
    fun helpTextEmphasizesCalculateVersionWorkflow() = setup(object {
        val command = cli()
    }) exercise {
        command.test("tag --help")
    } verify { result ->
        result.output.contains("calculate-version").assertIsEqualTo(true, "Help should reference calculate-version")
        result.output.contains("two-step workflow").assertIsEqualTo(true, "Help should mention two-step workflow")
        result.output.contains("manually override").assertIsEqualTo(true, "Help should mention manual override")
    }

    @Test
    fun helpTextDocumentsEveryOutputFormatEnumForTag() = setup(object {
        val command = cli()
    }) exercise {
        command.test("tag --help")
    } verify { result ->
        val undocumentedFormats = OutputFormat.entries.filterNot { format ->
            result.output.contains(format.name.lowercase())
        }
        undocumentedFormats.assertIsEqualTo(
            emptyList(),
            "Tag help must document every OutputFormat enum value. Missing: $undocumentedFormats",
        )
    }

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
        allowDetachedHead: Boolean?,
    ) {
        baseArguments = listOf("-q", "tag") +
            listOfNotNull(
                releaseBranch?.let { "--release-branch=$it" },
                userName?.let { "--user-name=$it" },
                userEmail?.let { "--user-email=$it" },
                warningsAsErrors?.let { "--warnings-as-errors=$it" },
                allowDetachedHead?.let { "--allow-detached-head=$it" },
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

        val data = json.jsonObject["data"]?.jsonObject ?: fail("Expected data object in JSON. Output: ${result.stdout}")
        data["tag"]?.jsonPrimitive?.content.assertIsEqualTo("1.2.4")
    }

    @Test
    fun dryRunShowsWhatWouldHappenWithoutTagging() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
        val expectedVersion = "1.2.4"
    }) {
        val originDirectory = com.zegreatrob.tools.test.git.createTempDirectory()
        val originGitAdapter = com.zegreatrob.tools.adapter.git.GitAdapter(originDirectory)
        originGitAdapter.init()
        originGitAdapter.config("receive.denyCurrentBranch", "ignore")
        originGitAdapter.config("commit.gpgsign", "false")
        originGitAdapter.addCommitWithMessage("init")

        val gitAdapter = initializeGitRepo(commits = commits, initialTag = initialTag, remoteUrl = originDirectory)
        gitAdapter.push()

        com.zegreatrob.tools.adapter.git.runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), projectDir)
        com.zegreatrob.tools.adapter.git.runProcess(listOf("git", "config", "user.name", "RoB as Test"), projectDir)

        baseArguments = listOf(
            "tag",
            "--release-branch=master",
            "--version=$expectedVersion",
            "--dry-run=true",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode
            .assertIsEqualTo(0, "Command failed. Stdout: ${result.stdout}\nStderr: ${result.stderr}\nOutput: ${result.output}")
        result.stdout
            .contains("Would create annotated tag")
            .assertIsEqualTo(true, "Output should indicate what would happen. Output: ${result.stdout}")
        result.stdout
            .contains(expectedVersion)
            .assertIsEqualTo(true, "Output should mention version. Output: ${result.stdout}")
        result.stdout
            .contains("no changes made")
            .assertIsEqualTo(true, "Output should clarify no changes were made. Output: ${result.stdout}")

        val gitAdapter = com.zegreatrob.tools.adapter.git.GitAdapter(projectDir)
        gitAdapter.showTag("HEAD")
            .assertIsEqualTo(null, "Should not create tag in dry-run mode")
    }

    @Test
    fun helpTextMentionsConfigFile() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("tag --help")
    } verify { result ->
        result.output.contains(".tagger").assertIsEqualTo(true, "Help should mention .tagger config file")
        result.output.contains(Regex("configuration|config file|settings")).assertIsEqualTo(true)
    }

    @Test
    fun helpTextExplainsReleaseBranchCanComeFromConfig() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("tag --help")
    } verify { result ->
        result.output.contains("--release-branch").assertIsEqualTo(true)
        result.output.contains(Regex("Required\\s+unless\\s+provided\\s+in\\s+\\.tagger")).assertIsEqualTo(true)
    }
}
