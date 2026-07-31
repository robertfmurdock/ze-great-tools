package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.lastVersionAndTag
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class PreviousVersion : DefaultTask() {
    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gitDirectory: DirectoryProperty

    @get:Input
    abstract val showCommands: Property<Boolean>

    @TaskAction
    fun execute() {
        val commandLogger = if (showCommands.get()) {
            { command: String -> logger.lifecycle(command) }
        } else {
            null
        }
        val core = TaggerCore(GitAdapter(workingDirectory.get().asFile.absolutePath, commandLogger = commandLogger))
        logger.quiet(core.lastVersionAndTag()?.first)
    }
}
