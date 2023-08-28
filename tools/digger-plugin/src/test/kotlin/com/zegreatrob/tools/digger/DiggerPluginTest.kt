package com.zegreatrob.tools.digger

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class DiggerPluginTest {
    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.zegreatrob.tools.digger")
        assertNotNull(project.tasks.findByName("listCoAuthorEmails"))
    }
}
