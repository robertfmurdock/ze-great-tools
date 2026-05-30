package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.CalculateVersionConfigFileParseFailureTestSpec
import com.zegreatrob.tools.tagger.CalculateVersionTestSpec
import com.zegreatrob.tools.tagger.TestResult
import com.zegreatrob.tools.tagger.json.TaggerConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@ExperimentalSerializationApi
class CalculateVersionCommandConfigFileTest :
    CalculateVersionTestSpec,
    CalculateVersionConfigFileParseFailureTestSpec {

    override lateinit var projectDir: String

    private val taggerFile get() = "$projectDir/.tagger"
    override val addFileNames: Set<String> get() = setOf(taggerFile.split("/").last())
    private val baseArguments: List<String> = listOf("-q", "calculate-version")

    override fun configureWithDefaults() {
        val config = TaggerConfig(releaseBranch = "master")
        Json.encodeToString(config)
            .writeToFile(taggerFile)
    }

    override fun configureWithRawTaggerConfig(contents: String) {
        contents.writeToFile(taggerFile)
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
        val config = TaggerConfig(
            releaseBranch = "master",
            implicitPatch = implicitPatch,
            disableDetached = disableDetached,
            allowDetachedHead = allowDetachedHead,
            majorRegex = majorRegex,
            minorRegex = minorRegex,
            patchRegex = patchRegex,
            noneRegex = noneRegex,
            versionRegex = versionRegex,
            forceSnapshot = forceSnapshot,
            warningsAsErrors = warningsAsErrors,
        )
        Json.encodeToString(config)
            .writeToFile(taggerFile)
    }

    override fun execute(): TestResult {
        val test = cli()
            .test(baseArguments, envvars = mapOf("PWD" to projectDir))
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
}
