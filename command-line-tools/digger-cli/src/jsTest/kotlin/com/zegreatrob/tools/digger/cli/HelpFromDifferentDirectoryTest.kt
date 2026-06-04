package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

@JsModule("node:process")
@JsNonModule
private external object NodeProcess {
    fun chdir(directory: String)
    fun cwd(): String
}

@JsModule("node:os")
@JsNonModule
private external object NodeOs {
    fun tmpdir(): String
}

class HelpFromDifferentDirectoryTest {
    @Test
    fun helpWorksFromArbitraryWorkingDirectory() = setup(object {
        val originalCwd = NodeProcess.cwd()
        val command = cli()
    }) exercise {
        runInTempDirectory(originalCwd) {
            command.test("--help")
        }
    } verify { result ->
        outputContainsExpectedHelpText(result)
            .assertIsEqualTo(true)
    }

    private fun runInTempDirectory(originalCwd: String, block: () -> Any) = try {
        NodeProcess.chdir(NodeOs.tmpdir())
        block()
    } finally {
        NodeProcess.chdir(originalCwd)
    }

    private fun outputContainsExpectedHelpText(result: Any) = (result as com.github.ajalt.clikt.testing.CliktCommandTestResult)
        .output.contains("Digger extracts contribution metadata")
}
