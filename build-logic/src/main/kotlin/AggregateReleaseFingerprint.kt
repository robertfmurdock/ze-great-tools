import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

abstract class AggregateReleaseFingerprint : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fingerprints: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val content = fingerprints.files
            .sortedBy { it.path }
            .joinToString("\n") { it.readText().trim() }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(
            buildString {
                appendLine("aggregate-release-fingerprint-v1")
                appendLine("hash=$digest")
                appendLine()
                append(content)
            }
        )
    }
}
