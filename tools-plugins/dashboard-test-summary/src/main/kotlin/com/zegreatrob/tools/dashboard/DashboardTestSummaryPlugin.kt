package com.zegreatrob.tools.dashboard

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.VerificationType
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.testing.AbstractTestTask

abstract class DashboardTestSummaryExtension {
    abstract val reports: ConfigurableFileCollection
    abstract val includedCounts: ConfigurableFileCollection
}

class DashboardTestSummaryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.tasks.findByName("collectDashboardProjectTestCounts") != null) return
        val options = project.extensions.create(
            "dashboardTestSummary",
            DashboardTestSummaryExtension::class.java,
        )
        configureProject(project, project.extensions.getByType(DashboardTestSummaryExtension::class.java))
        project.subprojects { child ->
            val childOptions = child.extensions.findByType(DashboardTestSummaryExtension::class.java)
                ?: child.extensions.create("dashboardTestSummary", DashboardTestSummaryExtension::class.java)
            configureProject(child, childOptions)
        }
        if (project == project.rootProject) {
            configureBuild(project, options)
        }
    }
}

private fun configureProject(project: Project, options: DashboardTestSummaryExtension) = with(project) {
    val tests = tasks.withType(AbstractTestTask::class.java)
    val projectCounts = tasks.register("collectDashboardProjectTestCounts", CollectJUnitCounts::class.java) { task ->
        task.reportFiles.from(options.reports)
        task.testOutputDirectories.from(
            provider {
                tests.filter { it.enabled && it.reports.junitXml.required.get() }
                    .map { test ->
                        val testTask = tasks.named(test.name, AbstractTestTask::class.java)
                        files(testTask.flatMap { it.reports.junitXml.outputLocation.locationOnly }).builtBy(testTask)
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

private fun configureBuild(project: Project, options: DashboardTestSummaryExtension) = with(project) {
    val localCounts = collectBuildCounts()
    tasks.register("dashboardTestSummaryCounts", MergeJUnitCounts::class.java) { task ->
        task.countsFiles.from(localCounts)
        task.countsFiles.from(options.includedCounts)
        task.countsFile.set(layout.buildDirectory.file("dashboard/build-test-counts.json"))
    }
    registerSummary(localCounts, options.includedCounts)
}

private fun Project.collectBuildCounts(): Provider<RegularFile> {
    return tasks.register("collectDashboardBuildTestCounts", MergeJUnitCounts::class.java) { task ->
        task.countsFiles.from(tasks.named("collectDashboardProjectTestCounts", CollectJUnitCounts::class.java).flatMap { it.countsFile })
        subprojects.forEach { child ->
            task.countsFiles.from(child.tasks.named("collectDashboardProjectTestCounts", CollectJUnitCounts::class.java).flatMap { it.countsFile })
        }
        task.countsFile.set(layout.buildDirectory.file("dashboard/build-test-counts.json"))
    }.flatMap { it.countsFile }
}

private fun Project.registerSummary(localCounts: Provider<RegularFile>, includedCounts: ConfigurableFileCollection) {
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
