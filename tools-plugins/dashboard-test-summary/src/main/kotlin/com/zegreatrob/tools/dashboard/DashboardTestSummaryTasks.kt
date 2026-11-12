package com.zegreatrob.tools.dashboard

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@CacheableTask
abstract class CollectJUnitCounts : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testOutputDirectories: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reportFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val countsFile: RegularFileProperty

    @TaskAction
    fun collect() {
        val explicitReports = reportFiles.files.flatMap { path ->
            path.xmlReports().also { require(it.isNotEmpty()) { "No JUnit XML report found at '$path'." } }
        }
        val reports = testOutputDirectories.files.flatMap(File::xmlReports) + explicitReports
        val counts = reports.distinctBy { it.canonicalFile }.fold(JUnitCounts()) { total, file ->
            total + countJUnitReport(file)
        }
        countsFile.writeCounts(counts)
    }
}

@CacheableTask
abstract class MergeJUnitCounts : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val countsFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val countsFile: RegularFileProperty

    @TaskAction
    fun merge() = countsFile.writeCounts(countsFiles.sumCounts())
}

@DisableCachingByDefault(because = "The report depends on the current execution context")
abstract class WriteDashboardTestSummary : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val countsFiles: ConfigurableFileCollection

    @get:Input
    abstract val githubActions: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val runId: Property<String>

    @get:Input
    @get:Optional
    abstract val runAttempt: Property<String>

    @get:Input
    @get:Optional
    abstract val headSha: Property<String>

    @get:OutputFile
    abstract val summaryFile: RegularFileProperty

    @TaskAction
    fun write() {
        val counts = countsFiles.sumCounts()
        require(counts.reports > 0) { "No JUnit XML reports found for this build." }
        summaryFile.writeJson(
            buildJsonObject {
                put("schemaVersion", 1)
                if (githubActions.get()) addGithubRunIdentity()
                counts.addTotals(this)
            },
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.addGithubRunIdentity() {
        val id = requireNotNull(runId.orNull?.toLongOrNull()?.takeIf { it > 0 }) { "A positive GitHub run ID is required in GitHub Actions." }
        val attempt = requireNotNull(runAttempt.orNull?.toIntOrNull()?.takeIf { it > 0 }) { "A positive GitHub run attempt is required in GitHub Actions." }
        val sha = requireNotNull(headSha.orNull?.takeIf { it.isNotBlank() }) { "A GitHub head SHA is required in GitHub Actions." }
        put("runId", id)
        put("runAttempt", attempt)
        put("headSha", sha)
    }
}

private fun File.xmlReports(): List<File> = if (isDirectory) {
    walkTopDown().filter { it.isFile && it.extension == "xml" }.toList()
} else {
    listOf(this).filter(File::isFile)
}

private fun ConfigurableFileCollection.sumCounts() = files.fold(JUnitCounts()) { counts, file -> counts + readCounts(file) }

private fun RegularFileProperty.writeCounts(counts: JUnitCounts) = writeJson(
    buildJsonObject {
        put("reports", counts.reports)
        counts.addTotals(this)
    },
)

private fun RegularFileProperty.writeJson(data: JsonObject) {
    val file = get().asFile
    file.parentFile.mkdirs()
    file.writeText("$data\n")
}
