
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

abstract class WriteReleaseFingerprint : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val dependencyFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val lines = mutableSetOf<String>()

        sourceFiles.files.forEach { file ->
            lines += "SRC:${file.path}"
        }

        dependencyFiles.files.forEach { file ->
            lines += "DEP:${file.name}"
        }

        val normalized = lines.toList().sorted()

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.joinToString("\n").toByteArray())
            .joinToString("") { "%02x".format(it) }

        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(
            buildString {
                appendLine("release-fingerprint-v1")
                appendLine("hash=$digest")
                appendLine()
                normalized.forEach(::appendLine)
            }
        )
    }
}
