package com.zegreatrob.tools

import com.zegreatrob.tools.digger.AllContributionData
import com.zegreatrob.tools.digger.CurrentContributionData
import com.zegreatrob.tools.digger.DiggerExtension
import com.zegreatrob.tools.digger.HeadTask

plugins {
    base
}

val digger = project.extensions.create("digger", DiggerExtension::class)

digger.label.convention(project.name)
digger.workingDirectory.convention(project.rootDir)

val exportToGithub = project.findProperty("exportToGithub")
val diggerBuildDirectory: Provider<Directory> = layout.buildDirectory.dir("digger")

tasks {
    val gitHead by registering(HeadTask::class) {
        this.diggerExtension = digger
        outputFile.set(diggerBuildDirectory.map { it.file("head") })
    }
    register<CurrentContributionData>("currentContributionData") {
        this.diggerExtension = digger
        dependsOn(gitHead)
        inputs.file(gitHead.map { it.outputFile })
        outputFile.set(layout.buildDirectory.file("digger/current.json"))
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
    register<AllContributionData>("allContributionData") {
        this.diggerExtension = digger
        dependsOn(gitHead)
        inputs.file(gitHead.map { it.outputFile })
        outputFile.set(layout.buildDirectory.file("digger/all.json"))
        exportToGithub?.let {
            exportToGithubEnv = true
        }
    }
}
