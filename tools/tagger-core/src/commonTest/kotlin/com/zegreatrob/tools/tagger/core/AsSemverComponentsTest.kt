package com.zegreatrob.tools.tagger.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AsSemverComponentsTest {
    @Test
    fun canParseSimpleVersion() {
        assertEquals(listOf(3, 1, 7), "v3.1.7".asSemverComponents())
    }

    @Test
    fun willIgnoreVersionPrefix() {
        assertEquals(listOf(1, 2, 3), "v1.2.3".asSemverComponents())
    }

    @Test
    fun willIgnoreMassivePrefix() {
        assertEquals(listOf(3, 2, 1), "vsjdhfksdjhf3.2.1".asSemverComponents())
    }
}
