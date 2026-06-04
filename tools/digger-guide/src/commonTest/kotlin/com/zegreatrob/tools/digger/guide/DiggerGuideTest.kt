package com.zegreatrob.tools.digger.guide

import com.zegreatrob.testmints.setup
import com.zegreatrob.minassert.assertIsEqualTo
import kotlin.test.Test

class DiggerGuideTest {

    @Test
    fun `getDiggerGuideContent returns guide markdown`() = setup(object {
    }) exercise {
        getDiggerGuideContent()
    } verify { content ->
        content.contains("Use Digger when:")
            .assertIsEqualTo(true)
    }
}
