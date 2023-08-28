package com.zegreatrob.tools

import com.zegreatrob.tools.digger.DiggerExtension
import com.zegreatrob.tools.digger.DiggerGitDescription

plugins {
    id("org.ajoberstar.grgit.service")
    base
}

val digger = project.extensions.create("digger", DiggerExtension::class, grgitService)

tasks {
    val exportToGithub = project.findProperty("exportToGithub")
    val diggerGitDescription by registering(DiggerGitDescription::class) {
        this.diggerExtension = digger
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
}
