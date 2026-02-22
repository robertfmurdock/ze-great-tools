package com.zegreatrob.tools.tagger

import com.zegreatrob.testmints.setup
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
    fun willGenerateSettingsFileToStandardOutByDefault() = setup(object {
    }) exercise {
        execute()
    } verify { result ->
        assertEquals(TestResult.Success(prettyJson.encodeToString(runtimeDefaultConfig)), result)
    }

    @Test
    fun willGenerateSettingsFileFileWithArgument() = setup(object {
    }) exercise {
        execute(file = "")
    } verify { result ->
        assertEquals(TestResult.Success("Saved to .tagger"), result)
        assertEquals(runtimeDefaultConfig, readFromFile(taggerFile)?.let(prettyJson::decodeFromString))
    }

    @Test
    fun willGenerateSettingsFileFileWithArgumentAtSpecifiedLocation() = setup(object {
        val fileName = "tagger-settings.json"
    }) exercise {
        execute(file = fileName)
    } verify { result ->
        assertEquals(TestResult.Success("Saved to $fileName"), result)
        assertEquals(runtimeDefaultConfig, readFromFile("$projectDir/$fileName")?.let(prettyJson::decodeFromString))
    }

    @Test
    fun whenFileAlreadyExistsWillNotGenerateSettingsFileFileWithArgument() = setup(object {
    }) {
        "Something already here".writeToFile(taggerFile)
    } exercise {
        execute(file = "")
    } verify { result ->
        assertEquals(TestResult.Failure("File already exists."), result)
    }

    @Test
    fun givenMergeArgumentWhenFileAlreadyExistsWillNotGenerateSettingsFileFileWithArgument() = setup(object {
        val config = TaggerConfig(
            releaseBranch = "jim",
            implicitPatch = false,
            userName = "jimbo",
        )
    }) {
        Json.encodeToString(config).writeToFile(taggerFile)
        execute(merge = true)
    } exercise {
        execute(file = "")
    } verify { result ->
        assertEquals(result is TestResult.Success, true, "$result")
        assertEquals(
            runtimeDefaultConfig.copy(
                releaseBranch = config.releaseBranch,
                implicitPatch = config.implicitPatch,
                userName = config.userName,
            ),
            readFromFile(taggerFile)?.let(Json::decodeFromString),
        )
    }

    @Test
    fun givenMergeArgumentWillMergeExistingFileIntoOutput() = setup(object {
        val config = TaggerConfig(
            releaseBranch = "jim",
            implicitPatch = false,
            userName = "jimbo",
        )
    }) {
        Json.encodeToString(config).writeToFile(taggerFile)
    } exercise {
        execute(merge = true)
    } verify { result ->
        assertEquals(
            TestResult.Success(
                prettyJson.encodeToString(
                    runtimeDefaultConfig.copy(
                        releaseBranch = config.releaseBranch,
                        implicitPatch = config.implicitPatch,
                        userName = config.userName,
                    ),
                ),
            ),
            result,
        )
    }
}
