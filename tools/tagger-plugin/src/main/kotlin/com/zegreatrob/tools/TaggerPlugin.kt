package com.zegreatrob.tools

import com.zegreatrob.tools.tagger.CalculateVersion
import com.zegreatrob.tools.tagger.CommitReport
import com.zegreatrob.tools.tagger.PreviousVersion
import com.zegreatrob.tools.tagger.ReleaseVersion
import com.zegreatrob.tools.tagger.TagVersion
import com.zegreatrob.tools.tagger.TaggerExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec

class TaggerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("base")
        val tagger = createTaggerExtension(project)
        val exportToGithub = project.findProperty("exportToGithub")
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

    private fun registerPreviousVersionTask(project: Project, tagger: TaggerExtension) {
        project.tasks.register("previousVersion", PreviousVersion::class.java) { task ->
            task.group = "versioning"
            task.description = "Read-only: report the most recent tagged version"
            task.workingDirectory.set(tagger.workingDirectory)
            task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
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
        task.version = "${project.version}"
        task.mustRunAfter(project.tasks.named("check"))
        task.mustRunAfter(project.provider { project.getTasksByName("check", true).toList() })
        task.mustRunAfter(
            project.provider { project.getTasksByName("publish", true).toList() },
            project.provider { project.getTasksByName("publish", true).map { it.finalizedBy }.toList() },
        )
    }

    private fun registerCommitReportTask(project: Project, tagger: TaggerExtension) {
        project.tasks.register("commitReport", CommitReport::class.java) { task ->
            task.group = "versioning"
            task.description = "Read-only: report semver signals in recent commit messages"
            task.workingDirectory.set(tagger.workingDirectory)
            task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
        }
    }

    private fun registerGithubReleaseTask(project: Project, tagger: TaggerExtension, tag: Any) = project.tasks.register("githubRelease", Exec::class.java) { task ->
        task.group = "versioning"
        task.description = "Side effect: create GitHub release via gh CLI. Requires tag to run first. Disabled for -SNAPSHOT versions."
        task.enabled = !project.version.toString().contains("SNAPSHOT") && tagger.githubReleaseEnabled.get()
        task.dependsOn(tag)
        val githubRepository = System.getenv("GITHUB_REPOSITORY")
        task.commandLine(
            "gh",
            "api",
            "--method",
            "POST",
            "-H",
            "Accept: application/vnd.github+json",
            "-H",
            "X-GitHub-Api-Version: 2022-11-28",
            "/repos/$githubRepository/releases",
            "-f",
            "tag_name=${project.version}",
            "-f",
            "name=${project.version}",
            "-f",
            "body=${project.version}",
            "-F",
            "draft=false",
            "-F",
            "prerelease=false",
            "-F",
            "generate_release_notes=false",
        )
    }

    private fun registerReleaseTask(project: Project, tagger: TaggerExtension, tag: Any, githubRelease: Any) {
        project.tasks.register("release", ReleaseVersion::class.java) { task ->
            task.group = "versioning"
            task.description = "Orchestrator: assemble, then tag, optionally publish and create GitHub release. Disabled for -SNAPSHOT versions."
            task.workingDirectory.set(tagger.workingDirectory)
            task.gitDirectory.set(tagger.workingDirectory.dir(".git"))
            task.releaseBranch.set(tagger.releaseBranchProperty)
            task.version = "${project.version}"
            task.enabled = !project.version.toString().contains("SNAPSHOT")
            task.dependsOn(project.tasks.named("assemble"))
            task.mustRunAfter(project.tasks.named("check"))
            task.finalizedBy(tag, githubRelease)
            task.finalizedBy(project.provider { project.getTasksByName("publish", true).toList() })
        }
    }
}
