package com.zegreatrob.tools.tagger

import org.ajoberstar.grgit.Grgit
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
        grgit: Grgit,
        releaseBranch: String?,
    ) = grgit.branch.current().name == releaseBranch
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
            headHasNoTag() &&
            isOnReleaseBranch(grgit, releaseBranch)
        ) {
            this.grgit.tag.add(
                fun (it: TagAddOp) {
                    it.name = version
                },
            )
            this.grgit.push(
                fun (it: PushOp) {
                    it.tags = true
                },
            )
        } else {
            logger.warn("skipping tag")
        }
    }

    private fun headHasNoTag(): Boolean =
        grgit.head().let { head ->
            grgit.resolve.toTagName(head.id) == head.id
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
        println("--------------------${taggerExtension.grgitServiceExtension.service.get().grgit.tagReport()}")
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
        val grgit = taggerExtension.grgitServiceExtension.service.get().grgit
        if (isOnReleaseBranch(grgit, taggerExtension.releaseBranch) && isSnapshot()) {
            throw GradleException("Cannot release a snapshot")
        }
    }
}
