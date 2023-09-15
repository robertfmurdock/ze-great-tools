package com.zegreatrob.tools

import com.zegreatrob.tools.digger.AllContributionData
import com.zegreatrob.tools.digger.CurrentContributionData
import com.zegreatrob.tools.digger.DiggerExtension
import com.zegreatrob.tools.digger.HeadTask

plugins {
    id("org.ajoberstar.grgit.service")
    base
}

val digger = project.extensions.create("digger", DiggerExtension::class, grgitService)

digger.label.convention(project.name)

val exportToGithub = project.findProperty("exportToGithub")
val diggerBuildDirectory: Provider<Directory> = layout.buildDirectory.dir("digger")

logger.warn("The 'digger' gradle plugin is current experimental. Be warned each update may make breaking changes.")

tasks {
    val gitHead by registering(HeadTask::class) {
        this.diggerExtension = digger
        outputFile.set(diggerBuildDirectory.map { it.file("head") })
    }
    val currentContributionData by registering(CurrentContributionData::class) {
        this.diggerExtension = digger
        dependsOn(gitHead)
        inputs.file(gitHead.map { it.outputFile })
        outputFile.set(layout.buildDirectory.file("digger/current.json"))
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
    val allContributionData by registering(AllContributionData::class) {
        this.diggerExtension = digger
        dependsOn(gitHead)
        inputs.file(gitHead.map { it.outputFile })
        outputFile.set(layout.buildDirectory.file("digger/all.json"))
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
}
