package com.zegreatrob.tools

import com.zegreatrob.tools.tagger.CalculateVersion
import com.zegreatrob.tools.tagger.CommitReport
import com.zegreatrob.tools.tagger.PreviousVersion
import com.zegreatrob.tools.tagger.ReleaseVersion
import com.zegreatrob.tools.tagger.TagVersion
import com.zegreatrob.tools.tagger.TaggerExtension
import com.zegreatrob.tools.tagger.TaggerGuideTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec

class TaggerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("base")
        val tagger = createTaggerExtension(project)
        val exportToGithub = project.findProperty("exportToGithub")
        registerTaggerGuideTask(project)
        registerPreviousVersionTask(project, tagger)
        registerCalculateVersionTask(project, tagger, exportToGithub)
        val tag = registerTagTask(project, tagger)
        registerCommitReportTask(project, tagger)
        val githubRelease = registerGithubReleaseTask(project, tagger, tag)
        registerReleaseTask(project, tagger, tag, githubRelease)
    }

    private fun createTaggerExtension(project: Project): TaggerExtension {
        val tagger = project.extensions.create("tagger", TaggerExtension::class.java, project.objects)
        tagger.workingDirectory.convention(project.layout.projectDirectory)
        project.findProperty("taggerForceSnapshot")
            ?.toString()
            ?.toBooleanStrictOrNull()
            ?.let { tagger.forceSnapshot.set(it) }
        return tagger
    }

    private fun registerTaggerGuideTask(project: Project) {
        project.tasks.register("taggerGuide", TaggerGuideTask::class.java) { task ->
            task.group = "help"
            task.description = "Display comprehensive usage guide and best practices"
        }
    }

    private fun registerPreviousVersionTask(project: Project, tagger: TaggerExtension) {
        project.tasks.register("previousVersion", PreviousVersion::class.java) { task ->
            task.group = "versioning"
            task.description = "Read-only: report the most recent tagged version"
            task.workingDirectory.set(tagger.workingDirectory)
            task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
            task.showCommands.set(tagger.showCommands)
        }
    }

    private fun registerCalculateVersionTask(project: Project, tagger: TaggerExtension, exportToGithub: Any?) {
        project.tasks.register("calculateVersion", CalculateVersion::class.java) { task ->
            task.group = "versioning"
            task.description = "Read-only: calculate next version from commit history without tagging. Check snapshot == false before tagging."
            task.workingDirectory.set(tagger.workingDirectory)
            task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
            task.releaseBranch.set(tagger.releaseBranchProperty)
            task.implicitPatch.set(tagger.implicitPatch)
            tagger.allowDetachedHeadProperty.orNull?.let { task.allowDetachedHead.set(it) }
            task.forceSnapshot.set(tagger.forceSnapshot)
            task.versionRegex.set(tagger.versionRegex)
            task.noneRegex.set(tagger.noneRegex)
            task.patchRegex.set(tagger.patchRegex)
            task.minorRegex.set(tagger.minorRegex)
            task.majorRegex.set(tagger.majorRegex)
            task.exportToGithubEnv.set(exportToGithub != null)
            task.warningsAsErrors.set(tagger.warningsAsErrors)
            task.showCommands.set(tagger.showCommands)
        }
    }

    private fun registerTagTask(project: Project, tagger: TaggerExtension) = project.tasks.register("tag", TagVersion::class.java) { task ->
        task.group = "versioning"
        task.description = "Side effect: create annotated Git tag at project.version. Only run after calculateVersion confirms snapshot == false."
        task.workingDirectory.set(tagger.workingDirectory)
        task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
        task.releaseBranch.set(tagger.releaseBranchProperty)
        task.userName.set(tagger.userNameProperty)
        task.userEmail.set(tagger.userEmailProperty)
        task.warningsAsErrors.set(tagger.warningsAsErrors)
        tagger.allowDetachedHeadProperty.orNull?.let { task.allowDetachedHead.set(it) }
        task.showCommands.set(tagger.showCommands)
        task.version = "${project.version}"
        task.mustRunAfter(project.tasks.named("check"))
        task.mustRunAfter(project.provider { project.getTasksByName("check", true).toList() })
    }

    private fun registerCommitReportTask(project: Project, tagger: TaggerExtension) {
        project.tasks.register("commitReport", CommitReport::class.java) { task ->
            task.group = "versioning"
            task.description = "Read-only: report semver signals in recent commit messages"
            task.workingDirectory.set(tagger.workingDirectory)
            task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
            task.showCommands.set(tagger.showCommands)
        }
    }

    private fun registerGithubReleaseTask(
        project: Project,
        tagger: TaggerExtension,
        tag: Any,
    ) = project.tasks.register("githubRelease", Exec::class.java) { task ->
        task.group = "versioning"
        task.description =
            "Side effect: create GitHub release (draft by default) via gh CLI. Requires tag to run first. Disabled for -SNAPSHOT versions. Idempotent - skips if release exists."
        task.enabled = !project.version.toString().contains("SNAPSHOT") && tagger.githubReleaseEnabled.get()
        task.dependsOn(tag)
        task.commandLine("sh", "-c", draftReleaseScript(project.version, tagger.githubReleaseDraft.get()))
    }

    private fun draftReleaseScript(version: Any, draft: Boolean) = """
        if gh release view $version >/dev/null 2>&1; then
            echo "Release $version already exists, skipping creation"
        else
            gh release create $version ${if (draft) "--draft " else ""}--title $version --notes $version
        fi
    """.trimIndent()

    private fun registerReleaseTask(project: Project, tagger: TaggerExtension, tag: Any, githubRelease: Any) {
        project.tasks.register("release", ReleaseVersion::class.java) { task ->
            task.group = "versioning"
            task.description =
                "Orchestrator: assemble, then tag, optionally publish and create GitHub release. Disabled for -SNAPSHOT versions."
            configureReleaseTask(task, project, tagger, tag, githubRelease)
        }
    }

    private fun configureReleaseTask(
        task: ReleaseVersion,
        project: Project,
        tagger: TaggerExtension,
        tag: Any,
        githubRelease: Any,
    ) {
        task.workingDirectory.set(tagger.workingDirectory)
        task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
        task.releaseBranch.set(tagger.releaseBranchProperty)
        task.showCommands.set(tagger.showCommands)
        task.version = "${project.version}"
        task.enabled = !project.version.toString().contains("SNAPSHOT")
        task.dependsOn(project.tasks.named("assemble"))
        task.mustRunAfter(project.tasks.named("check"))
        task.dependsOn(tag)
        task.finalizedBy(githubRelease)
        task.finalizedBy(project.provider { project.getTasksByName("publish", true).toList() })
    }
}
