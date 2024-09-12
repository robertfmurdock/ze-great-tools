package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.tagger.core.TagResult
import com.zegreatrob.tools.tagger.core.tag
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

interface TaggerExtensionSyntax {
    var taggerExtension: TaggerExtension
}

open class TagVersion :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @Input
    lateinit var version: String

    override fun getEnabled(): Boolean = !isSnapshot()

    private fun isSnapshot() = version.contains("SNAPSHOT")

    @TaskAction
    fun execute() = taggerExtension.run {
        when (val result = core.tag(this@TagVersion.version, releaseBranch, userName, userEmail)) {
            TagResult.Success -> {}
            is TagResult.Error -> if (warningsAsErrors.get()) {
                throw GradleException(result.message)
            } else {
                logger.warn(result.message)
            }
        }
    }
}
