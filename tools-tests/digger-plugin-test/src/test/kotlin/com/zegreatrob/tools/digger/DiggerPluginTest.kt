package com.zegreatrob.tools.digger

import com.zegreatrob.minassert.assertIsEqualTo
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

    @Test
    fun `plugin registers diggerGuide task`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.digger")
    } verify {
        project.tasks.findByName("diggerGuide")
            .assertIsNotEqualTo(null, "Expected diggerGuide task to be registered")
    }

    @Test
    fun `diggerGuide task loads content from markdown resource`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.digger")
        val task = project.tasks.findByName("diggerGuide") as DiggerGuideTask
        task.getGuideContent()
    } verify { content ->
        content.contains("Use Digger when:")
            .assertIsEqualTo(true, "Expected guide content to contain CLI guide text")
    }
}
