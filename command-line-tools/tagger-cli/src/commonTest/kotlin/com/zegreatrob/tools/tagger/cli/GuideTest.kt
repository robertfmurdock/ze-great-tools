package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class GuideTest {
    @Test
    fun guideCommandShowsFitAssessment() = setup(object {
        val command = cli()
    }) exercise {
        command.test("guide --help")
    } verify { result ->
        result.output.contains("Use Tagger when").assertIsEqualTo(true)
        result.output.contains("Do not use Tagger when").assertIsEqualTo(true)
    }
}
