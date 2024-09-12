package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.CalculateVersionTestSpec
import com.zegreatrob.tools.tagger.TestResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.BeforeTest

@ExperimentalSerializationApi
class CalculateVersionCommandConfigFileTest : CalculateVersionTestSpec {

    @field:TempDir
    override lateinit var projectDir: File

    override val addFileNames: Set<String> get() = setOf(projectDir.resolve(".tagger").name)
    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = listOf("-q", "calculate-version")
    }

    override fun configureWithDefaults() {
        val config = TaggerConfig(releaseBranch = "master")
        Json.encodeToStream(config, File(projectDir, ".tagger").outputStream())
    }

    override fun configureWithOverrides(
        implicitPatch: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
    ) {
        var config = TaggerConfig()
        implicitPatch?.let { config = config.copy(implicitPatch = implicitPatch) }
        versionRegex?.let { config = config.copy(versionRegex = versionRegex) }
        majorRegex?.let { config = config.copy(majorRegex = majorRegex) }
        minorRegex?.let { config = config.copy(minorRegex = minorRegex) }
        patchRegex?.let { config = config.copy(patchRegex = patchRegex) }
        noneRegex?.let { config = config.copy(noneRegex = noneRegex) }
        config = config.copy(releaseBranch = "master")
        Json.encodeToStream(config, File(projectDir, ".tagger").outputStream())
    }

    override fun execute(): TestResult {
        val test = cli()
            .test(arguments, envvars = mapOf("PWD" to projectDir.absolutePath))
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
