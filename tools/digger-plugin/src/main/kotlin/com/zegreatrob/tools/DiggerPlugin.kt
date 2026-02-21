package com.zegreatrob.tools

import com.zegreatrob.tools.digger.AllContributionData
import com.zegreatrob.tools.digger.CurrentContributionData
import com.zegreatrob.tools.digger.DiggerExtension
import com.zegreatrob.tools.digger.HeadTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

class DiggerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("base")

        val digger = project.extensions.create("digger", DiggerExtension::class.java)
        digger.label.convention(project.name)
        digger.workingDirectory.convention(project.rootDir)

        val exportToGithub = project.findProperty("exportToGithub")
        val diggerBuildDirectory: Provider<Directory> = project.layout.buildDirectory.dir("digger")

        val gitHead = project.tasks.register("gitHead", HeadTask::class.java) { task ->
            task.diggerExtension = digger
            task.outputFile.set(diggerBuildDirectory.map { it.file("head") })
        }

        project.tasks.register("currentContributionData", CurrentContributionData::class.java) { task ->
            task.diggerExtension = digger
            task.dependsOn(gitHead)
            task.inputs.file(gitHead.map { it.outputFile })
            task.outputFile.set(project.layout.buildDirectory.file("digger/current.json"))
            if (exportToGithub != null) {
                task.exportToGithubEnv = true
            }
        }

        project.tasks.register("allContributionData", AllContributionData::class.java) { task ->
            task.diggerExtension = digger
            task.dependsOn(gitHead)
            task.inputs.file(gitHead.map { it.outputFile })
            task.outputFile.set(project.layout.buildDirectory.file("digger/all.json"))
            if (exportToGithub != null) {
                task.exportToGithubEnv = true
            }
        }
    }
}
