package com.zegreatrob.tools.tagger.core

import com.zegreatrob.testmints.setup
import kotlin.test.Test
import kotlin.test.assertEquals

class AsSemverComponentsTest {
    private fun test(block: () -> Unit) = setup(object {}) exercise { block() } verify { }

    @Test
    fun canParseSimpleVersion() = test {
        assertEquals(listOf(3, 1, 7), "v3.1.7".asSemverComponents())
    }

    @Test
    fun willIgnoreVersionPrefix() = test {
        assertEquals(listOf(1, 2, 3), "v1.2.3".asSemverComponents())
    }

    @Test
    fun willIgnoreMassivePrefix() = test {
        assertEquals(listOf(3, 2, 1), "vsjdhfksdjhf3.2.1".asSemverComponents())
    }
}
