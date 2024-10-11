package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TaggerTest {
    @Test
    fun quietWillSuppressWelcome() {
        Tagger()
            .test("--quiet")
            .output
            .let {
                assertEquals("", it)
            }
    }

    @Test
    fun quietHasShorthand() {
        Tagger()
            .test("-q")
            .output
            .let {
                assertEquals("", it)
            }
    }

    @Test
    fun versionWillReturnAppropriateVersion() {
        val expectedVersion = getEnvironmentVariable("EXPECTED_VERSION")
        assertNotNull(expectedVersion, "Test not setup correctly - include build version")
        cli()
            .test("-q --version")
            .output
            .trim()
            .let {
                assertEquals("tagger version $expectedVersion", it)
            }
    }
}
