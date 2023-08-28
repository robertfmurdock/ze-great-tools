package com.zegreatrob.tools.digger

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class ListCoAuthorEmails : DefaultTask() {

    @Input
    lateinit var diggerExtension: DiggerExtension

    @Input
    var exportToGithubEnv: Boolean = false

    @TaskAction
    fun execute() {
        logger.quiet(
            JsonOutput.toJson(
                ContributionDataJson(
                    diggerExtension.collectCoAuthors()
                        .sortedBy { it.email }
                        .map { it.email }
                        .toList(),
                ),
            ),
        )
    }
}

data class ContributionDataJson(val authors: List<String>)
