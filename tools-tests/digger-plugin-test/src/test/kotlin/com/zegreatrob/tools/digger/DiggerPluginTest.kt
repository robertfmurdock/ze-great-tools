package com.zegreatrob.tools.digger

import com.zegreatrob.minassert.assertIsNotEqualTo
import com.zegreatrob.testmints.setup
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

class DiggerPluginTest {
    @Test
    fun `plugin registers task`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.digger")
    } verify {
        project.tasks.findByName("currentContributionData")
            .assertIsNotEqualTo(null, "Expected currentContributionData task to be registered")
    }
}
