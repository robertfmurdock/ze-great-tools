package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

interface TaggerExtensionSyntax {
    var taggerExtension: TaggerExtension

    @get:Internal
    val releaseBranch get() = taggerExtension.releaseBranch

    @Internal
    fun isSnapshot() = taggerExtension.isSnapshot

    @Internal
    fun isOnReleaseBranch(
        adapter: GitAdapter,
        releaseBranch: String?,
    ) = adapter.status().head == releaseBranch
}

open class TagVersion :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @Input
    lateinit var version: String

    override fun getEnabled(): Boolean = !isSnapshot()

    @TaskAction
    fun execute() {
        val gitAdapter = taggerExtension.gitAdapter
        val isSnapshot = isSnapshot()
        val headTag = gitAdapter.showTag("HEAD")
        val alreadyTagged = headTag != null
        val headBranch = gitAdapter.status().head
        val isNotOnReleaseBranch = headBranch != releaseBranch
        if (isSnapshot || alreadyTagged || isNotOnReleaseBranch) {
            logger.warn(
                "skipping tag due to ${
                    mapOf(
                        isSnapshot to "being snapshot",
                        alreadyTagged to "already tagged $headTag",
                        isNotOnReleaseBranch to "not on release branch $releaseBranch - branch was $headBranch",
                    )
                        .filterKeys { it }
                        .values.joinToString(", ")
                }",
            )
        } else {
            gitAdapter.newAnnotatedTag(version, "HEAD")
            gitAdapter.pushTags()
        }
    }
}

open class CommitReport :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @TaskAction
    fun execute() {
        println("COMMIT REPORT-------")
        println("--------------------${tagReport(taggerExtension.gitAdapter)}")
        println("COMMIT REPORT OVAH--")
    }
}

open class ReleaseVersion :
    DefaultTask(),
    TaggerExtensionSyntax {
    @Input
    override lateinit var taggerExtension: TaggerExtension

    @TaskAction
    fun execute() {
        if (isOnReleaseBranch(taggerExtension.gitAdapter, taggerExtension.releaseBranch) && isSnapshot()) {
            throw GradleException("Cannot release a snapshot")
        }
    }
}
