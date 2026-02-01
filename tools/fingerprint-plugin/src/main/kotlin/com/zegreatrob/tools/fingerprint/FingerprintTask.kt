package com.zegreatrob.tools.fingerprint

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

@CacheableTask
abstract class FingerprintTask : DefaultTask() {

    @get:Input
    abstract val pluginVersion: Property<String>

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Optional
    @get:OutputFile
    abstract val digestInputDumpFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val digest = MessageDigest.getInstance("SHA-256")

        val dumpFile = digestInputDumpFile.orNull?.asFile
        val dump = if (dumpFile != null) StringBuilder() else null
        var step = 0

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

        updateBytes("pluginVersion(prefix)", "pluginVersion=".toByteArray())
        updateBytes("pluginVersion(value)", pluginVersion.get().toByteArray())

        classpath.files
            .sortedBy { it.name }
            .forEach { file ->
                val fileContext = "classpath[fileName=${file.name} fileLength=${file.length()}]"
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.name", file.name.toByteArray())
                updateByte("$fileContext sep(0)", 0)
                updateBytes("$fileContext file.length", file.length().toString().toByteArray())
            }

        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        outputFile.get().asFile.writeText(hash)

        if (dump != null && dumpFile != null) {
            dumpFile.parentFile?.mkdirs()
            dumpFile.writeText(dump.toString())
        }
    }
}
