package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.tagger.core.tagReport
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class CommitReport :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @TaskAction
    fun execute() {
        println("COMMIT REPORT-------")
        println("--------------------${taggerExtension.core.tagReport()}")
        println("COMMIT REPORT OVAH--")
    }
}
