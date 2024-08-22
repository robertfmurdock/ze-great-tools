package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.tagger.core.isOnReleaseBranch
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class ReleaseVersion :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @Input
    lateinit var version: String
    private fun isSnapshot() = version.contains("SNAPSHOT")

    @TaskAction
    fun execute() {
        if (taggerExtension.core.isOnReleaseBranch(taggerExtension.releaseBranch) && isSnapshot()) {
            throw GradleException("Cannot release a snapshot")
        }
    }
}
