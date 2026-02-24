package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TagResult
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.tag
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class TagVersion : DefaultTask() {
    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gitDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val releaseBranch: Property<String>

    @get:Input
    @get:Optional
    abstract val userName: Property<String>

    @get:Input
    @get:Optional
    abstract val userEmail: Property<String>

    @get:Input
    abstract val warningsAsErrors: Property<Boolean>

    @Input
    lateinit var version: String

    override fun getEnabled(): Boolean = !isSnapshot()

    private fun isSnapshot() = version.contains("SNAPSHOT")

    @TaskAction
    fun execute() {
        val core = TaggerCore(GitAdapter(workingDirectory.get().asFile.absolutePath))
        when (val result = core.tag(version, releaseBranch.orNull, userName.orNull, userEmail.orNull)) {
            TagResult.Success -> {}

            is TagResult.Error -> if (warningsAsErrors.get()) {
                throw GradleException(result.message)
            } else {
                logger.warn(result.message)
            }
        }
    }
}
