package com.zegreatrob.tools.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Copying report files is inexpensive")
abstract class CopyReportFiles : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Internal
    abstract val sourceDirectory: DirectoryProperty

    @get:Internal
    abstract val destinationDirectory: DirectoryProperty

    @get:OutputFiles
    val outputFiles: List<java.io.File>
        get() {
            val source = sourceDirectory.get().asFile
            val destination = destinationDirectory.get().asFile
            return sourceFiles.files.map { destination.resolve(it.relativeTo(source)) }
        }

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun copyReports() {
        fileSystemOperations.copy {
            from(sourceDirectory)
            into(destinationDirectory)
        }
    }
}
