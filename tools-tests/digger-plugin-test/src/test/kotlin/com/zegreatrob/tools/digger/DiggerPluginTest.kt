package com.zegreatrob.tools.digger

import com.zegreatrob.testmints.setup
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class DiggerPluginTest {
    @Test
    fun `plugin registers task`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.digger")
    } verify {
        assertNotNull(project.tasks.findByName("currentContributionData"))
    }
}
