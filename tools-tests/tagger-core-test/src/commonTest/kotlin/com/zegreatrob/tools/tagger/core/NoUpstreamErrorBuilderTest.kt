package com.zegreatrob.tools.tagger.core

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class NoUpstreamErrorBuilderTest {

    @Test
    fun errorMessageIncludesWarningAndRisk() = setup(object {
    }) exercise {
        buildNoUpstreamError()
    } verify { result ->
        result.contains("⚠️")
            .assertIsEqualTo(true)
        result.contains("detached HEAD")
            .assertIsEqualTo(true)
        result.contains("RISK:")
            .assertIsEqualTo(true)
        result.contains("production releases")
            .assertIsEqualTo(true)
    }

    @Test
    fun errorMessageIncludesDocumentationLink() = setup(object {
    }) exercise {
        buildNoUpstreamError()
    } verify { result ->
        result.contains("https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md")
            .assertIsEqualTo(true)
    }
}
