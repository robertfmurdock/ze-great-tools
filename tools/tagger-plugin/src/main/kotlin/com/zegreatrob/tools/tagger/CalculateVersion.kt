package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.tagger.core.VersionResult
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream

open class CalculateVersion :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @Input
    var exportToGithubEnv: Boolean = false

    @TaskAction
    fun execute() {
        val result = taggerExtension.calculateVersion()
        when (result) {
            is VersionResult.Failure ->
                throw GradleException(result.reasons.joinToString("\n") { it.message })

            is VersionResult.Success -> {
                result.outputSuccess()
            }
        }
    }

    private fun VersionResult.Success.outputSuccess() {
        logger.quiet(version)
        val githubEnvFile = System.getenv("GITHUB_ENV")
        if (exportToGithubEnv && githubEnvFile != null) {
            FileOutputStream(githubEnvFile, true)
                .write("TAGGER_VERSION=$version".toByteArray())
        }
    }
}
