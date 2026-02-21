package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TaggerTest {
    @Test
    fun quietWillSuppressWelcome() = setup(object {
        val tagger = Tagger()
    }) exercise {
        tagger.test("--quiet")
    } verify { result ->
        assertEquals("", result.output)
    }

    @Test
    fun quietHasShorthand() = setup(object {
        val tagger = Tagger()
    }) exercise {
        tagger.test("-q")
    } verify { result ->
        assertEquals("", result.output)
    }

    @Test
    fun versionWillReturnAppropriateVersion() = setup(object {
        val expectedVersion = getEnvironmentVariable("EXPECTED_VERSION")
        val command = cli()
    }) {
        assertNotNull(expectedVersion, "Test not setup correctly - include build version")
    } exercise {
        command.test("-q --version")
    } verify { result ->
        assertEquals("tagger version $expectedVersion", result.output.trim())
    }
}
