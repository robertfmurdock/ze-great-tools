package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
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
        result.assertIsEqualTo(TestResult.Success(prettyJson.encodeToString(runtimeDefaultConfig)))
    }

    @Test
    fun willGenerateSettingsFileFileWithArgument() = setup(object {
    }) exercise {
        execute(file = "")
    } verify { result ->
        result.assertIsEqualTo(TestResult.Success("Saved to .tagger"))
        readFromFile(taggerFile)
            ?.let { prettyJson.decodeFromString<TaggerConfig>(it) }
            .assertIsEqualTo(runtimeDefaultConfig)
    }

    @Test
    fun willGenerateSettingsFileFileWithArgumentAtSpecifiedLocation() = setup(object {
        val fileName = "tagger-settings.json"
    }) exercise {
        execute(file = fileName)
    } verify { result ->
        result.assertIsEqualTo(TestResult.Success("Saved to $fileName"))
        readFromFile("$projectDir/$fileName")
            ?.let { prettyJson.decodeFromString<TaggerConfig>(it) }
            .assertIsEqualTo(runtimeDefaultConfig)
    }

    @Test
    fun whenFileAlreadyExistsWillNotGenerateSettingsFileFileWithArgument() = setup(object {
    }) {
        "Something already here".writeToFile(taggerFile)
    } exercise {
        execute(file = "")
    } verify { result ->
        result.assertIsEqualTo(TestResult.Failure("File already exists."))
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
        (result is TestResult.Success).assertIsEqualTo(true, "$result")
        readFromFile(taggerFile)
            ?.let { Json.decodeFromString<TaggerConfig>(it) }
            .assertIsEqualTo(
                runtimeDefaultConfig.copy(
                    releaseBranch = config.releaseBranch,
                    implicitPatch = config.implicitPatch,
                    userName = config.userName,
                ),
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
        result.assertIsEqualTo(
            TestResult.Success(
                prettyJson.encodeToString(
                    runtimeDefaultConfig.copy(
                        releaseBranch = config.releaseBranch,
                        implicitPatch = config.implicitPatch,
                        userName = config.userName,
                    ),
                ),
            ),
        )
    }
}
