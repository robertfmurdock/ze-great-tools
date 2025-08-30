package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.cli.writeToFile
import com.zegreatrob.tools.tagger.json.TaggerConfig
import com.zegreatrob.tools.tagger.json.runtimeDefaultConfig
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.removeDirectory
import kotlinx.serialization.json.Json
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

    fun execute(file: String? = null, merge: Boolean? = null): TestResult

    val prettyJson
        get() = Json {
            prettyPrint = true
            encodeDefaults = true
        }

    @Test
    fun willGenerateSettingsFileToStandardOutByDefault() {
        val result = execute()

        assertEquals(TestResult.Success(prettyJson.encodeToString(runtimeDefaultConfig)), result)
    }

    @Test
    fun willGenerateSettingsFileFileWithArgument() {
        val result = execute(file = "")

        assertEquals(TestResult.Success("Saved to .tagger"), result)
        assertEquals(runtimeDefaultConfig, readFromFile(taggerFile)?.let(prettyJson::decodeFromString))
    }

    @Test
    fun willGenerateSettingsFileFileWithArgumentAtSpecifiedLocation() {
        val fileName = "tagger-settings.json"
        val result = execute(file = fileName)

        assertEquals(TestResult.Success("Saved to $fileName"), result)
        assertEquals(runtimeDefaultConfig, readFromFile("$projectDir/$fileName")?.let(prettyJson::decodeFromString))
    }

    @Test
    fun whenFileAlreadyExistsWillNotGenerateSettingsFileFileWithArgument() {
        "Something already here".writeToFile(taggerFile)
        val result = execute(file = "")

        assertEquals(TestResult.Failure("File already exists."), result)
    }

    @Test
    fun givenMergeArgumentWhenFileAlreadyExistsWillNotGenerateSettingsFileFileWithArgument() {
        Json.encodeToString(
            TaggerConfig(
                releaseBranch = "jim",
                implicitPatch = false,
                userName = "jimbo",
            ),
        )
            .writeToFile(taggerFile)
        execute(merge = true)
        val result = execute(file = "")
        assertEquals(result is TestResult.Success, true, "$result")

        assertEquals(
            runtimeDefaultConfig.copy(
                releaseBranch = "jim",
                implicitPatch = false,
                userName = "jimbo",
            ),
            readFromFile(taggerFile)?.let(Json::decodeFromString),
        )
    }

    @Test
    fun givenMergeArgumentWillMergeExistingFileIntoOutput() {
        Json.encodeToString(
            TaggerConfig(
                releaseBranch = "jim",
                implicitPatch = false,
                userName = "jimbo",
            ),
        )
            .writeToFile(taggerFile)
        val result = execute(merge = true)
        assertEquals(
            TestResult.Success(
                prettyJson.encodeToString(
                    runtimeDefaultConfig.copy(
                        releaseBranch = "jim",
                        implicitPatch = false,
                        userName = "jimbo",
                    ),
                ),
            ),
            result,
        )
    }
}
