package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.cli.readFromFile
import com.zegreatrob.tools.tagger.GenerateSettingsFileTestSpec
import com.zegreatrob.tools.tagger.TestResult
import com.zegreatrob.tools.tagger.json.TaggerConfig
import com.zegreatrob.tools.tagger.json.runtimeDefaultConfig
import kotlin.test.Test

class GenerateSettingsFileCommandTest : GenerateSettingsFileTestSpec {

    override lateinit var projectDir: String
    private val baseArguments: List<String> = listOf("-q", "generate-settings-file")
    private var mergeFlag: Boolean? = null

    override fun execute(file: String?, merge: Boolean?): TestResult {
        if (merge != null) {
            mergeFlag = merge
        }
        val args = baseArguments +
            listOfNotNull(
                file?.let { "--file=$it" },
                mergeFlag?.let { "--merge=$it" },
            )
        val test = cli()
            .test(args, envvars = mapOf("PWD" to projectDir))
        return if (test.statusCode == 0) {
            test
                .output
                .trim()
                .let { TestResult.Success(it) }
        } else {
            TestResult.Failure(test.output.trim())
        }
    }

    @Test
    fun helpTextExplainsCommandPurpose() = setup(object {
        val command = cli()
    }) exercise {
        command.test("generate-settings-file --help")
    } verify { result ->
        result.output.contains("generate-settings-file").assertIsEqualTo(true)
        result.output.contains("configuration").assertIsEqualTo(true, "Help should explain config purpose")
        result.output.contains(".tagger").assertIsEqualTo(true, "Help should mention .tagger filename")
    }

    @Test
    fun helpTextExplainsFileOption() = setup(object {
        val command = cli()
    }) exercise {
        command.test("generate-settings-file --help")
    } verify { result ->
        result.output.contains("--file").assertIsEqualTo(true)
        result.output.contains(Regex("save|Save|write|output")).assertIsEqualTo(true, "Help should explain file writing")
    }

    @Test
    fun helpTextExplainsMergeOption() = setup(object {
        val command = cli()
    }) exercise {
        command.test("generate-settings-file --help")
    } verify { result ->
        result.output.contains("--merge").assertIsEqualTo(true)
        result.output.contains(Regex("existing|preserve|Merge|merge")).assertIsEqualTo(true, "Help should explain merge behavior")
    }

    @Test
    fun fileOptionWithoutValueUsesDefaultTaggerFile() = setup(object {
        val command = cli()
    }) exercise {
        command.test(listOf("-q", "generate-settings-file", "--file"), envvars = mapOf("PWD" to projectDir))
    } verify { result ->
        result.statusCode.assertIsEqualTo(0)
        result.output.trim().assertIsEqualTo("Saved to .tagger")
        readFromFile("$projectDir/.tagger")
            ?.let { prettyJson.decodeFromString<TaggerConfig>(it) }
            .assertIsEqualTo(runtimeDefaultConfig)
    }
}
