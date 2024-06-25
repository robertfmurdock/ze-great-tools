package com.zegreatrob.tools.tagger

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class PreviousVersion :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @TaskAction
    fun execute() {
        logger.quiet(taggerExtension.lastVersionAndTag?.first)
    }
}
