package com.zegreatrob.tools.digger

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream

abstract class AllContributionData : DefaultTask() {

    @Internal
    lateinit var diggerExtension: DiggerExtension

    @Input
    var exportToGithubEnv: Boolean = false

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val output = JsonOutput.toJson(
            diggerExtension.allContributionData(),
        )

        val githubEnvFile = System.getenv("GITHUB_ENV")
        if (exportToGithubEnv && githubEnvFile != null) {
            FileOutputStream(githubEnvFile, true)
                .write("DIGGER_ALL_CONTRIBUTION_DATA=$output".toByteArray())
        } else {
            outputFile.get().asFile.writeText(output)
        }
    }
}
