package com.zegreatrob.tools

import com.zegreatrob.tools.digger.DiggerExtension
import com.zegreatrob.tools.digger.ListCoAuthorEmails

plugins {
    id("org.ajoberstar.grgit.service")
    base
}

val digger = project.extensions.create("digger", DiggerExtension::class, grgitService)

tasks {
    val exportToGithub = project.findProperty("exportToGithub")
    val listCoAuthorEmails by registering(ListCoAuthorEmails::class) {
        this.diggerExtension = digger
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
}
