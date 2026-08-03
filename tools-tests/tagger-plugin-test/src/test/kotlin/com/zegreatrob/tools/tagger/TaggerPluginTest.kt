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

    @Test
    fun `taggerGuide task loads content from markdown resource`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val task = project.tasks.findByName("taggerGuide") as TaggerGuideTask
        task.getGuideContent()
    } verify { content ->
        content.contains("Use Tagger when:")
            .assertIsEqualTo(true, "Expected guide content to contain CLI guide text")
    }

    @Test
    fun `release task depends on tag task to ensure tag created before publication`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.tasks.findByName("release")
    } verify { releaseTask ->
        val tagTask = project.tasks.findByName("tag")!!
        releaseTask!!.taskDependencies.getDependencies(releaseTask)
            .contains(tagTask)
            .assertIsEqualTo(true, "Expected release task to depend on tag task")
    }

    @Test
    fun `githubRelease task uses gh release create with idempotency check`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.tasks.findByName("githubRelease") as org.gradle.api.tasks.Exec
    } verify { task ->
        val commandLine = task.commandLine
        val script = commandLine.joinToString(" ")
        script.contains("gh release view")
            .assertIsEqualTo(true, "Expected idempotency check with 'gh release view'")
        script.contains("gh release create")
            .assertIsEqualTo(true, "Expected 'gh release create' command")
        script.contains("already exists, skipping creation")
            .assertIsEqualTo(true, "Expected skip message when release exists")
    }

    @Test
    fun `githubRelease task does not include generate_release_notes parameter`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.tasks.findByName("githubRelease") as org.gradle.api.tasks.Exec
    } verify { task ->
        val commandLine = task.commandLine
        val script = commandLine.joinToString(" ")
        script.contains("generate_release_notes")
            .assertIsEqualTo(false, "Expected no generate_release_notes parameter with gh release create")
    }

    @Test
    fun `githubRelease task publishes immediately by default when githubReleaseDraft not set`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.tasks.findByName("githubRelease") as org.gradle.api.tasks.Exec
    } verify { task ->
        val commandLine = task.commandLine
        val script = commandLine.joinToString(" ")
        script.contains("--draft")
            .assertIsEqualTo(false, "Expected no --draft flag when githubReleaseDraft not explicitly set (default false)")
        script.contains("gh release create")
            .assertIsEqualTo(true, "Expected 'gh release create' command to still be present")
    }

    @Test
    fun `githubRelease task creates draft when githubReleaseDraft set to true`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val tagger = project.extensions.getByType(TaggerExtension::class.java)
        tagger.githubReleaseDraft.set(true)
        project.tasks.findByName("githubRelease") as org.gradle.api.tasks.Exec
    } verify { task ->
        val commandLine = task.commandLine
        val script = commandLine.joinToString(" ")
        script.contains("--draft")
            .assertIsEqualTo(true, "Expected --draft flag when githubReleaseDraft.set(true)")
    }

    @Test
    fun `tag task does not have mustRunAfter relationship with publish to avoid circular dependency`() = setup(object {
        val rootProject = ProjectBuilder.builder().build()
        val innerProject = ProjectBuilder.builder()
            .withParent(rootProject)
            .withName("p1")
            .build()
    }) exercise {
        rootProject.plugins.apply("com.zegreatrob.tools.tagger")
        innerProject.tasks.register("publish")
        val tagTask = rootProject.tasks.findByName("tag")!!
        tagTask.mustRunAfter.getDependencies(tagTask)
    } verify { mustRunAfterDeps ->
        val publishTask = innerProject.tasks.findByName("publish")!!
        mustRunAfterDeps.contains(publishTask)
            .assertIsEqualTo(false, "tag must not run after publish to avoid circular dependency (release depends on tag, publish runs after release)")
    }

    @Test
    fun `extension showCommands defaults to false`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.extensions.getByType(TaggerExtension::class.java)
    } verify { extension ->
        extension.showCommands
            .get()
            .assertIsEqualTo(false, "Expected showCommands default value to be false")
    }

    @Test
    fun `extension showCommands can be set to true`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val extension = project.extensions.getByType(TaggerExtension::class.java)
        extension.showCommands.set(true)
        extension
    } verify { extension ->
        extension.showCommands
            .get()
            .assertIsEqualTo(true, "Expected showCommands to be settable")
    }

    @Test
    fun `extension githubReleaseAssets property exists and defaults to empty collection`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.extensions.getByType(TaggerExtension::class.java)
    } verify { extension ->
        extension.githubReleaseAssets
            .assertIsNotEqualTo(null, "Expected githubReleaseAssets property to exist")
        extension.githubReleaseAssets.isEmpty
            .assertIsEqualTo(true, "Expected githubReleaseAssets to default to empty collection")
    }

    @Test
    fun `extension githubReleaseAssets can be configured with files`() = setup(object {
        val project = ProjectBuilder.builder().build()
        val testFile = java.io.File.createTempFile("test", ".txt")
    }) {
        testFile.deleteOnExit()
        testFile.writeText("test content")
    } exercise {
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val extension = project.extensions.getByType(TaggerExtension::class.java)
        extension.githubReleaseAssets.from(testFile)
        extension.githubReleaseAssets.files
    } verify { files ->
        files.size
            .assertIsEqualTo(1, "Expected one file in githubReleaseAssets")
        files.first().name
            .assertIsEqualTo(testFile.name, "Expected configured file to be in collection")
    }

    @Test
    fun `plugin registers githubReleaseUpload task`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
    } verify {
        project.tasks.findByName("githubReleaseUpload")
            .assertIsNotEqualTo(null, "Expected githubReleaseUpload task to be registered")
    }

    @Test
    fun `githubReleaseUpload task disabled for SNAPSHOT versions`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3-SNAPSHOT"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val tagger = project.extensions.getByType(TaggerExtension::class.java)
        tagger.githubReleaseEnabled.set(true)
        project.tasks.findByName("githubReleaseUpload") as org.gradle.api.tasks.Exec
    } verify { task ->
        task.enabled
            .assertIsEqualTo(false, "Expected githubReleaseUpload to be disabled for SNAPSHOT versions")
    }

    @Test
    fun `githubReleaseUpload task disabled when githubReleaseEnabled is false`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val tagger = project.extensions.getByType(TaggerExtension::class.java)
        tagger.githubReleaseEnabled.set(false)
        project.tasks.findByName("githubReleaseUpload") as org.gradle.api.tasks.Exec
    } verify { task ->
        task.enabled
            .assertIsEqualTo(false, "Expected githubReleaseUpload to be disabled when githubReleaseEnabled is false")
    }

    @Test
    fun `githubReleaseUpload task disabled when githubReleaseAssets is empty`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val tagger = project.extensions.getByType(TaggerExtension::class.java)
        tagger.githubReleaseEnabled.set(true)
        project.tasks.findByName("githubReleaseUpload") as org.gradle.api.tasks.Exec
    } verify { task ->
        task.enabled
            .assertIsEqualTo(false, "Expected githubReleaseUpload to be disabled when no assets configured")
    }

    @Test
    fun `githubReleaseUpload task enabled when conditions met`() = setup(object {
        val project = ProjectBuilder.builder().build()
        val testFile = java.io.File.createTempFile("test", ".txt")
    }) {
        testFile.deleteOnExit()
    } exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        val tagger = project.extensions.getByType(TaggerExtension::class.java)
        tagger.githubReleaseEnabled.set(true)
        tagger.githubReleaseAssets.from(testFile)
        project.tasks.findByName("githubReleaseUpload") as org.gradle.api.tasks.Exec
    } verify { task ->
        task.enabled
            .assertIsEqualTo(true, "Expected githubReleaseUpload to be enabled when version is not SNAPSHOT, githubReleaseEnabled is true, and assets are configured")
    }

    @Test
    fun `githubReleaseUpload task depends on githubRelease`() = setup(object {
        val project = ProjectBuilder.builder().build()
    }) exercise {
        project.version = "1.2.3"
        project.plugins.apply("com.zegreatrob.tools.tagger")
        project.tasks.findByName("githubReleaseUpload")
    } verify { uploadTask ->
        val githubReleaseTask = project.tasks.findByName("githubRelease")!!
        uploadTask!!.taskDependencies.getDependencies(uploadTask)
            .contains(githubReleaseTask)
            .assertIsEqualTo(true, "Expected githubReleaseUpload task to depend on githubRelease task")
    }
}
