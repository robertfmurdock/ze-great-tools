package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.tagger.CalculateVersionTestSpec
import com.zegreatrob.tools.tagger.TestResult
import com.zegreatrob.tools.tagger.core.SnapshotReason
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

class CalculateVersionCommandTest : CalculateVersionTestSpec {
    @Test
    fun helpTextDocumentsEverySnapshotReasonEnum() = setup(object {
        val command = cli()
    }) exercise {
        command.test("calculate-version --help", width = 120)
    } verify { result ->
        val undocumentedReasons = SnapshotReason.entries.filterNot { reason ->
            result.output.contains(reason.name)
        }
        undocumentedReasons.assertIsEqualTo(
            emptyList(),
            "Help must document every SnapshotReason enum name. Missing: $undocumentedReasons",
        )
    }

    @Test
    fun helpTextIncludesSnapshotRemediationGuidance() = setup(object {
        val command = cli()
    }) exercise {
        command.test("calculate-version --help", width = 120)
    } verify { result ->
        result.output.contains("DIRTY").assertIsEqualTo(true, "Help should list DIRTY snapshot reason")
        result.output.contains("Uncommitted changes").assertIsEqualTo(true, "Help should explain DIRTY reason")
        result.output.contains("AHEAD").assertIsEqualTo(true, "Help should list AHEAD snapshot reason")
        result.output.contains("Push changes").assertIsEqualTo(true, "Help should explain how to fix AHEAD")
        result.output.contains("NOT_RELEASE_BRANCH").assertIsEqualTo(true, "Help should list NOT_RELEASE_BRANCH reason")
    }

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
        allowDetachedHead: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
        forceSnapshot: Boolean?,
        warningsAsErrors: Boolean?,
    ) {
        baseArguments = listOf("-q", "calculate-version") +
            listOfNotNull(
                implicitPatch?.let { "--implicit-patch=$it" },
                disableDetached?.let { "--disable-detached=$it" },
                allowDetachedHead?.let { "--allow-detached-head=$it" },
                versionRegex?.let { "--version-regex=$it" },
                majorRegex?.let { "--major-regex=$it" },
                minorRegex?.let { "--minor-regex=$it" },
                patchRegex?.let { "--patch-regex=$it" },
                noneRegex?.let { "--none-regex=$it" },
                forceSnapshot?.let { "--force-snapshot=$it" },
                warningsAsErrors?.let { "--warnings-as-errors=$it" },
            ) +
            listOf("--release-branch=master", projectDir)
    }

    override fun execute(): TestResult {
        val test = cli()
            .test(baseArguments)
        return if (test.statusCode == 0) {
            val stderr = test.stderr.trim()
            val lines = stderr.lines().filter { it.isNotBlank() }
            val (snapshotLines, warningLines) = lines.partition { !it.startsWith("⚠️") }
            TestResult.Success(
                message = test.stdout.trim(),
                details = snapshotLines.joinToString("\n"),
                warnings = warningLines,
            )
        } else {
            TestResult.Failure(test.output.trim())
        }
    }

    override fun warningFeatureToken(feature: String): String {
        val kebabFeature = feature.replace(Regex("([A-Z])")) { "-${it.value.lowercase()}" }
        return "--$kebabFeature"
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

    @Test
    fun withFormatTextPreservesPreviousBehavior() = setup(object {
        val commits = listOf("init", "[patch] commit 1", "[patch] commit 2")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            "--format=text",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0, "Command failed. Output: ${result.output}")
        result.stdout.trim().assertIsEqualTo("1.2.4", "Text format should output version only")
        result.stdout.contains("{").assertIsEqualTo(false, "Text format should not contain JSON")
    }

    @Test
    fun defaultFormatIsText() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0, "Command failed. Output: ${result.output}")
        result.stdout.trim().assertIsEqualTo("1.2.4")
        result.stdout.contains("{").assertIsEqualTo(false, "Default format should be text, not JSON")
    }

    @Test
    fun jsonOutputGoesToStdout() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
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
        result.statusCode.assertIsEqualTo(0)
        result.stdout.contains("\"status\"").assertIsEqualTo(true, "JSON should be in stdout")
        result.stderr.isEmpty().assertIsEqualTo(true, "JSON mode should not write to stderr for success")
    }

    @Test
    fun jsonFormatWithQuietFlagStillWorks() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "-q",
            "calculate-version",
            "--release-branch=master",
            "--format=json",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0)
        val json = Json.parseToJsonElement(result.stdout)
        json.jsonObject["status"]?.jsonPrimitive?.content.assertIsEqualTo("success")
        assertNotNull(json.jsonObject["data"])
    }

    @Test
    fun jsonErrorsGoToStdout() = setup(object {
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
        result.statusCode.assertIsEqualTo(1)
        result.stdout.contains("\"status\"").assertIsEqualTo(true, "JSON error should be in stdout")
        result.stdout.contains("\"error\"").assertIsEqualTo(true, "JSON error should be in stdout")
    }

    @Test
    fun helpTextExplainsFormatOption() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("calculate-version --help", width = 120)
    } verify { result ->
        result.output.contains("--format").assertIsEqualTo(true)
        result.output.contains(Regex("\\(default:\\s*text\\)")).assertIsEqualTo(true, "Help should show default value automatically")
        result.output.contains("structured data").assertIsEqualTo(true, "Help should explain json format purpose")
    }

    @Test
    fun helpTextDocumentsEveryOutputFormatEnumForCalculateVersion() = setup(object {
        val command = cli()
    }) exercise {
        command.test("calculate-version --help", width = 120)
    } verify { result ->
        val undocumentedFormats = OutputFormat.entries.filterNot { format ->
            result.output.contains(format.name.lowercase())
        }
        undocumentedFormats.assertIsEqualTo(
            emptyList(),
            "Help must document every OutputFormat enum value. Missing: $undocumentedFormats",
        )
    }

    @Test
    fun helpTextExplainsForceSnapshotOption() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("calculate-version --help", width = 120)
    } verify { result ->
        result.output.contains("--force-snapshot").assertIsEqualTo(true)
        result.output.contains("force").assertIsEqualTo(true, "Help should mention forcing behavior")
        result.output.contains("SNAPSHOT").assertIsEqualTo(true, "Help should mention SNAPSHOT suffix")
    }

    @Test
    fun helpTextExplainsReleaseBranchOption() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("calculate-version --help", width = 120)
    } verify { result ->
        result.output.contains("--release-branch").assertIsEqualTo(true)
        result.output.contains("release branch name").assertIsEqualTo(true, "Help should explain purpose")
        result.output.contains("SNAPSHOT").assertIsEqualTo(true, "Help should explain snapshot behavior")
    }

    @Test
    fun helpTextExplainsAllowDetachedHeadOption() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("calculate-version --help", width = 120)
    } verify { result ->
        result.output.contains("--allow-detached-head").assertIsEqualTo(true)
        result.output.contains("detached HEAD").assertIsEqualTo(true, "Help should explain what detached HEAD means")
        result.output.contains("blocked by default").assertIsEqualTo(true, "Help should explain default behavior")
    }

    @Test
    fun helpTextExplainsImplicitPatchOption() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("calculate-version --help", width = 120)
    } verify { result ->
        result.output.contains("--implicit-patch").assertIsEqualTo(true)
        result.output.contains("patch version").assertIsEqualTo(true, "Help should mention patch bumping")
        result.output.contains("default: true").assertIsEqualTo(true, "Help should show default value automatically")
    }

    @Test
    fun helpTextMentionsConfigFile() = setup(object {
        val cli = cli()
    }) exercise {
        cli.test("calculate-version --help", width = 120)
    } verify { result ->
        result.output.contains(".tagger").assertIsEqualTo(true, "Help should mention .tagger config file")
        result.output.contains(Regex("configuration|config file|settings")).assertIsEqualTo(true)
    }

    @Test
    fun allowDetachedHeadTrueAllowsNoRemote() = setup(object {
    }) {
        com.zegreatrob.tools.test.git.initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = emptySet(),
            commits = listOf("init", "commit 1"),
            initialTag = "1.2.3",
        )
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            "--allow-detached-head=true",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode
            .assertIsEqualTo(0, "Command failed. Stdout: ${result.stdout}\nStderr: ${result.stderr}")
        result.stdout
            .trim()
            .assertIsEqualTo("1.2.4-SNAPSHOT")
    }

    @Test
    fun snapshotReasonsIncludeActionableGuidance() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        initializeGitRepo(commits = commits, initialTag = initialTag)
        baseArguments = listOf(
            "calculate-version",
            "--release-branch=master",
            "--force-snapshot=true",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(0, "Command failed. Output: ${result.output}")
        result.stdout.trim().assertIsEqualTo("1.2.4-SNAPSHOT")

        val stderr = result.stderr
        stderr.contains("FORCED").assertIsEqualTo(
            true,
            "Expected FORCED reason in stderr. Stderr: $stderr",
        )
        stderr.contains(" - ").assertIsEqualTo(
            true,
            "Expected 'ENUM - message' format. Stderr: $stderr",
        )
        stderr.contains("Snapshot forced via --force-snapshot flag").assertIsEqualTo(
            true,
            "Expected actionable message in stderr. Stderr: $stderr",
        )
    }

    @Test
    fun jsonFormatWithWarningsAsErrorsReturnsErrorStatusWhenWarningsExist() = setup(object {
        val commits = listOf("init", "commit 1")
        val initialTag = "1.2.3"
    }) {
        com.zegreatrob.tools.test.git.initializeGitRepo(
            directory = projectDir,
            commits = commits,
            initialTag = initialTag,
            remoteUrl = null,
            addFileNames = addFileNames,
        )
        baseArguments = listOf(
            "-q",
            "calculate-version",
            "--release-branch=master",
            "--disable-detached=false",
            "--warnings-as-errors=true",
            "--format=json",
            projectDir,
        )
    } exercise {
        cli().test(baseArguments)
    } verify { result ->
        result.statusCode.assertIsEqualTo(1, "Should exit with code 1 when warnings present and warningsAsErrors enabled")

        val json = Json.parseToJsonElement(result.stdout)
        json.jsonObject["status"]?.jsonPrimitive?.content.assertIsEqualTo(
            "error",
            "JSON status should be 'error' when warnings escalate to errors, not 'success'",
        )

        val errorMessage = json.jsonObject["error"]?.jsonPrimitive?.content
        assertNotNull(errorMessage, "JSON should include error message")
        errorMessage.contains("warning", ignoreCase = true).assertIsEqualTo(
            true,
            "Error message should mention warnings. Got: $errorMessage",
        )

        val code = json.jsonObject["code"]?.jsonPrimitive?.content
        code.assertIsEqualTo("WARNINGS_AS_ERRORS", "Error code should indicate warnings escalation")
    }
}
