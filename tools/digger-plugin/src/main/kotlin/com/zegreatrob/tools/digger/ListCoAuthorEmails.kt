package com.zegreatrob.tools.digger

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
            diggerExtension.collectCoAuthors()
                .sortedBy { it.email }
                .joinToString("\n", transform = CoAuthor::email),
        )
    }
}
