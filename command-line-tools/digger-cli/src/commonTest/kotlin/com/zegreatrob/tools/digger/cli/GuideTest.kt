package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class GuideTest {
    @Test
    fun guideCommandShowsFitAssessment() = setup(object {
        val command = cli()
    }) exercise {
        command.test("guide")
    } verify { result ->
        assertGuideOutput(result.statusCode, result.output)
    }
}

private val requiredGuidePhrases = listOf(
    "Use Digger when",
    "Do not use Digger when",
    "Workflow philosophy",
    "Best practices",
    "Prerequisites",
    "Regex override contract",
    "First-run workflow",
)

private fun String.toWhitespaceTolerantRegex(): String = trim()
    .split(Regex("\\s+"))
    .joinToString("\\s+") { Regex.escape(it) }

private fun assertGuideOutput(statusCode: Int, output: String) {
    statusCode.assertIsEqualTo(0, "Guide command should exit successfully")
    assertGuideIncludesRequiredPhrases(output)
}

private fun assertGuideIncludesRequiredPhrases(output: String) = requiredGuidePhrases.forEach { phrase ->
    output
        .contains(Regex(phrase.toWhitespaceTolerantRegex()))
        .assertIsEqualTo(true, "Expected guide output to include phrase: $phrase")
}
