package com.zegreatrob.tools.tagger

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class TaggerPluginTest {
    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.zegreatrob.tools.tagger")
        assertNotNull(project.tasks.findByName("calculateVersion"))
    }
}
