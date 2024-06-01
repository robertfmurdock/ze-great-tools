package com.zegreatrob.tools.digger

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class HeadTask : DefaultTask() {
    @Internal
    lateinit var diggerExtension: DiggerExtension

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val output = diggerExtension.headId()
        outputFile.get().asFile.writeText(output)
    }
}
