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
        val context = DigestContext(digestInputDumpFile.orNull?.asFile)
        val manifest = StringBuilder()
        processAllInputs(digest, context, manifest)
        writeOutputFiles(digest, manifest, context)
    }

    private fun processAllInputs(digest: MessageDigest, context: DigestContext, manifest: StringBuilder) {
        processPluginVersion(digest, context, manifest)
        processClasspath(digest, context, manifest)
        processPublishedArtifacts(digest, context, manifest)
        processSources(digest, context, manifest)
    }

    private fun processPluginVersion(digest: MessageDigest, context: DigestContext, manifest: StringBuilder) {
        manifest.append("pluginVersion|").append(pluginVersion.get()).append("|\n")
        context.updateBytes(digest, "pluginVersion(prefix)", "pluginVersion=".toByteArray())
        context.updateBytes(digest, "pluginVersion(value)", pluginVersion.get().toByteArray())
    }

    private fun processClasspath(digest: MessageDigest, context: DigestContext, manifest: StringBuilder) {
        classpath.files.sortedBy { it.name }.forEach { file ->
            processFileEntry(digest, context, manifest, file, "classpath")
        }
    }

    private fun processPublishedArtifacts(digest: MessageDigest, context: DigestContext, manifest: StringBuilder) {
        publishedArtifacts.files.sortedBy { it.name }.forEach { file ->
            processFileEntry(digest, context, manifest, file, "artifact")
        }
    }

    private fun processFileEntry(
        digest: MessageDigest,
        context: DigestContext,
        manifest: StringBuilder,
        file: java.io.File,
        category: String,
    ) {
        val sha256 = file.sha256Hex()
        manifest.append("$category|").append(file.name).append("|").append(file.length()).append("|").append(sha256).append("\n")
        addFileToDigest(digest, context, category, file, sha256)
    }

    private fun addFileToDigest(digest: MessageDigest, context: DigestContext, category: String, file: java.io.File, sha256: String) {
        val fileContext = "$category[fileName=${file.name} fileLength=${file.length()}]"
        addFileMetadataToDigest(digest, context, fileContext, file.name, file.length().toString(), sha256)
    }

    private fun addFileMetadataToDigest(
        digest: MessageDigest,
        context: DigestContext,
        fileContext: String,
        name: String,
        length: String,
        sha256: String,
    ) {
        addFieldToDigest(digest, context, "$fileContext file.name", name.toByteArray())
        addFieldToDigest(digest, context, "$fileContext file.length", length.toByteArray())
        addFieldToDigest(digest, context, "$fileContext content.sha256", sha256.toByteArray())
    }

    private fun addFieldToDigest(digest: MessageDigest, context: DigestContext, label: String, value: ByteArray) {
        context.updateByte(digest, "$label sep(0)", 0)
        context.updateBytes(digest, label, value)
    }

    private fun processSources(digest: MessageDigest, context: DigestContext, manifest: StringBuilder) {
        val baseDirFile = baseDir.get().asFile
        sources.files.filter { it.isFile }.sortedBy { it.relativeTo(baseDirFile).invariantSeparatorsPath }.forEach { file ->
            processSourceFile(digest, context, manifest, file, baseDirFile)
        }
    }

    private fun processSourceFile(
        digest: MessageDigest,
        context: DigestContext,
        manifest: StringBuilder,
        file: java.io.File,
        baseDirFile: java.io.File,
    ) {
        val relPath = file.relativeTo(baseDirFile).invariantSeparatorsPath
        val bytes = file.readBytes()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
        manifest.append("source|$relPath|${file.length()}|$sha256\n")
        addSourceBytesToDigest(digest, context, relPath, bytes)
    }

    private fun addSourceBytesToDigest(digest: MessageDigest, context: DigestContext, relPath: String, bytes: ByteArray) {
        val fileContext = "sources[path=$relPath length=${bytes.size}]"
        addFieldToDigest(digest, context, "$fileContext file.path", relPath.toByteArray())
        addFieldToDigest(digest, context, "$fileContext file.bytes", bytes)
    }

    private fun writeOutputFiles(digest: MessageDigest, manifest: StringBuilder, context: DigestContext) {
        val hash = digest.digest().toHex()
        outputFile.get().asFile.writeText(hash)
        manifestFile.get().asFile.writeToFile(manifest.toString())
        context.writeDump()
    }
}

private fun java.io.File.sha256Hex(): String {
    val md = MessageDigest.getInstance("SHA-256")
    if (isFile) {
        return md.digest(readBytes()).toHex()
    }
    if (isDirectory) {
        processDirectoryContents(md)
    }
    return md.digest().toHex()
}

private fun java.io.File.processDirectoryContents(md: MessageDigest) {
    walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(this).invariantSeparatorsPath }.forEach { file ->
        updateDirectoryHash(md, file)
    }
}

private fun java.io.File.updateDirectoryHash(md: MessageDigest, file: java.io.File) {
    val rel = file.relativeTo(this).invariantSeparatorsPath
    md.update(rel.toByteArray(Charsets.UTF_8))
    md.update(0)
    md.update(file.readBytes())
    md.update(0)
}

private class DigestContext(private val dumpFile: java.io.File?) {
    private val dump = if (dumpFile != null) StringBuilder() else null
    private var step = 0

    fun updateBytes(digest: MessageDigest, context: String, bytes: ByteArray) {
        writeLine(context, "len=${bytes.size} hex=${bytes.toHex()} ascii=${bytes.toString(Charsets.UTF_8)}")
        digest.update(bytes)
    }

    fun updateByte(digest: MessageDigest, context: String, value: Int) {
        writeLine(context, "len=1 hex=${"%02x".format(value and 0xFF)} ascii=\\u${"%04x".format(value and 0xFF)}")
        digest.update(value.toByte())
    }

    fun writeDump() {
        if (dump != null && dumpFile != null) {
            dumpFile.writeToFile(dump.toString())
        }
    }

    private fun writeLine(context: String, detail: String) {
        dump?.append("%04d".format(step++))?.append(" | ")?.append(context)?.append(" | ")?.append(detail)?.append('\n')
    }
}
