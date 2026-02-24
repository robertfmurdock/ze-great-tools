package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.tagReport
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CommitReport : DefaultTask() {
    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gitDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        val core = TaggerCore(GitAdapter(workingDirectory.get().asFile.absolutePath))
        println("COMMIT REPORT-------")
        println("--------------------${core.tagReport()}")
        println("COMMIT REPORT OVAH--")
    }
}
