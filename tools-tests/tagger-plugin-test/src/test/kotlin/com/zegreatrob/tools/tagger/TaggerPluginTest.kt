package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.minassert.assertIsNotEqualTo
import com.zegreatrob.testmints.setup
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

class TaggerPluginTest {
    @Test
    fun `plugin registers task`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
    } verify {
        project.tasks.findByName("calculateVersion")
            .assertIsNotEqualTo(null, "Expected calculateVersion task to be registered")
    }

    @Test
    fun tagMustRunAfterAllChecksInMultiProject() = setup(object {
        val rootProject = ProjectBuilder.builder().build()
        val innerProject1 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("p1")
            .build()
        val innerProject2 = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("p2")
            .build()
    }) exercise {
        rootProject.plugins.apply("com.zegreatrob.tools.tagger")
    } verify {
        val rootCheck = rootProject.tasks.named("check").get()
        val innerProject1Check = innerProject1.tasks.register("check")
        val innerProject2Check = innerProject2.tasks.register("check")
        val tagTask = rootProject.tasks.findByName("tag")!!

        tagTask.mustRunAfter.getDependencies(tagTask)
            .contains(rootCheck)
            .assertIsEqualTo(true, "Did not run after root check")
        tagTask.mustRunAfter.getDependencies(tagTask)
            .contains(innerProject1Check.get())
            .assertIsEqualTo(true, "Did not run after inner project check")
        tagTask.mustRunAfter.getDependencies(tagTask)
            .contains(innerProject2Check.get())
            .assertIsEqualTo(true, "Did not run after second inner project check")
    }

    @Test
    fun `tag task annotates git inputs correctly`() = setup(object {
        val workingDirectoryMethod = TagVersion::class.java.getMethod("getWorkingDirectory")
        val gitDirectoryMethod = TagVersion::class.java.getMethod("getGitDirectory")
    }) exercise {} verify {
        workingDirectoryMethod.isAnnotationPresent(Internal::class.java)
            .assertIsEqualTo(true, "Expected workingDirectory to be internal.")
        workingDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(false, "Expected workingDirectory not to be an input.")
        gitDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(true, "Expected gitDirectory to be an input.")
    }

    @Test
    fun `release task annotates git inputs correctly`() = setup(object {
        val workingDirectoryMethod = ReleaseVersion::class.java.getMethod("getWorkingDirectory")
        val gitDirectoryMethod = ReleaseVersion::class.java.getMethod("getGitDirectory")
    }) exercise {} verify {
        workingDirectoryMethod.isAnnotationPresent(Internal::class.java)
            .assertIsEqualTo(true, "Expected workingDirectory to be internal.")
        workingDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(false, "Expected workingDirectory not to be an input.")
        gitDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(true, "Expected gitDirectory to be an input.")
    }

    @Test
    fun `calculate version task annotates git inputs correctly`() = setup(object {
        val workingDirectoryMethod = CalculateVersion::class.java.getMethod("getWorkingDirectory")
        val gitDirectoryMethod = CalculateVersion::class.java.getMethod("getGitDirectory")
    }) exercise {} verify {
        workingDirectoryMethod.isAnnotationPresent(Internal::class.java)
            .assertIsEqualTo(true, "Expected workingDirectory to be internal.")
        workingDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(false, "Expected workingDirectory not to be an input.")
        gitDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(true, "Expected gitDirectory to be an input.")
    }

    @Test
    fun `commit report task annotates git inputs correctly`() = setup(object {
        val workingDirectoryMethod = CommitReport::class.java.getMethod("getWorkingDirectory")
        val gitDirectoryMethod = CommitReport::class.java.getMethod("getGitDirectory")
    }) exercise {} verify {
        workingDirectoryMethod.isAnnotationPresent(Internal::class.java)
            .assertIsEqualTo(true, "Expected workingDirectory to be internal.")
        workingDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(false, "Expected workingDirectory not to be an input.")
        gitDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(true, "Expected gitDirectory to be an input.")
    }

    @Test
    fun `previous version task annotates git inputs correctly`() = setup(object {
        val workingDirectoryMethod = PreviousVersion::class.java.getMethod("getWorkingDirectory")
        val gitDirectoryMethod = PreviousVersion::class.java.getMethod("getGitDirectory")
    }) exercise {} verify {
        workingDirectoryMethod.isAnnotationPresent(Internal::class.java)
            .assertIsEqualTo(true, "Expected workingDirectory to be internal.")
        workingDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(false, "Expected workingDirectory not to be an input.")
        gitDirectoryMethod.isAnnotationPresent(InputDirectory::class.java)
            .assertIsEqualTo(true, "Expected gitDirectory to be an input.")
    }

    @Test
    fun `extension allowDetachedHead defaults to null`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.extensions.getByType(TaggerExtension::class.java)
    } verify { extension ->
        extension.allowDetachedHead
            .assertIsEqualTo(null, "Expected allowDetachedHead default value to be null")
    }

    @Test
    fun `extension allowDetachedHead can be set to true`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val extension = project.extensions.getByType(TaggerExtension::class.java)
        extension.allowDetachedHead = true
        extension
    } verify { extension ->
        extension.allowDetachedHead
            .assertIsEqualTo(true, "Expected allowDetachedHead to be settable")
    }

    @Test
    fun `calculateVersion task has allowDetachedHead property`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.tasks.findByName("calculateVersion") as CalculateVersion
    } verify { task ->
        task.allowDetachedHead
            .assertIsNotEqualTo(null, "Expected allowDetachedHead property to exist on CalculateVersion task")
    }

    @Test
    fun `tag task has allowDetachedHead property`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.tasks.findByName("tag") as TagVersion
    } verify { task ->
        task.allowDetachedHead
            .assertIsNotEqualTo(null, "Expected allowDetachedHead property to exist on TagVersion task")
    }

    @Test
    fun `when extension allowDetachedHead is set, tasks receive the value`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val extension = project.extensions.getByType(TaggerExtension::class.java)
        extension.allowDetachedHead = true
        Pair(
            project.tasks.findByName("calculateVersion") as CalculateVersion,
            project.tasks.findByName("tag") as TagVersion,
        )
    } verify { (calculateVersion, tag) ->
        calculateVersion.allowDetachedHead
            .orNull
            .assertIsEqualTo(true, "Expected calculateVersion to receive allowDetachedHead=true from extension")
        tag.allowDetachedHead
            .orNull
            .assertIsEqualTo(true, "Expected tag to receive allowDetachedHead=true from extension")
    }

    @Test
    fun `plugin registers taggerGuide task`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
    } verify {
        project.tasks.findByName("taggerGuide")
            .assertIsNotEqualTo(null, "Expected taggerGuide task to be registered")
    }
}
