package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlin.test.Test

class TaggerTest {
    @Test
    fun quietWillSuppressWelcome() = setup(object {
        val tagger = Tagger()
    }) exercise {
        tagger.test("--quiet")
    } verify { result ->
        result.output.assertIsEqualTo("")
    }

    @Test
    fun quietHasShorthand() = setup(object {
        val tagger = Tagger()
    }) exercise {
        tagger.test("-q")
    } verify { result ->
        result.output.assertIsEqualTo("")
    }

    @Test
    fun versionWillReturnAppropriateVersion() = setup(object {
        val expectedVersion = getEnvironmentVariable("EXPECTED_VERSION")
            ?: error("Test not setup correctly - include build version")
        val command = cli()
    }) exercise {
        command.test("-q --version")
    } verify { result ->
        result.output.trim().assertIsEqualTo("tagger version $expectedVersion")
    }

    @Test
    fun helpTextExplainsQuietOption() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("--quiet").assertIsEqualTo(true)
        result.output.contains("stdout").assertIsEqualTo(true, "Help should explain stdout/stderr split")
        result.output.contains("stderr").assertIsEqualTo(true, "Help should explain stdout/stderr split")
    }

    @Test
    fun helpTextGuidesAutomationToJsonFormat() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("--format=json").assertIsEqualTo(true, "Help should mention JSON format")
        result.output.contains("machine-readable").assertIsEqualTo(true, "Help should mention machine-readable output")
    }

    @Test
    fun helpTextReferencesFitCheckInGuide() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("tagger guide").assertIsEqualTo(true, "Help should reference guide command for fit check")
        result.output.contains("fit assessment").assertIsEqualTo(true, "Help should mention fit assessment")
    }

    @Test
    fun helpTextIncludesBuildScriptExample() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("build script").assertIsEqualTo(true, "Help should reference build script usage")
        result.output.contains("calculate-version").assertIsEqualTo(true, "Help should show calculate-version")
        result.output.contains("your-build-script").assertIsEqualTo(true, "Help should show generic build script")
        result.output.contains("tag --version").assertIsEqualTo(true, "Help should show tagging step")
        result.output.contains("before tagging")
            .assertIsEqualTo(true, "Help should emphasize building before tagging")
    }

    @Test
    fun rootHelpIsConcise() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        val lines = result.output.lines()
        val commandsLineIndex = lines.indexOfFirst { it.trim().startsWith("Commands:") }
        if (commandsLineIndex == -1) error("Commands section not found in help output")

        (commandsLineIndex <= 25).assertIsEqualTo(
            true,
            "Commands section should appear within first screen (~25 lines), found at line $commandsLineIndex",
        )

        result.output.contains(Regex("tagger\\s+guide")).assertIsEqualTo(
            true,
            "Help should reference guide command",
        )
    }
}
