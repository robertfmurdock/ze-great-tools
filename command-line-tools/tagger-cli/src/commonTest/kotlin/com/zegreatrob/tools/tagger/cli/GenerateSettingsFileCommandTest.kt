package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.tagger.GenerateSettingsFileTestSpec
import com.zegreatrob.tools.tagger.TestResult
import kotlin.test.BeforeTest

class GenerateSettingsFileCommandTest : GenerateSettingsFileTestSpec {

    override lateinit var projectDir: String
    private lateinit var arguments: List<String>

    @BeforeTest
    fun setup() {
        arguments = listOf("-q", "generate-settings-file")
    }

    override fun execute(file: String?, merge: Boolean?): TestResult {
        file?.let { arguments += "--file=$file" }
        merge?.let { arguments += "--merge=$merge" }
        val test = cli()
            .test(arguments, envvars = mapOf("PWD" to projectDir))
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
