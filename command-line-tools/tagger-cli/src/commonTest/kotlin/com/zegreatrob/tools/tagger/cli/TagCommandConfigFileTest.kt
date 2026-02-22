package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.TagTestSpec
import com.zegreatrob.tools.tagger.TestResult
import com.zegreatrob.tools.tagger.json.TaggerConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
class TagCommandConfigFileTest : TagTestSpec {

    override lateinit var projectDir: String

    private val taggerFile get() = "$projectDir/.tagger"
    override val addFileNames: Set<String> = emptySet()
    private val baseArguments: List<String> = listOf("-q", "tag")

    override fun configureWithDefaults() {
        val config = TaggerConfig(releaseBranch = "master")
        Json.encodeToString(config)
            .writeToFile(taggerFile)
    }

    override fun configureWithOverrides(
        releaseBranch: String?,
        userName: String?,
        userEmail: String?,
        warningsAsErrors: Boolean?,
    ) {
        var config = TaggerConfig()
        releaseBranch?.let { config = config.copy(releaseBranch = releaseBranch) }
        userName?.let { config = config.copy(userName = userName) }
        userEmail?.let { config = config.copy(userEmail = userEmail) }
        warningsAsErrors?.let { config = config.copy(warningsAsErrors = warningsAsErrors) }
        Json.encodeToString(config)
            .writeToFile(taggerFile)
    }

    override fun execute(version: String): TestResult {
        val test = cli()
            .test(baseArguments + "--version=$version", envvars = mapOf("PWD" to projectDir))
        return if (test.statusCode == 0) {
            test
                .output
                .trim()
                .let { TestResult.Success(it) }
        } else {
            TestResult.Failure(test.output.trim())
        }
    }
}
