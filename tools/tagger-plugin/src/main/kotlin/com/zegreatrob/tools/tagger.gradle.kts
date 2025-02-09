package com.zegreatrob.tools

import com.zegreatrob.tools.tagger.CalculateVersion
import com.zegreatrob.tools.tagger.CommitReport
import com.zegreatrob.tools.tagger.PreviousVersion
import com.zegreatrob.tools.tagger.ReleaseVersion
import com.zegreatrob.tools.tagger.TagVersion
import com.zegreatrob.tools.tagger.TaggerExtension

plugins {
    base
}

val tagger = project.extensions.create("tagger", TaggerExtension::class, project)

tagger.workingDirectory.convention(project.rootDir)

tasks {
    val exportToGithub = project.findProperty("exportToGithub")
    register<PreviousVersion>("previousVersion") {
        this.taggerExtension = tagger
    }
    val calculateVersion by registering(CalculateVersion::class) {
        this.taggerExtension = tagger
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
    check {
        dependsOn(calculateVersion)
        dependsOn(
            provider { (project.getTasksByName("check", true) - check.get()).toList() },
        )
    }

    val tag by registering(TagVersion::class) {
        taggerExtension = tagger
        version = "${project.version}"
        mustRunAfter(check)

        mustRunAfter(
            provider { (project.getTasksByName("check", true)).toList() },
        )
        mustRunAfter(
            provider {
                project.getTasksByName("publish", true).toList()
            },
            provider {
                project.getTasksByName("publish", true).map { it.finalizedBy }.toList()
            },
        )
    }
    register<CommitReport>("commitReport") {
        taggerExtension = tagger
    }

    val githubRelease by registering(Exec::class) {
        enabled = !version.toString().contains("SNAPSHOT") && tagger.githubReleaseEnabled.get()
        dependsOn(tag)
        val githubRepository = System.getenv("GITHUB_REPOSITORY")
        commandLine(
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

    register<ReleaseVersion>("release") {
        taggerExtension = tagger
        version = "${project.version}"
        enabled = !project.version.toString().contains("SNAPSHOT")
        dependsOn(assemble)
        mustRunAfter(check)
        finalizedBy(tag, githubRelease)
        finalizedBy(provider { (getTasksByName("publish", true)).toList() })
    }
}
