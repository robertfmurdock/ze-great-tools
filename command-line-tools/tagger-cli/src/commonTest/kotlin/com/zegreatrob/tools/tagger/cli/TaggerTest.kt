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
        result.output.contains("should not be used in releases or tags").assertIsEqualTo(true)
    }

    @Test
    fun helpTextIncludesConfigurationSection() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Configuration:").assertIsEqualTo(true, "Help should include Configuration section")
        result.output.contains(".tagger").assertIsEqualTo(true, "Help should mention .tagger file")
        result.output.contains("generate-settings-file").assertIsEqualTo(true, "Help should reference settings generation")
    }

    @Test
    fun helpTextIncludesFitCheckGuidance() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Use Tagger when").assertIsEqualTo(true, "Help should include fit check")
        result.output.contains("Do not use Tagger when").assertIsEqualTo(true, "Help should include anti-fit guidance")
        result.output.contains("docs/why-tagger.md").assertIsEqualTo(true, "Help should reference philosophy doc")
    }

    @Test
    fun helpTextIncludesTwoStepWorkflowGuidance() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Quick start").assertIsEqualTo(true, "Help should include quick start")
        result.output.contains("calculate-version").assertIsEqualTo(true, "Help should reference calculate-version")
        result.output.contains("tag --version").assertIsEqualTo(true, "Help should reference tag with version")
        result.output.contains("snapshot == false").assertIsEqualTo(true, "Help should show snapshot check gate")
    }

    @Test
    fun helpTextIncludesSnapshotRemediationGuidance() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Resolving snapshot").assertIsEqualTo(true, "Help should include remediation section")
        result.output.contains("DIRTY").assertIsEqualTo(true, "Help should list DIRTY")
        result.output.contains("commit/stash").assertIsEqualTo(true, "Help should suggest commit/stash for DIRTY")
        result.output.contains("AHEAD").assertIsEqualTo(true, "Help should list AHEAD")
        result.output.contains("sync").assertIsEqualTo(true, "Help should suggest sync for AHEAD/BEHIND")
    }

    @Test
    fun helpTextIncludesAntiPatternGuidance() = setup(object {
        val command = cli()
    }) exercise {
        command.test("--help")
    } verify { result ->
        result.output.contains("Do:").assertIsEqualTo(true, "Help should include do/don't patterns")
        result.output.contains("Don't:").assertIsEqualTo(true, "Help should include do/don't patterns")
        result.output.contains("Calculate then tag").assertIsEqualTo(true, "Help should emphasize two-step workflow")
        result.output.contains("Tag arbitrary versions").assertIsEqualTo(true, "Help should warn against ad hoc versions")
    }
}
