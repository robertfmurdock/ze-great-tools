package com.zegreatrob.tools

import com.zegreatrob.tools.tagger.CalculateVersion
import com.zegreatrob.tools.tagger.CommitReport
import com.zegreatrob.tools.tagger.ReleaseVersion
import com.zegreatrob.tools.tagger.TagVersion
import com.zegreatrob.tools.tagger.TaggerExtension

plugins {
    id("org.ajoberstar.grgit.service")
    base
}

val tagger = project.extensions.create("tagger", TaggerExtension::class, grgitService, project)

tasks {
    val exportToGithub = project.findProperty("exportToGithub")
    val calculateVersion by registering(CalculateVersion::class) {
        taggerExtension = tagger
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
    register("commitReport", CommitReport::class) {
        taggerExtension = tagger
    }

    val githubRelease by registering(Exec::class) {
        enabled = tagger.githubReleaseEnabled.get()
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
            "tag_name=${tagger.version}",
            "-f",
            "name=${tagger.version}",
            "-f",
            "body=${tagger.version}",
            "-F",
            "draft=false",
            "-F",
            "prerelease=false",
            "-F",
            "generate_release_notes=false",
        )
    }

    register("release", ReleaseVersion::class) {
        taggerExtension = tagger
        enabled = !taggerExtension.isSnapshot
        dependsOn(assemble)
        mustRunAfter(check)
        finalizedBy(tag, githubRelease)
        finalizedBy(provider { (getTasksByName("publish", true)).toList() })
    }
}
