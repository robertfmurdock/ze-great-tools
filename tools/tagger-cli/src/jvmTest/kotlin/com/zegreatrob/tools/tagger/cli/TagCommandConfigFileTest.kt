@file:OptIn(ExperimentalSerializationApi::class)

package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.TagTestSpec
import com.zegreatrob.tools.tagger.TestResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest

class TagCommandConfigFileTest : TagTestSpec {

    @field:TempDir
    override lateinit var projectDir: File

    override val addFileNames: Set<String> = emptySet()
    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = listOf("-q", "tag")
    }

    override fun configureWithDefaults() {
        val config = TaggerConfig(releaseBranch = "master")
        Json.encodeToStream(config, File(projectDir, ".tagger").outputStream())
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
        Json.encodeToStream(config, File(projectDir, ".tagger").outputStream())
    }

    override fun execute(version: String): TestResult {
        arguments += "--version=$version"
        val test = cli()
            .test(arguments, envvars = mapOf("PWD" to projectDir.absolutePath))
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
