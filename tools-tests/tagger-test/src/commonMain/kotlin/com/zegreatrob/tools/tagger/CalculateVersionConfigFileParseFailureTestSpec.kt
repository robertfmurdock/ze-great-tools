package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

interface CalculateVersionConfigFileParseFailureTestSpec : CalculateVersionTestSpec {
    fun configureWithRawTaggerConfig(contents: String)

    @Test
    fun reportsErrorForInvalidTaggerFile() = setup(object {
        val invalidJson = """
            {
              "releaseBranch": "master",
              invalid
            }
        """.trimIndent()
    }) {
        configureWithRawTaggerConfig(invalidJson)
        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1"),
            initialTag = "1.0.0",
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains("Failed to parse .tagger file")
                .assertIsEqualTo(true, "Expected parse error. Output:\n$reason")
        }
    }
}
