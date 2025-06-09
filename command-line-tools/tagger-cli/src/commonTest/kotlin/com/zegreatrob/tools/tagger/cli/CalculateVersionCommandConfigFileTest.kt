package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.CalculateVersionTestSpec
import com.zegreatrob.tools.tagger.TestResult
import com.zegreatrob.tools.tagger.json.TaggerConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest

@ExperimentalSerializationApi
class CalculateVersionCommandConfigFileTest : CalculateVersionTestSpec {

    override lateinit var projectDir: String

    private val taggerFile get() = "$projectDir/.tagger"
    override val addFileNames: Set<String> get() = setOf(taggerFile.split("/").last())
    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = listOf("-q", "calculate-version")
    }

    override fun configureWithDefaults() {
        val config = TaggerConfig(releaseBranch = "master")
        Json.encodeToString(config)
            .writeToFile(taggerFile)
    }

    override fun configureWithOverrides(
        implicitPatch: Boolean?,
        disableDetached: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
    ) {
        var config = TaggerConfig()
        implicitPatch?.let { config = config.copy(implicitPatch = implicitPatch) }
        disableDetached?.let { config = config.copy(disableDetached = disableDetached) }
        versionRegex?.let { config = config.copy(versionRegex = versionRegex) }
        majorRegex?.let { config = config.copy(majorRegex = majorRegex) }
        minorRegex?.let { config = config.copy(minorRegex = minorRegex) }
        patchRegex?.let { config = config.copy(patchRegex = patchRegex) }
        noneRegex?.let { config = config.copy(noneRegex = noneRegex) }
        config = config.copy(releaseBranch = "master")
        Json.encodeToString(config)
            .writeToFile(taggerFile)
    }

    override fun execute(): TestResult {
        val test = cli()
            .test(arguments, envvars = mapOf("PWD" to projectDir))
        return if (test.statusCode == 0) {
            test
                .stdout
                .trim()
                .let { TestResult.Success(it) }
        } else {
            TestResult.Failure(test.output.trim())
        }
    }
}
