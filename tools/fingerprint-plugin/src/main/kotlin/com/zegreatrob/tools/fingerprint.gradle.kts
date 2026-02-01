package com.zegreatrob.tools

import com.zegreatrob.tools.fingerprint.AggregateFingerprintsTask
import com.zegreatrob.tools.fingerprint.FingerprintExtension
import com.zegreatrob.tools.fingerprint.FingerprintTask
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

val version: Provider<String> = project.providers.systemProperty("test.plugin.version")
    .orElse(
        project.provider {
            this.javaClass.`package`.implementationVersion ?: "development"
        },
    )

val isRoot = project == project.rootProject

val extension = project.extensions.create("fingerprintConfig", FingerprintExtension::class.java)!!

extension.includedProjects.convention(emptySet<String>())

project.tasks.register("generateFingerprint", FingerprintTask::class.java) {
    pluginVersion.set(version)
    outputFile.set(project.layout.buildDirectory.file("fingerprint.txt"))
    baseDir.set(project.layout.projectDirectory)

    val includedNames = extension.includedProjects.get()
    val targets = if (isRoot) project.allprojects else listOf(project)

    val filteredTargets = targets.filter {
        includedNames.isEmpty() || it.name in includedNames || it == project.rootProject
    }

    fun FingerprintTask.wireClasspathInputsFrom(sub: Project) {
        sub.configurations
            .matching {
                it.isCanBeResolved &&
                    (it.name.contains("CompileClasspath") || it.name.contains("CompilationClasspath")) &&
                    !it.name.contains("Test")
            }
            .forEach { cfg -> classpath.from(cfg) }
    }

    fun FingerprintTask.wireMainSourcesFromJavaSourceSets(sub: Project) {
        val sourceSets = sub.extensions.getByType<SourceSetContainer>()
        val main = sourceSets.named("main").get()
        sources.from(main.allSource)
    }

    fun FingerprintTask.wireMainSourcesFromKotlinMultiplatform(sub: Project) {
        val kotlinExt = sub.extensions.findByName("kotlin") ?: return

        val getSourceSets = kotlinExt.javaClass.methods
            .firstOrNull { it.name == "getSourceSets" && it.parameterCount == 0 }
            ?: return

        val asIterable = when (val sourceSetsContainer = getSourceSets.invoke(kotlinExt)) {
            is Iterable<*> -> sourceSetsContainer
            else -> {
                val iteratorMethod = sourceSetsContainer.javaClass.methods
                    .firstOrNull { it.name == "iterator" && it.parameterCount == 0 }
                    ?: return
                val iterator = iteratorMethod.invoke(sourceSetsContainer) as? Iterator<*> ?: return
                Iterable { iterator }
            }
        }

        fun invokeSourceDirSet(sourceSet: Any, getterName: String): SourceDirectorySet? =
            sourceSet.javaClass.methods
                .firstOrNull { it.name == getterName && it.parameterCount == 0 }
                ?.invoke(sourceSet) as? SourceDirectorySet

        fun sourceSetName(sourceSet: Any): String? =
            sourceSet.javaClass.methods
                .firstOrNull { it.name == "getName" && it.parameterCount == 0 }
                ?.invoke(sourceSet) as? String

        asIterable
            .filterNotNull()
            .forEach { ss ->
                val name = sourceSetName(ss) ?: return@forEach
                if (!name.endsWith("Main")) return@forEach

                invokeSourceDirSet(ss, "getKotlin")?.let { sources.from(it) }
                invokeSourceDirSet(ss, "getResources")?.let { sources.from(it) }
            }
    }

    filteredTargets.forEach { sub ->
        wireClasspathInputsFrom(sub)

        sub.pluginManager.withPlugin("java") { wireMainSourcesFromJavaSourceSets(sub) }
        sub.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") { wireMainSourcesFromJavaSourceSets(sub) }
        sub.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") { wireMainSourcesFromKotlinMultiplatform(sub) }

        sources.from(
            sub.fileTree("src") {
                include("main/**")
                include("**/*Main/**")
                exclude("**/build/**")
                exclude("**/.gradle/**")
            },
        )
    }
}

if (project == project.rootProject) {
    project.tasks.register("aggregateFingerprints", AggregateFingerprintsTask::class.java) {
        val localTask = project.tasks.named("generateFingerprint", FingerprintTask::class.java)
        localFingerprint.set(localTask.flatMap { it.outputFile })

        dependsOn(localTask)

        val includedBuildNames = extension.includedBuilds.get()
        includedBuildNames.forEach { buildName ->
            dependsOn(project.gradle.includedBuild(buildName).task(":generateFingerprint"))
            includedFingerprints.from(
                project.gradle.includedBuild(buildName).projectDir.resolve("build/fingerprint.txt"),
            )
        }

        outputFile.set(project.layout.buildDirectory.file("aggregate-fingerprint.txt"))
    }
}
