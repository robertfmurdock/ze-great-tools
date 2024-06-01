package com.zegreatrob.tools

import com.zegreatrob.tools.tagger.CalculateVersion
import com.zegreatrob.tools.tagger.CommitReport
import com.zegreatrob.tools.tagger.PreviousVersion
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
    val previousVersion by registering(PreviousVersion::class) {
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
        this.taggerExtension = tagger
    }

    val githubRelease by registering(Exec::class) {
        enabled = with(tagger) { !isSnapshot && githubReleaseEnabled.get() }
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

    register<ReleaseVersion>("release") {
        taggerExtension = tagger
        enabled = !tagger.isSnapshot
        dependsOn(assemble)
        mustRunAfter(check)
        finalizedBy(tag, githubRelease)
        finalizedBy(provider { (getTasksByName("publish", true)).toList() })
    }
}
