package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DiggerTest {

    @Test
    fun versionWillReturnAppropriateVersion() = setup(object {
        val expectedVersion = getEnvironmentVariable("EXPECTED_VERSION")
    }) {
        assertNotNull(expectedVersion, "Test not setup correctly - include build version")
    } exercise {
        cli().test("--version")
    } verify { result ->
        assertEquals("digger version $expectedVersion", result.output.trim())
    }
}
