package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DiggerTest {

    @Test
    fun versionWillReturnAppropriateVersion() {
        val expectedVersion = getEnvironmentVariable("EXPECTED_VERSION")
        assertNotNull(expectedVersion, "Test not setup correctly - include build version")
        cli()
            .test("--version")
            .output
            .trim()
            .let {
                assertEquals("digger version $expectedVersion", it)
            }
    }
}
