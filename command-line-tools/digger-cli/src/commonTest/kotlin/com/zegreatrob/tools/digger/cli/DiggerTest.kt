package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.minassert.assertIsNotEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import kotlin.test.Test

class DiggerTest {

    @Test
    fun versionWillReturnAppropriateVersion() = setup(object {
        val expectedVersion = getEnvironmentVariable("EXPECTED_VERSION")
        val command = cli()
    }) {
        expectedVersion.assertIsNotEqualTo(null, "Test not setup correctly - include build version")
    } exercise {
        command.test("--version")
    } verify { result ->
        result.output.trim().assertIsEqualTo("digger version $expectedVersion")
    }
}
