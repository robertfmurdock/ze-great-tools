package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.minassert.assertIsNotEqualTo
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
        val command = cli()
    }) {
        expectedVersion.assertIsNotEqualTo(null, "Test not setup correctly - include build version")
    } exercise {
        command.test("-q --version")
    } verify { result ->
        result.output.trim().assertIsEqualTo("tagger version $expectedVersion")
    }
}
