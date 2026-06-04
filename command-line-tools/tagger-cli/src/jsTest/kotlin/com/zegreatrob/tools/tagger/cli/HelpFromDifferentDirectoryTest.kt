package com.zegreatrob.tools.tagger.cli

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
        NodeProcess.chdir(NodeOs.tmpdir())
        try {
            command.test("--help")
        } finally {
            NodeProcess.chdir(originalCwd)
        }
    } verify { result ->
        result.output.contains("Tagger calculates semantic versions")
            .assertIsEqualTo(true, "Help should display from any working directory")
    }
}
