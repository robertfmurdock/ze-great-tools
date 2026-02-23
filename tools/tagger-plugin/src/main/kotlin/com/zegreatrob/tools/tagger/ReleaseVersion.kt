package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.isOnReleaseBranch
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ReleaseVersion : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val workingDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val releaseBranch: Property<String>

    @Input
    lateinit var version: String
    private fun isSnapshot() = version.contains("SNAPSHOT")

    @TaskAction
    fun execute() {
        val branch = releaseBranch.orNull
            ?: throw GradleException("Please configure the tagger release branch.")
        val core = TaggerCore(GitAdapter(workingDirectory.get().asFile.absolutePath))
        if (core.isOnReleaseBranch(branch) && isSnapshot()) {
            throw GradleException("Cannot release a snapshot")
        }
    }
}
