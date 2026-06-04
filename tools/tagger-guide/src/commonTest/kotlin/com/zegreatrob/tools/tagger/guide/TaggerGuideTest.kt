package com.zegreatrob.tools.tagger.guide

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlin.test.Test

class TaggerGuideTest {

    @Test
    fun `getTaggerGuideContent returns guide markdown`() = setup(object {
    }) exercise {
        getTaggerGuideContent()
    } verify { content ->
        content.contains("Use Tagger when:")
            .assertIsEqualTo(true)
    }
}
