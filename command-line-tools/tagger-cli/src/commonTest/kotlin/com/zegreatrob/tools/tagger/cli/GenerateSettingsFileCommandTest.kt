package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.GenerateSettingsFileTestSpec
import com.zegreatrob.tools.tagger.TestResult
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
}
