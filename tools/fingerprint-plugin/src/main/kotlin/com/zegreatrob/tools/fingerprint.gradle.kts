package com.zegreatrob.tools

import com.zegreatrob.tools.fingerprint.AggregateFingerprintsTask
import com.zegreatrob.tools.fingerprint.CompareAggregateFingerprintsTask
import com.zegreatrob.tools.fingerprint.FingerprintExtension
import com.zegreatrob.tools.fingerprint.FingerprintTask
import org.gradle.jvm.tasks.Jar

val version: Provider<String> = project.providers.systemProperty("test.plugin.version")
    .orElse(
        project.provider {
            this.javaClass.`package`.implementationVersion ?: "development"
        },
    )

val isRoot = project == project.rootProject

val extension = project.extensions.create("fingerprintConfig", FingerprintExtension::class.java)!!
extension.includedProjects.convention(emptySet<String>())
extension.includedBuilds.convention(emptySet())

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
            val name = it.name.lowercase()
            it.isCanBeResolved &&
                (name.contains("compileclasspath") || name.contains("compilationclasspath")) &&
                !name.contains("test")
        }
        .forEach { cfg -> classpath.from(cfg) }
}

fun FingerprintTask.addJavaMainSources(from: Project) {
    val sourceSets = from.extensions.getByType<SourceSetContainer>()
    val main = sourceSets.named("main").get()
    sources.from(main.allSource)
}

fun FingerprintTask.addJavaJarArtifact(from: Project) {
    val jarTask = from.tasks.named("jar", Jar::class.java)
    dependsOn(jarTask)
    publishedArtifacts.from(jarTask.flatMap { it.archiveFile })
}

fun FingerprintTask.addKmpMainSources(from: Project) {
    from.kmpMainSourceSets().forEach { ss ->
        ss.kmpKotlinOrNull()?.let { sources.from(it) }
        ss.kmpResourcesOrNull()?.let { sources.from(it) }
    }
}

fun FingerprintTask.addKmpPublishedArtifacts(from: Project) {
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

            sub.pluginManager.withPlugin("java") {
                addJavaMainSources(sub)
                addJavaJarArtifact(sub)
            }
            sub.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                addJavaMainSources(sub)
                addJavaJarArtifact(sub)
            }
            sub.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                addKmpMainSources(sub)
                addKmpPublishedArtifacts(sub)
            }
        }
}

if (project == project.rootProject) {
    project.tasks.register("aggregateFingerprints", AggregateFingerprintsTask::class.java) {
        val localTask = project.tasks.named("generateFingerprint", FingerprintTask::class.java)
        localFingerprint.set(localTask.flatMap { it.outputFile })
        localManifest.set(localTask.flatMap { it.manifestFile })

        dependsOn(localTask)

        extension.includedBuilds.orElse(emptySet()).get().forEach { buildName ->
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

    project.tasks.register("compareAggregateFingerprints", CompareAggregateFingerprintsTask::class.java) {
        val aggregateTask = project.tasks.named("aggregateFingerprints", AggregateFingerprintsTask::class.java)
        dependsOn(aggregateTask)

        currentFingerprint.set(aggregateTask.flatMap { it.outputFile })
        expectedFingerprint.set(extension.compareToFingerprintFile)
    }
}
