package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.json.runtimeDefaultConfig
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.removeDirectory
import kotlinx.serialization.encodeToString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

interface GenerateSettingsFileTestSpec {

    private val taggerFile get() = "$projectDir/.tagger"

    var projectDir: String

    @BeforeTest
    fun setUpProjectDir() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun deleteProjectDir() {
        removeDirectory(projectDir)
    }

    fun execute(file: String? = null): TestResult

    @Test
    fun willGenerateSettingsFileToStandardOutByDefault() {
        val result = execute()

        val json = kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }
        assertEquals(TestResult.Success(json.encodeToString(runtimeDefaultConfig)), result)
    }

    @Test
    fun willGenerateSettingsFileFileWithArgument() {
        val result = execute(file = "")

        val json = kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }
        assertEquals(TestResult.Success("Saved to .tagger"), result)
        assertEquals(runtimeDefaultConfig, readFromFile(taggerFile)?.let(json::decodeFromString))
    }

    @Test
    fun willGenerateSettingsFileFileWithArgumentAtSpecifiedLocation() {
        val fileName = "tagger-settings.json"
        val result = execute(file = fileName)

        val json = kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }
        assertEquals(TestResult.Success("Saved to $fileName"), result)
        assertEquals(runtimeDefaultConfig, readFromFile("$projectDir/$fileName")?.let(json::decodeFromString))
    }

    @Test
    fun whenFileAlreadyExistsWillNotGenerateSettingsFileFileWithArgument() {
        "Something already here".writeToFile(taggerFile)
        val result = execute(file = "")

        assertEquals(TestResult.Failure("File already exists."), result)
    }
}
