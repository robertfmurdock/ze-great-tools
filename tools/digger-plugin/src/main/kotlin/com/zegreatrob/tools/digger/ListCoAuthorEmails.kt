package com.zegreatrob.tools.digger

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream

open class ListCoAuthorEmails : DefaultTask() {

    @Input
    lateinit var diggerExtension: DiggerExtension

    @Input
    var exportToGithubEnv: Boolean = false

    @TaskAction
    fun execute() {
        val output = JsonOutput.toJson(
            ContributionDataJson(
                diggerExtension.collectCoAuthors()
                    .sortedBy { it.email }
                    .map { it.email }
                    .toList(),
            ),
        )

        val githubEnvFile = System.getenv("GITHUB_ENV")
        if (exportToGithubEnv && githubEnvFile != null) {
            FileOutputStream(githubEnvFile, true)
                .write("DIGGER_CONTRIBUTION_DATA=$output".toByteArray())
        } else {
            logger.quiet(output)
        }
    }
}

data class ContributionDataJson(val authors: List<String>)
