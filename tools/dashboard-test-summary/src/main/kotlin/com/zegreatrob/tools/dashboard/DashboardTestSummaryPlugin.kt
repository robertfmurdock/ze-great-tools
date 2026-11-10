package com.zegreatrob.tools.dashboard

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.VerificationType
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.testing.AbstractTestTask

open class DashboardTestSummarySettings(private val settings: Settings) {
    internal val includedBuilds = linkedSetOf<String>()

    fun includeBuild(path: String, module: String) {
        require(module.split(':').let { it.size == 2 && it.all(String::isNotBlank) }) {
            "Expected group:name for dashboard test reports from '$path'."
        }
        settings.includeBuild(path)
        includedBuilds.add(module)
    }
}

abstract class DashboardTestSummaryReports {
    abstract val reports: ConfigurableFileCollection
}

class DashboardTestSummaryPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val options = settings.extensions.create(
            "dashboardTestSummary",
            DashboardTestSummarySettings::class.java,
            settings,
        )
        settings.gradle.settingsEvaluated {
            val projectPaths = settings.rootProject.projectPaths()
            val modules = options.includedBuilds.toList()
            settings.gradle.lifecycle.beforeProject { project ->
                configureProject(project)
                if (project.path == ":") configureBuild(project, projectPaths, modules)
            }
        }
    }
}

private fun ProjectDescriptor.projectPaths(): List<String> = listOf(path) + children.flatMap { it.projectPaths() }

private fun configureProject(project: Project) = with(project) {
    val options = extensions.create("dashboardTestSummary", DashboardTestSummaryReports::class.java)
    val tests = tasks.withType(AbstractTestTask::class.java)
    val projectCounts = tasks.register("collectDashboardProjectTestCounts", CollectJUnitCounts::class.java) { task ->
        task.reportFiles.from(options.reports)
        task.testOutputDirectories.from(
            provider {
                tests.filter { it.enabled && it.reports.junitXml.required.get() }
                    .map { test ->
                        tasks.named(test.name, AbstractTestTask::class.java).map { it.reports.junitXml.outputLocation.get().asFile }
                    }
            },
        )
        task.countsFile.set(layout.buildDirectory.file("dashboard/project-test-counts.json"))
    }
    configurations.consumable("dashboardProjectTestCountsElements") { configuration ->
        configuration.verificationVariant(project, "dashboard-project-test-counts")
        configuration.outgoing.artifact(projectCounts.flatMap { it.countsFile })
    }
}

private fun configureBuild(project: Project, paths: List<String>, modules: List<String>) = with(project) {
    val localCounts = collectBuildCounts(paths)
    val includedBuilds = configurations.dependencyScope("dashboardIncludedTestCountsDependencies")
    modules.forEach { module -> dependencies.add(includedBuilds.name, module) }
    exportBuildCounts(localCounts, includedBuilds.get())
    val includedCounts = verificationInputs("dashboardBuildTestCounts", "dashboard-build-test-counts", includedBuilds.get())
    registerSummary(localCounts, includedCounts)
}

private fun Project.collectBuildCounts(paths: List<String>): Provider<RegularFile> {
    val projects = configurations.dependencyScope("dashboardProjectTestCountsDependencies")
    paths.forEach { path -> dependencies.add(projects.name, dependencies.project(mapOf("path" to path))) }
    val projectInputs = verificationInputs("dashboardProjectTestCounts", "dashboard-project-test-counts", projects.get())
    return tasks.register("collectDashboardBuildTestCounts", MergeJUnitCounts::class.java) { task ->
        task.countsFiles.from(projectInputs)
        task.countsFile.set(layout.buildDirectory.file("dashboard/build-test-counts.json"))
    }.flatMap { it.countsFile }
}

private fun Project.exportBuildCounts(localCounts: Provider<RegularFile>, includedBuilds: Configuration) {
    configurations.consumable("dashboardBuildTestCountsElements") { configuration ->
        configuration.extendsFrom(includedBuilds)
        configuration.verificationVariant(this, "dashboard-build-test-counts")
        configuration.outgoing.artifact(localCounts)
    }
}

private fun Project.registerSummary(localCounts: Provider<RegularFile>, includedCounts: Configuration) {
    val summaryPath = layout.buildDirectory.file("dashboard/test-summary.json")
    val clearSummary = tasks.register("clearDashboardTestSummary", Delete::class.java) { it.delete(summaryPath) }
    tasks.register("dashboardTestSummary", WriteDashboardTestSummary::class.java) { task ->
        task.group = "reporting"
        task.description = "Writes the aggregate JUnit test summary for Ze Great Dashboard."
        task.dependsOn(clearSummary)
        task.countsFiles.from(localCounts, includedCounts)
        task.runId.convention(providers.gradleProperty("dashboardRunId").orElse(providers.environmentVariable("GITHUB_RUN_ID")))
        task.runAttempt.convention(
            providers.gradleProperty("dashboardRunAttempt").orElse(providers.environmentVariable("GITHUB_RUN_ATTEMPT")),
        )
        task.headSha.convention(providers.gradleProperty("dashboardHeadSha").orElse(providers.environmentVariable("GITHUB_SHA")))
        task.summaryFile.set(summaryPath)
    }
}

private fun Project.verificationInputs(name: String, type: String, scope: Configuration): Configuration = configurations.resolvable(name) {
    it.extendsFrom(scope)
    it.verificationVariant(this, type)
}.get()

private fun Configuration.verificationVariant(project: Project, type: String) {
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.VERIFICATION))
    attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, project.objects.named(VerificationType::class.java, type))
}
