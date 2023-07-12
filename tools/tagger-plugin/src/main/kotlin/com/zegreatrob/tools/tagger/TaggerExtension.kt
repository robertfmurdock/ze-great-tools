package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.TaggerPlugin
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property

open class TaggerExtension(
    val grgitServiceExtension: GrgitServiceExtension,
    @Transient val rootProject: Project,
    objectFactory: ObjectFactory,
) {

    @Input
    var releaseBranch: String? = null

    @Input
    var implicitPatch = objectFactory.property<Boolean>().convention(true)

    @Input
    var githubReleaseEnabled = objectFactory.property<Boolean>().convention(false)

    val version by lazy {
        calculateBuildVersion(
            grgitServiceExtension.service.get().grgit,
            releaseBranch
                ?: throw GradleException("Please configure the tagger release branch."),
        )
    }

    val isSnapshot get() = version.contains("SNAPSHOT")

    private fun calculateBuildVersion(grgit: Grgit, releaseBranch: String) = grgit.calculateNextVersion(implicitPatch.get()) +
        if (grgit.canRelease(releaseBranch)) {
            ""
        } else {
            "-SNAPSHOT"
        }

    companion object {
        fun apply(rootProject: Project): TaggerExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(TaggerPlugin::class.java)
            return rootProject.extensions.getByName("tagger") as TaggerExtension
        }
    }
}
