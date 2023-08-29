package com.zegreatrob.tools

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
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
}
