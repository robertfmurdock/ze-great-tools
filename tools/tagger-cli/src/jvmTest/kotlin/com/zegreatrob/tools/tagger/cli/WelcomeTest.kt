package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertEquals

class WelcomeTest {
    @Test
    fun quietWillSuppressWelcome() {
        Welcome()
            .test("--quiet")
            .output
            .let {
                assertEquals("", it)
            }
    }

    @Test
    fun quietHasShorthand() {
        Welcome()
            .test("-q")
            .output
            .let {
                assertEquals("", it)
            }
    }
}
