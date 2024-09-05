package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
