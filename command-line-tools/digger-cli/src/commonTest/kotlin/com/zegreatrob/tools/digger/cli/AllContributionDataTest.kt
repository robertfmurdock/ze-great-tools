package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.digger.AllContributionTestSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertNotNull

class AllContributionDataTest : AllContributionTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>
    private val outputFile: String get() = "$projectDir/temp-file.json"

    override fun setupWithDefaults() {
        arguments = listOf(
            "--output-file=$outputFile",
            projectDir,
        )
    }

    override fun setupWithOverrides(
        label: String?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        noneRegex: String?,
        storyRegex: String?,
        easeRegex: String?,
        tagRegex: String?,
    ) {
        arguments = listOf(
            "--output-file=$outputFile",
            projectDir,
        ) + listOfNotNull(
            label?.let { "--label=$it" },
            majorRegex?.let { "--major-regex=$it" },
            minorRegex?.let { "--minor-regex=$it" },
            patchRegex?.let { "--patch-regex=$it" },
            noneRegex?.let { "--none-regex=$it" },
            storyRegex?.let { "--story-id-regex=$it" },
            easeRegex?.let { "--ease-regex=$it" },
            tagRegex?.let { "--tag-regex=$it" },
        )
    }

    override fun runAllContributionData(): AllContributionTestSpec.AllContributionDataResult {
        val output = AllContributionData()
            .test(arguments)
            .output
        return AllContributionTestSpec.AllContributionDataResult(
            output = output,
            data = readFromFile(outputFile) ?: "",
        )
    }

    override fun verifyOutput(result: AllContributionTestSpec.AllContributionDataResult) {
        result.output.assertIsEqualTo("Data written to ${outputFile}\n")
    }

    @Test
    fun withFormatJsonOutputsValidJson() = setup(object {
        val commits = listOf("[major] initial commit", "[minor] add feature")
    }) {
        initializeGitRepo(commits = commits)
        arguments = listOf(
            "--format=json",
            projectDir,
        )
    } exercise {
        AllContributionData().test(arguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0, "Command failed. Stdout: ${result.stdout}\nStderr: ${result.stderr}")

        val json = Json.parseToJsonElement(result.stdout)
        json.jsonObject["status"]?.jsonPrimitive?.content.assertIsEqualTo("success")
        assertNotNull(json.jsonObject["data"], "Expected data field in JSON output")
    }

    @Test
    fun withFormatTextPreservesCurrentBehavior() = setup(object {
        val commits = listOf("[major] initial commit", "[minor] add feature")
    }) {
        initializeGitRepo(commits = commits)
        arguments = listOf(
            "--format=text",
            "--output-file=$outputFile",
            projectDir,
        )
    } exercise {
        AllContributionData().test(arguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0)
        result.output.contains("Data written to").assertIsEqualTo(true, "Expected text mode to write to file")
        val fileContent = readFromFile(outputFile)
        assertNotNull(fileContent, "Expected file to be written in text mode")
    }

    @Test
    fun withInvalidFormatShowsAvailableOptions() = setup(object {
        val commits = listOf("[major] initial commit")
    }) {
        initializeGitRepo(commits = commits)
        arguments = listOf(
            "--format=yaml",
            projectDir,
        )
    } exercise {
        AllContributionData().test(arguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(1, "Command should fail with invalid format")
        result.output.contains("text", ignoreCase = true).assertIsEqualTo(
            true,
            "Error should mention 'text' option. Output: ${result.output}",
        )
        result.output.contains("json", ignoreCase = true).assertIsEqualTo(
            true,
            "Error should mention 'json' option. Output: ${result.output}",
        )
    }

    @Test
    fun helpExplainsTagBasedBoundariesAndOutputShape() = setup(object {
        val command = cli()
    }) exercise {
        command.test("all-contribution-data --help")
    } verify { result ->
        result.output.contains("tag boundaries").assertIsEqualTo(
            true,
            "Help should explain tag-based contribution boundaries",
        )
        result.output.contains("data[]").assertIsEqualTo(
            true,
            "Help should describe output array structure in json mode",
        )
    }
}
