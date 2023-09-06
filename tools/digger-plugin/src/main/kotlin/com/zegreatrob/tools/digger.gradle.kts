package com.zegreatrob.tools

import com.zegreatrob.tools.digger.AllContributionData
import com.zegreatrob.tools.digger.CurrentContributionData
import com.zegreatrob.tools.digger.DiggerExtension

plugins {
    id("org.ajoberstar.grgit.service")
    base
}

val digger = project.extensions.create("digger", DiggerExtension::class, grgitService)

tasks {
    val exportToGithub = project.findProperty("exportToGithub")
    val currentContributionData by registering(CurrentContributionData::class) {
        this.diggerExtension = digger

        inputs.property("GIT_HEAD", digger.headId())

        outputFile.set(layout.buildDirectory.file("digger/current.json"))
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
    val allContributionData by registering(AllContributionData::class) {
        this.diggerExtension = digger
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
}
