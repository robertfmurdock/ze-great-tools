package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlin.test.Test

class DiggerTest {

    @Test
    fun versionWillReturnAppropriateVersion() = setup(object {
        val expectedVersion = getEnvironmentVariable("EXPECTED_VERSION")
            ?: error("Test not setup correctly - include build version")
        val command = cli()
    }) exercise {
        command.test("--version")
    } verify { result ->
        result.output.trim().assertIsEqualTo("digger version $expectedVersion")
    }

    @Test
    fun currentContributionDataHelpShowsDefaultFormatValue() = setup(object {
        val command = cli()
    }) exercise {
        command.test("current-contribution-data --help")
    } verify { result ->
        result.output.contains(Regex("\\(default:\\s*text\\)", RegexOption.IGNORE_CASE)).assertIsEqualTo(
            true,
            "Expected format option default value in help output",
        )
    }

    @Test
    fun rootHelpRendersMarkdownWithoutRawMarkers() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Typical CI/build script usage").assertIsEqualTo(true)
        result.output.contains("```").assertIsEqualTo(false, "Expected markdown code fences to be rendered")
        result.output.contains("| Command |").assertIsEqualTo(false, "Expected markdown table syntax to be rendered")
    }
}
