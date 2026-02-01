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

fun Project.isIncludedByConfig(includedNames: Set<String>, root: Project): Boolean = includedNames.isEmpty() || name in includedNames || this == root

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

fun FingerprintTask.addNonTestCompileClasspaths(from: Project) {
    from.configurations
        .matching {
            it.isCanBeResolved &&
                (it.name.contains("CompileClasspath") || it.name.contains("CompilationClasspath")) &&
                !it.name.contains("Test")
        }
        .forEach { cfg -> classpath.from(cfg) }
}

fun FingerprintTask.addJavaMainSources(from: Project) {
    val sourceSets = from.extensions.getByType<SourceSetContainer>()
    val main = sourceSets.named("main").get()
    sources.from(main.allSource)
}

fun FingerprintTask.addKmpMainSources(from: Project) {
    from.kmpMainSourceSets().forEach { ss ->
        ss.kmpKotlinOrNull()?.let { sources.from(it) }
        ss.kmpResourcesOrNull()?.let { sources.from(it) }
    }
}

project.tasks.register("generateFingerprint", FingerprintTask::class.java) {
    pluginVersion.set(version)
    outputFile.set(project.layout.buildDirectory.file("fingerprint.txt"))
    manifestFile.set(project.layout.buildDirectory.file("fingerprint-manifest.log"))
    baseDir.set(project.layout.projectDirectory)

    val includedNames = extension.includedProjects.get()
    val targets = if (isRoot) project.allprojects else listOf(project)

    targets
        .filter { it.isIncludedByConfig(includedNames, project.rootProject) }
        .forEach { sub ->
            addNonTestCompileClasspaths(sub)

            sub.pluginManager.withPlugin("java") { addJavaMainSources(sub) }
            sub.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") { addJavaMainSources(sub) }
            sub.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") { addKmpMainSources(sub) }
        }
}

if (project == project.rootProject) {
    project.tasks.register("aggregateFingerprints", AggregateFingerprintsTask::class.java) {
        val localTask = project.tasks.named("generateFingerprint", FingerprintTask::class.java)
        localFingerprint.set(localTask.flatMap { it.outputFile })
        localManifest.set(localTask.flatMap { it.manifestFile })

        dependsOn(localTask)

        extension.includedBuilds.get().forEach { buildName ->
            dependsOn(project.gradle.includedBuild(buildName).task(":generateFingerprint"))

            includedFingerprints.from(
                project.gradle.includedBuild(buildName).projectDir.resolve("build/fingerprint.txt"),
            )
            includedManifests.from(
                project.gradle.includedBuild(buildName).projectDir.resolve("build/fingerprint-manifest.log"),
            )
        }

        outputFile.set(project.layout.buildDirectory.file("aggregate-fingerprint.txt"))
        outputManifestFile.set(project.layout.buildDirectory.file("aggregate-fingerprint-manifest.log"))
    }
}
