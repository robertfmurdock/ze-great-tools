package com.zegreatrob.tools

import com.zegreatrob.tools.fingerprint.AggregateFingerprintsTask
import com.zegreatrob.tools.fingerprint.FingerprintExtension
import com.zegreatrob.tools.fingerprint.FingerprintTask

val version = project.providers.systemProperty("test.plugin.version")
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

    val includedNames = extension.includedProjects.get()
    val targets = if (isRoot) project.allprojects else listOf(project)

    val filteredTargets = targets.filter {
        includedNames.isEmpty() || it.name in includedNames || it == project.rootProject
    }

    filteredTargets.forEach { sub ->
        sub.configurations
            .matching {
                it.isCanBeResolved &&
                    (it.name.contains("CompileClasspath") || it.name.contains("CompilationClasspath")) &&
                    !it.name.contains("Test")
            }
            .forEach { cfg ->
                classpath.from(cfg)
            }
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
