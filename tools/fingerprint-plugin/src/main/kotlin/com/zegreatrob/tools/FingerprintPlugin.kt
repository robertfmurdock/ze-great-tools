package com.zegreatrob.tools

import com.zegreatrob.tools.fingerprint.AggregateFingerprintsTask
import com.zegreatrob.tools.fingerprint.CompareAggregateFingerprintsTask
import com.zegreatrob.tools.fingerprint.FingerprintExtension
import com.zegreatrob.tools.fingerprint.FingerprintTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

class FingerprintPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val version = project.providers.systemProperty("test.plugin.version")
            .orElse(
                project.provider {
                    this.javaClass.`package`.implementationVersion ?: "development"
                },
            )

        val isRoot = project == project.rootProject

        val extension = project.extensions.create("fingerprintConfig", FingerprintExtension::class.java)
        extension.includedProjects.convention(emptySet())
        extension.includedBuilds.convention(emptySet())

        project.providers.gradleProperty("fingerprintCompareToFile")
            .map { project.file(it) }
            .let { fileProvider ->
                extension.compareToFile.convention(project.layout.file(fileProvider))
            }

        project.tasks.register("generateFingerprint", FingerprintTask::class.java) { task ->
            task.pluginVersion.set(version)
            task.outputFile.set(project.layout.buildDirectory.file("fingerprint.txt"))
            task.manifestFile.set(project.layout.buildDirectory.file("fingerprint-manifest.log"))
            task.baseDir.set(project.layout.projectDirectory)

            val includedNames = extension.includedProjects.get()
            val targets = if (isRoot) project.allprojects else listOf(project)

            targets
                .filter { it.isIncludedByConfig(includedNames, project.rootProject) }
                .forEach { sub ->
                    task.addNonTestCompileClasspaths(sub)

                    sub.pluginManager.withPlugin("java") {
                        task.addJavaMainSources(sub)
                        task.addJavaJarArtifact(sub)
                    }
                    sub.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                        task.addJavaMainSources(sub)
                        task.addJavaJarArtifact(sub)
                    }
                    sub.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                        task.addKmpMainSources(sub)
                        task.addKmpPublishedArtifacts(sub)
                    }
                }
        }

        if (isRoot) {
            project.tasks.register("aggregateFingerprints", AggregateFingerprintsTask::class.java) { task ->
                val localTask = project.tasks.named("generateFingerprint", FingerprintTask::class.java)
                task.localFingerprint.set(localTask.flatMap { it.outputFile })
                task.localManifest.set(localTask.flatMap { it.manifestFile })

                task.dependsOn(localTask)

                extension.includedBuilds.orElse(emptySet()).get().forEach { buildName ->
                    task.dependsOn(project.gradle.includedBuild(buildName).task(":generateFingerprint"))

                    task.includedFingerprints.from(
                        project.gradle.includedBuild(buildName).projectDir.resolve("build/fingerprint.txt"),
                    )
                    task.includedManifests.from(
                        project.gradle.includedBuild(buildName).projectDir.resolve("build/fingerprint-manifest.log"),
                    )
                }

                task.outputFile.set(project.layout.buildDirectory.file("aggregate-fingerprint.txt"))
                task.outputManifestFile.set(project.layout.buildDirectory.file("aggregate-fingerprint-manifest.log"))
            }

            project.tasks.register("compareAggregateFingerprints", CompareAggregateFingerprintsTask::class.java) { task ->
                val aggregateTask = project.tasks.named("aggregateFingerprints", AggregateFingerprintsTask::class.java)
                task.dependsOn(aggregateTask)

                task.currentFingerprint.set(aggregateTask.flatMap { it.outputFile })
                task.expectedFingerprint.set(extension.compareToFile)
            }
        }
    }
}

private fun Project.isIncludedByConfig(includedNames: Set<String>, root: Project): Boolean = includedNames.isEmpty() || name in includedNames || this == root

private fun Any.invokeNoArg(methodName: String): Any? = javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }?.invoke(this)

private fun Any.asIterableOrEmpty(): Iterable<Any> {
    val iterator = (invokeNoArg("iterator") as? Iterator<*>) ?: return emptyList()
    return Iterable { iterator }.filterNotNull()
}

private fun Project.kotlinExtensionOrNull(): Any? = extensions.findByName("kotlin")

private fun Project.kmpSourceSets(): Sequence<Any> {
    val kotlinExt = kotlinExtensionOrNull() ?: return emptySequence()
    val sourceSets = kotlinExt.invokeNoArg("getSourceSets") ?: return emptySequence()
    return sourceSets.asIterableOrEmpty().asSequence()
}

private fun Any.kmpNameOrNull(): String? = invokeNoArg("getName") as? String

private fun Any.kmpKotlinOrNull(): SourceDirectorySet? = invokeNoArg("getKotlin") as? SourceDirectorySet

private fun Any.kmpResourcesOrNull(): SourceDirectorySet? = invokeNoArg("getResources") as? SourceDirectorySet

private fun Project.kmpMainSourceSets(): Sequence<Any> = kmpSourceSets().filter { it.kmpNameOrNull()?.endsWith("Main") == true }

private fun FingerprintTask.addNonTestCompileClasspaths(from: Project) {
    from.configurations
        .matching {
            val name = it.name.lowercase()
            it.isCanBeResolved &&
                (name.contains("compileclasspath") || name.contains("compilationclasspath")) &&
                !name.contains("test")
        }
        .forEach { cfg -> classpath.from(cfg) }
}

private fun FingerprintTask.addJavaMainSources(from: Project) {
    val sourceSets = from.extensions.getByType(SourceSetContainer::class.java)
    val main = sourceSets.getByName("main")
    sources.from(main.allSource)
}

private fun FingerprintTask.addJavaJarArtifact(from: Project) {
    val jarTask = from.tasks.named("jar", Jar::class.java)
    dependsOn(jarTask)
    publishedArtifacts.from(jarTask.flatMap { it.archiveFile })
}

private fun FingerprintTask.addKmpMainSources(from: Project) {
    from.kmpMainSourceSets().forEach { ss ->
        ss.kmpKotlinOrNull()?.let { sources.from(it) }
        ss.kmpResourcesOrNull()?.let { sources.from(it) }
    }
}

private fun FingerprintTask.addKmpPublishedArtifacts(from: Project) {
    from.configurations
        .matching {
            val n = it.name.lowercase()
            it.isCanBeConsumed &&
                (n.endsWith("apielements") || n.endsWith("runtimeelements")) &&
                !n.contains("test")
        }
        .forEach { cfg ->
            publishedArtifacts.from(cfg.outgoing.artifacts.files)
        }
}
