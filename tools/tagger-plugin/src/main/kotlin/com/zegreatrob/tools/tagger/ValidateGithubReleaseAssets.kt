package com.zegreatrob.tools.tagger

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ValidateGithubReleaseAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assets: ConfigurableFileCollection

    @get:Input
    abstract val githubReleaseEnabled: Property<Boolean>

    @TaskAction
    fun execute() {
        if (!githubReleaseEnabled.get()) {
            logger.lifecycle("GitHub release not enabled, skipping asset validation")
            return
        }

        val allFiles = assets.files
        val missing = allFiles.filter { !it.exists() }
        if (missing.isNotEmpty()) {
            val missingList = missing.joinToString("\n  - ") { it.absolutePath }
            throw GradleException(
                "Missing GitHub release assets:\n  - $missingList",
            )
        }

        if (allFiles.isEmpty()) {
            logger.lifecycle("No GitHub release assets configured")
        } else {
            logger.lifecycle("All ${allFiles.size} GitHub release assets validated successfully")
        }
    }
}
