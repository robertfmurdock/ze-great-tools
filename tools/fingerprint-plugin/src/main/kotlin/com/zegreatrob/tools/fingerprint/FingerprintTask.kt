package com.zegreatrob.tools.fingerprint

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

@CacheableTask
abstract class FingerprintTask : DefaultTask() {

    @get:Input
    abstract val pluginVersion: Property<String>

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Classpath
    abstract val publishedArtifacts: ConfigurableFileCollection

    @get:Internal
    abstract val baseDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @get:Optional
    @get:OutputFile
    abstract val digestInputDumpFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val digest = MessageDigest.getInstance("SHA-256")

        val dumpFile = digestInputDumpFile.orNull?.asFile
        val dump = if (dumpFile != null) StringBuilder() else null
        var step = 0

        val manifest = StringBuilder()

        fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

        fun writeLine(context: String, detail: String) {
            dump?.append("%04d".format(step++))
                ?.append(" | ")
                ?.append(context)
                ?.append(" | ")
                ?.append(detail)
                ?.append('\n')
        }

        fun updateBytes(context: String, bytes: ByteArray) {
            writeLine(
                context = context,
                detail = "len=${bytes.size} hex=${bytes.toHex()} ascii=${bytes.toString(Charsets.UTF_8)}",
            )
            digest.update(bytes)
        }

        fun updateByte(context: String, value: Int) {
            writeLine(
                context = context,
                detail = "len=1 hex=${"%02x".format(value and 0xFF)} ascii=\\u${"%04x".format(value and 0xFF)}",
            )
            digest.update(value.toByte())
        }

        fun classpathEntryContentSha256Hex(entry: java.io.File): String {
            val md = MessageDigest.getInstance("SHA-256")

            if (entry.isFile) {
                md.update(entry.readBytes())
                return md.digest().toHex()
            }

            if (entry.isDirectory) {
                entry.walkTopDown()
                    .filter { it.isFile }
                    .sortedBy { it.relativeTo(entry).invariantSeparatorsPath }
                    .forEach { f ->
                        val rel = f.relativeTo(entry).invariantSeparatorsPath
                        md.update(rel.toByteArray(Charsets.UTF_8))
                        md.update(0)
                        md.update(f.readBytes())
                        md.update(0)
                    }
                return md.digest().toHex()
            }

            return md.digest().toHex()
        }

        manifest.append("pluginVersion|").append(pluginVersion.get()).append("|\n")

        updateBytes("pluginVersion(prefix)", "pluginVersion=".toByteArray())
        updateBytes("pluginVersion(value)", pluginVersion.get().toByteArray())

        classpath.files
            .sortedBy { it.name }
            .forEach { file ->
                val classpathSha256 = classpathEntryContentSha256Hex(file)
                manifest.append("classpath|").append(file.name).append("|").append(file.length()).append("|")
                    .append(classpathSha256).append("\n")

                val fileContext = "classpath[fileName=${file.name} fileLength=${file.length()}]"
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.name", file.name.toByteArray())
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.length", file.length().toString().toByteArray())

                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext content.sha256", classpathSha256.toByteArray())
            }

        publishedArtifacts.files
            .sortedBy { it.name }
            .forEach { file ->
                val sha256 = classpathEntryContentSha256Hex(file)
                manifest.append("artifact|").append(file.name).append("|").append(file.length()).append("|")
                    .append(sha256).append("\n")

                val fileContext = "artifact[fileName=${file.name} fileLength=${file.length()}]"
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.name", file.name.toByteArray())
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.length", file.length().toString().toByteArray())
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext content.sha256", sha256.toByteArray())
            }

        val baseDirFile = baseDir.get().asFile

        sources.files
            .filter { it.isFile }
            .sortedBy { it.relativeTo(baseDirFile).invariantSeparatorsPath }
            .forEach { file ->
                val relPath = file.relativeTo(baseDirFile).invariantSeparatorsPath
                val fileContext = "sources[path=$relPath length=${file.length()}]"

                val bytes = file.readBytes()
                val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
                manifest.append("source|").append(relPath).append("|").append(file.length()).append("|").append(sha256)
                    .append("\n")

                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.path", relPath.toByteArray())
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.bytes", bytes)
            }

        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        outputFile.get().asFile.writeText(hash)

        val mf = manifestFile.get().asFile
        mf.parentFile?.mkdirs()
        mf.writeText(manifest.toString())

        if (dump != null && dumpFile != null) {
            dumpFile.parentFile?.mkdirs()
            dumpFile.writeText(dump.toString())
        }
    }
}
