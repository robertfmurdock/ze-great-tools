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

val tagger = project.extensions.create("tools", TaggerExtension::class, grgitService, project)

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
    }

    val tag by registering(TagVersion::class) {
        taggerExtension = tagger
    }
    register("commitReport", CommitReport::class) {
        taggerExtension = tagger
    }

    register("release", ReleaseVersion::class) {
        taggerExtension = tagger
        dependsOn(assemble)
        mustRunAfter(check)
        finalizedBy(tag)
    }
}
