package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.TagTestSpec
import com.zegreatrob.tools.tagger.TestResult
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest

class TagCommandConfigFileTest : TagTestSpec {

    override lateinit var projectDir: String

    private val taggerFile get() = "$projectDir/.tagger"
    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = listOf("-q", "tag")
    }

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
        arguments += "--version=$version"
        val test = cli()
            .test(
                arguments,
                envvars = mapOf("PWD" to projectDir) + mapOf(
                    "PATH" to (getEnvironmentVariable("PATH") ?: ""),
                    "GIT_CONFIG_GLOBAL" to (getEnvironmentVariable("GIT_CONFIG_GLOBAL") ?: ""),
                    "GIT_CONFIG_SYSTEM" to (getEnvironmentVariable("GIT_CONFIG_SYSTEM") ?: ""),
                ),
            )
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
