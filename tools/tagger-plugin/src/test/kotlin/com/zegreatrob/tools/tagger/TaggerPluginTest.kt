package com.zegreatrob.tools.tagger

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaggerPluginTest {
    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.zegreatrob.tools.tagger")
        assertNotNull(project.tasks.findByName("calculateVersion"))
    }

    @Test
    fun tagMustRunAfterAllChecksInMultiProject() {
        val rootProject =
            ProjectBuilder.builder()
                .build()
        val innerProject1 =
            ProjectBuilder.builder()
                .withParent(rootProject)
                .withName("p1")
                .build()
        ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("p2")
            .build()
        rootProject.plugins.apply("com.zegreatrob.tools.tagger")
        val rootCheck = rootProject.tasks.named("check").get()
        val innerProject1Check = innerProject1.tasks.register("check")

        val tagTask = rootProject.tasks.findByName("tag")!!

        assertTrue(
            tagTask.mustRunAfter.getDependencies(tagTask)
                .contains(rootCheck),
            "Did not run after root check",
        )
        assertTrue(
            tagTask.mustRunAfter.getDependencies(tagTask)
                .contains(innerProject1Check.get()),
            "Did not run after inner project check",
        )
    }
}
