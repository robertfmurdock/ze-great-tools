package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.tagger.core.SnapshotReason
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
    fun helpTextIncludesOutputSection() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Output:").assertIsEqualTo(true, "Help should include Output section")
        result.output.contains("-SNAPSHOT").assertIsEqualTo(true, "Help should explain -SNAPSHOT semantics")
        SnapshotReason.entries.forEach { reason ->
            result.output.contains(reason.name)
                .assertIsEqualTo(true, "Help should document ${reason.name} status flag")
        }
    }

    @Test
    fun helpTextGuidesAutomationToJsonFormat() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Automation").assertIsEqualTo(true)
        result.output.contains("--format=json").assertIsEqualTo(true)
        result.output.contains("unmet conditions").assertIsEqualTo(true)
        result.output.contains("not decorative").assertIsEqualTo(true)
    }
}
