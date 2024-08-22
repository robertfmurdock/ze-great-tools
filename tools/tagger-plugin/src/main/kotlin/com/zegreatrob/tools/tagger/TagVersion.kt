package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import org.ajoberstar.grgit.operation.PushOp
import org.ajoberstar.grgit.operation.TagAddOp
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

interface TaggerExtensionSyntax {
    var taggerExtension: TaggerExtension

    @get:Internal
    val grgit get() = taggerExtension.grgitServiceExtension.service.get().grgit

    @get:Internal
    val releaseBranch get() = taggerExtension.releaseBranch

    @get:Internal
    val version get() = taggerExtension.version

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

    override fun getEnabled(): Boolean = !isSnapshot()

    @TaskAction
    fun execute() {
        if (
            !isSnapshot() &&
            taggerExtension.gitAdapter.showTag("HEAD") == null &&
            isOnReleaseBranch(taggerExtension.gitAdapter, releaseBranch)
        ) {
            this.grgit.tag.add(
                fun(it: TagAddOp) {
                    it.name = version
                },
            )
            this.grgit.push(
                fun(it: PushOp) {
                    it.tags = true
                },
            )
        } else {
            logger.warn("skipping tag")
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
