package com.zegreatrob.tools.tagger.core

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class AsSemverComponentsTest {
    @Test
    fun canParseSimpleVersion() = setup(object {
        val version = "v3.1.7"
    }) exercise {
        version.asSemverComponents()
    } verify { result ->
        result.assertIsEqualTo(listOf(3, 1, 7))
    }

    @Test
    fun willIgnoreVersionPrefix() = setup(object {
        val version = "v1.2.3"
    }) exercise {
        version.asSemverComponents()
    } verify { result ->
        result.assertIsEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun willIgnoreMassivePrefix() = setup(object {
        val version = "vsjdhfksdjhf3.2.1"
    }) exercise {
        version.asSemverComponents()
    } verify { result ->
        result.assertIsEqualTo(listOf(3, 2, 1))
    }
}
