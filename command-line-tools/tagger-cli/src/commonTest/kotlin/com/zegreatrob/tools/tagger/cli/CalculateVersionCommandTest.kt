package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.tagger.CalculateVersionTestSpec
import com.zegreatrob.tools.tagger.TestResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

class CalculateVersionCommandTest : CalculateVersionTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String> = emptySet()
    private lateinit var baseArguments: List<String>

    override fun configureWithDefaults() {
        baseArguments = listOf(
            "-q",
            "calculate-version",
            "--release-branch=master",
            projectDir,
        )
    }

    override fun configureWithOverrides(
        implicitPatch: Boolean?,
        disableDetached: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
        forceSnapshot: Boolean?,
    ) {
        baseArguments = listOf("-q", "calculate-version") +
            listOfNotNull(
                implicitPatch?.let { "--implicit-patch=$it" },
                disableDetached?.let { "--disable-detached=$it" },
                versionRegex?.let { "--version-regex=$it" },
                majorRegex?.let { "--major-regex=$it" },
                minorRegex?.let { "--minor-regex=$it" },
                patchRegex?.let { "--patch-regex=$it" },
                noneRegex?.let { "--none-regex=$it" },
                forceSnapshot?.let { "--force-snapshot=$it" },
            ) +
            listOf("--release-branch=master", projectDir)
    }

    override fun execute(): TestResult {
        val test = cli()
            .test(baseArguments)
        return if (test.statusCode == 0) {
            TestResult.Success(
                message = test.stdout.trim(),
                details = test.stderr.trim(),
            )
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
            "calculate-version",
            "--release-branch=master",
            "--format=json",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0, "Command failed. Stdout: ${result.stdout}\nStderr: ${result.stderr}\nOutput: ${result.output}")

        val json = Json.parseToJsonElement(result.stdout)
        json.jsonObject["status"]?.jsonPrimitive?.content.assertIsEqualTo("success")
        assertNotNull(json.jsonObject["data"], "Expected data field in JSON output")
    }

    @Test
    fun withFormatJsonIncludesVersionData() = setup(object {
        val commits = listOf("init", "[patch] commit 1", "[patch] commit 2")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            "--format=json",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0, "Stdout: ${result.stdout}")

        val json = Json.parseToJsonElement(result.stdout)
        val data = json.jsonObject["data"]?.jsonObject ?: fail("Expected data object")

        data["version"]?.jsonPrimitive?.content.assertIsEqualTo("1.2.4")
        data["snapshot"]?.jsonPrimitive?.boolean.assertIsEqualTo(false)

        val reasons = data["snapshotReasons"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        reasons.assertIsEqualTo(emptyList(), "Expected empty snapshot reasons for non-snapshot version")
    }

    @Test
    fun withFormatJsonIncludesSnapshotReasons() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            "--format=json",
            "--force-snapshot=true",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0)

        val json = Json.parseToJsonElement(result.stdout)
        val data = json.jsonObject["data"]?.jsonObject ?: fail("Expected data object")

        data["version"]?.jsonPrimitive?.content.assertIsEqualTo("1.2.4-SNAPSHOT")
        data["snapshot"]?.jsonPrimitive?.boolean.assertIsEqualTo(true)

        val reasons = data["snapshotReasons"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        reasons.contains("FORCED").assertIsEqualTo(true, "Expected FORCED in snapshot reasons. Got: $reasons")
    }

    @Test
    fun withInvalidFormatShowsAvailableOptions() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            "--format=yaml",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
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
    fun withFormatJsonErrorReturnsValidJson() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
    }) {
        initializeGitRepo(commits = commits, initialTag = null)
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            "--format=json",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(1, "Command should fail when no tags exist")

        val json = Json.parseToJsonElement(result.stdout)
        json.jsonObject["status"]?.jsonPrimitive?.content.assertIsEqualTo("error")
        assertNotNull(json.jsonObject["error"], "Expected error field in JSON output")
        assertNotNull(json.jsonObject["code"], "Expected error code in JSON output")
    }
}
