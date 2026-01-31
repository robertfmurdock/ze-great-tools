package com.zegreatrob.tools

import com.zegreatrob.tools.fingerprint.AggregateFingerprintsTask
import com.zegreatrob.tools.fingerprint.FingerprintExtension
import com.zegreatrob.tools.fingerprint.FingerprintTask

val version = System.getProperty("test.plugin.version")
    ?: this.javaClass.`package`.implementationVersion
    ?: "development"
val isRoot = project == project.rootProject

val extension = project.extensions.create("fingerprintConfig", FingerprintExtension::class.java)!!

extension.includedProjects.convention(emptySet<String>())

project.tasks.register("generateFingerprint", FingerprintTask::class.java) {
    pluginVersion.set(version)

    dependencies.set(
        project.provider {
            val includedNames = extension.includedProjects.get()
            val targets = if (isRoot) project.allprojects else listOf(project)

            val filteredTargets = targets.filter {
                includedNames.isEmpty() || it.name in includedNames || it == project.rootProject
            }

            val depsString = filteredTargets.flatMap { sub ->
                sub.configurations.filter {
                    it.isCanBeResolved && (it.name.contains("CompileClasspath") || it.name.contains("CompilationClasspath")) && !it.name.contains(
                        "Test",
                    )
                }.flatMap { config ->
                    val resolvedConfig = config.resolvedConfiguration

                    if (resolvedConfig.hasError()) {
                        resolvedConfig.rethrowFailure()
                    }

                    resolvedConfig.resolvedArtifacts.map {
                        "${sub.name}:${it.moduleVersion.id.group}:${it.moduleVersion.id.name}:${it.moduleVersion.id.version}"
                    }
                }
            }.distinct().sorted().joinToString(",")
            "v=$version|deps=${depsString.ifEmpty { "none" }}"
        },
    )

    outputFile.set(project.layout.buildDirectory.file("fingerprint.txt"))
}

if (project == project.rootProject) {
    project.tasks.register("aggregateFingerprints", AggregateFingerprintsTask::class.java) {
        val localTask = project.tasks.named("generateFingerprint", FingerprintTask::class.java)
        localFingerprint.set(localTask.flatMap { it.outputFile })

        includedFingerprints.from(
            project.gradle.includedBuilds.map { included ->
                included.projectDir.resolve("build/fingerprint.txt")
            },
        )

        dependsOn(localTask)

        outputFile.set(project.layout.buildDirectory.file("aggregate-fingerprint.txt"))
    }
}
