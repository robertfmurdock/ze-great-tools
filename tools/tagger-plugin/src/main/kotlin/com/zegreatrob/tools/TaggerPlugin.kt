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

        val tagger = project.extensions.create("tagger", TaggerExtension::class.java, project.objects)
        tagger.workingDirectory.convention(project.layout.projectDirectory)

        project.findProperty("taggerForceSnapshot")
            ?.toString()
            ?.toBooleanStrictOrNull()
            ?.let { tagger.forceSnapshot.set(it) }

        val exportToGithub = project.findProperty("exportToGithub")

        project.tasks.register("previousVersion", PreviousVersion::class.java) { task ->
            task.workingDirectory.set(tagger.workingDirectory)
        }
        project.tasks.register("calculateVersion", CalculateVersion::class.java) { task ->
            task.workingDirectory.set(tagger.workingDirectory)
            task.releaseBranch.set(tagger.releaseBranchProperty)
            task.implicitPatch.set(tagger.implicitPatch)
            task.disableDetached.set(tagger.disableDetached)
            task.forceSnapshot.set(tagger.forceSnapshot)
            task.versionRegex.set(tagger.versionRegex)
            task.noneRegex.set(tagger.noneRegex)
            task.patchRegex.set(tagger.patchRegex)
            task.minorRegex.set(tagger.minorRegex)
            task.majorRegex.set(tagger.majorRegex)
            task.exportToGithubEnv.set(exportToGithub != null)
        }

        val tag = project.tasks.register("tag", TagVersion::class.java) { task ->
            task.workingDirectory.set(tagger.workingDirectory)
            task.releaseBranch.set(tagger.releaseBranchProperty)
            task.userName.set(tagger.userNameProperty)
            task.userEmail.set(tagger.userEmailProperty)
            task.warningsAsErrors.set(tagger.warningsAsErrors)
            task.version = "${project.version}"
            task.mustRunAfter(project.tasks.named("check"))
            task.mustRunAfter(project.provider { project.getTasksByName("check", true).toList() })
            task.mustRunAfter(
                project.provider { project.getTasksByName("publish", true).toList() },
                project.provider { project.getTasksByName("publish", true).map { it.finalizedBy }.toList() },
            )
        }

        project.tasks.register("commitReport", CommitReport::class.java) { task ->
            task.workingDirectory.set(tagger.workingDirectory)
        }

        val githubRelease = project.tasks.register("githubRelease", Exec::class.java) { task ->
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

        project.tasks.register("release", ReleaseVersion::class.java) { task ->
            task.workingDirectory.set(tagger.workingDirectory)
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
