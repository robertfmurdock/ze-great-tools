package com.zegreatrob.tools

import com.zegreatrob.tools.fingerprint.FingerprintTask

val version = System.getProperty("test.plugin.version")
    ?: this.javaClass.`package`.implementationVersion
    ?: "development"

project.tasks.register("generateFingerprint", FingerprintTask::class.java) {
    pluginVersion.set(version)
    dependencies.set(
        project.provider {
            val resolvableConfigs = project.configurations.filter {
                it.isCanBeResolved &&
                    (it.name.contains("CompileClasspath") || it.name.contains("CompilationClasspath")) &&
                    !it.name.contains("Test", ignoreCase = true)
            }

            val depsString = if (resolvableConfigs.isNotEmpty() && project.repositories.isNotEmpty()) {
                resolvableConfigs.flatMap { config ->
                    config.resolvedConfiguration.resolvedArtifacts.map {
                        "${it.moduleVersion.id.group}:${it.moduleVersion.id.name}:${it.moduleVersion.id.version}"
                    }
                }.distinct().sorted().joinToString(",")
            } else {
                "no-deps"
            }

            "v=$version|deps=$depsString"
        },
    )

    outputFile.set(project.layout.buildDirectory.file("fingerprint.txt"))
}
