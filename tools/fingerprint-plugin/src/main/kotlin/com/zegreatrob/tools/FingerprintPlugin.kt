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
        val version = resolvePluginVersion(project)
        val extension = createExtension(project)
        registerGenerateFingerprintTask(project, version, extension)
        registerRootTasksIfNeeded(project, extension)
    }

    private fun registerRootTasksIfNeeded(project: Project, extension: FingerprintExtension) {
        if (project.isRoot()) {
            registerRootTasks(project, extension)
        }
    }

    private fun resolvePluginVersion(project: Project) = project.providers.systemProperty("test.plugin.version")
        .orElse(project.provider { this.javaClass.`package`.implementationVersion ?: "development" })

    private fun createExtension(project: Project): FingerprintExtension {
        val extension = project.extensions.create("fingerprintConfig", FingerprintExtension::class.java)
        extension.includedProjects.convention(emptySet())
        extension.includedBuilds.convention(emptySet())
        extension.compareToFile.convention(project.layout.file(project.providers.gradleProperty("fingerprintCompareToFile").map { project.file(it) }))
        return extension
    }

    private fun registerGenerateFingerprintTask(project: Project, version: org.gradle.api.provider.Provider<String>, extension: FingerprintExtension) {
        project.tasks.register("generateFingerprint", FingerprintTask::class.java) { task ->
            configureGenerateFingerprintTask(task, project, version, extension)
        }
    }

    private fun configureGenerateFingerprintTask(
        task: FingerprintTask,
        project: Project,
        version: org.gradle.api.provider.Provider<String>,
        extension: FingerprintExtension,
    ) {
        task.pluginVersion.set(version)
        task.baseDir.set(project.layout.projectDirectory)
        setTaskOutputPaths(task, project)
        addProjectSources(task, project, extension)
    }

    private fun setTaskOutputPaths(task: FingerprintTask, project: Project) {
        task.outputFile.set(project.layout.buildDirectory.file("fingerprint.txt"))
        task.manifestFile.set(project.layout.buildDirectory.file("fingerprint-manifest.log"))
    }

    private fun addProjectSources(task: FingerprintTask, project: Project, extension: FingerprintExtension) {
        val targets = if (project.isRoot()) project.allprojects else listOf(project)
        targets.filter { it.isIncludedByConfig(extension.includedProjects.get(), project.rootProject) }.forEach { sub ->
            configureProjectFingerprints(task, sub)
        }
    }

    private fun configureProjectFingerprints(task: FingerprintTask, sub: Project) {
        task.addNonTestCompileClasspaths(sub)
        sub.pluginManager.withPlugin("java") { configureJavaProject(task, sub) }
        sub.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") { configureJavaProject(task, sub) }
        sub.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") { configureKmpProject(task, sub) }
    }

    private fun configureJavaProject(task: FingerprintTask, sub: Project) {
        task.addJavaMainSources(sub)
        task.addJavaJarArtifact(sub)
    }

    private fun configureKmpProject(task: FingerprintTask, sub: Project) {
        task.addKmpMainSources(sub)
        task.addKmpPublishedArtifacts(sub)
    }

    private fun registerRootTasks(project: Project, extension: FingerprintExtension) {
        registerAggregateFingerprintsTask(project, extension)
        registerCompareAggregateFingerprints(project, extension)
    }

    private fun registerAggregateFingerprintsTask(project: Project, extension: FingerprintExtension) {
        project.tasks.register("aggregateFingerprints", AggregateFingerprintsTask::class.java) { task ->
            configureAggregateFingerprintsTask(task, project, extension)
        }
    }

    private fun configureAggregateFingerprintsTask(task: AggregateFingerprintsTask, project: Project, extension: FingerprintExtension) {
        val localTask = project.tasks.named("generateFingerprint", FingerprintTask::class.java)
        setLocalTaskDependencies(task, localTask)
        addIncludedBuilds(task, project, extension)
        setAggregateOutputPaths(task, project)
    }

    private fun setLocalTaskDependencies(task: AggregateFingerprintsTask, localTask: org.gradle.api.tasks.TaskProvider<FingerprintTask>) {
        task.localFingerprint.set(localTask.flatMap { it.outputFile })
        task.localManifest.set(localTask.flatMap { it.manifestFile })
        task.dependsOn(localTask)
    }

    private fun setAggregateOutputPaths(task: AggregateFingerprintsTask, project: Project) {
        task.outputFile.set(project.layout.buildDirectory.file("aggregate-fingerprint.txt"))
        task.outputManifestFile.set(project.layout.buildDirectory.file("aggregate-fingerprint-manifest.log"))
    }

    private fun addIncludedBuilds(task: AggregateFingerprintsTask, project: Project, extension: FingerprintExtension) {
        extension.includedBuilds.orElse(emptySet()).get().forEach { buildName ->
            addIncludedBuild(task, project, buildName)
        }
    }

    private fun addIncludedBuild(task: AggregateFingerprintsTask, project: Project, buildName: String) {
        val includedBuild = project.gradle.includedBuild(buildName)
        task.dependsOn(includedBuild.task(":generateFingerprint"))
        task.includedFingerprints.from(includedBuild.projectDir.resolve("build/fingerprint.txt"))
        task.includedManifests.from(includedBuild.projectDir.resolve("build/fingerprint-manifest.log"))
    }

    private fun registerCompareAggregateFingerprints(project: Project, extension: FingerprintExtension) {
        project.tasks.register("compareAggregateFingerprints", CompareAggregateFingerprintsTask::class.java) { task ->
            val aggregateTask = project.tasks.named("aggregateFingerprints", AggregateFingerprintsTask::class.java)
            task.dependsOn(aggregateTask)
            task.currentFingerprint.set(aggregateTask.flatMap { it.outputFile })
            task.expectedFingerprint.set(extension.compareToFile)
        }
    }
}

private fun Project.isRoot(): Boolean = this == rootProject

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
    from.configurations.matching { it.isCompileClasspathConfiguration() }.forEach { cfg -> classpath.from(cfg) }
}

private fun org.gradle.api.artifacts.Configuration.isCompileClasspathConfiguration(): Boolean {
    val name = name.lowercase()
    return isCanBeResolved && (name.contains("compileclasspath") || name.contains("compilationclasspath")) && !name.contains("test")
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
    from.configurations.matching { it.isPublishableKmpConfiguration() }.forEach { cfg ->
        publishedArtifacts.from(cfg.outgoing.artifacts.files)
    }
}

private fun org.gradle.api.artifacts.Configuration.isPublishableKmpConfiguration(): Boolean {
    val n = name.lowercase()
    return isCanBeConsumed && (n.endsWith("apielements") || n.endsWith("runtimeelements")) && !n.contains("test")
}
