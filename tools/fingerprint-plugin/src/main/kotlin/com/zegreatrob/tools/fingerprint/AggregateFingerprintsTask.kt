package com.zegreatrob.tools.fingerprint

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

@CacheableTask
abstract class AggregateFingerprintsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includedFingerprints: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val localFingerprint: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includedManifests: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val localManifest: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputManifestFile: RegularFileProperty

    @TaskAction
    fun execute() {
        writeAggregateFingerprint()
        writeMergedManifest()
    }

    private fun writeAggregateFingerprint() {
        val hash = computeAggregateHash()
        outputFile.get().asFile.writeToFile(hash)
    }

    private fun computeAggregateHash(): String {
        val allFingerprintFiles = includedFingerprints.files + localFingerprint.get().asFile
        val combinedContent = allFingerprintFiles.filter { it.exists() }.map { it.readText().trim() }.sorted().joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(combinedContent.toByteArray()).toHex()
    }

    private fun writeMergedManifest() {
        val merged = mergeManifestFiles()
        outputManifestFile.get().asFile.writeToFile(merged)
    }

    private fun mergeManifestFiles(): String = buildString {
        val allManifestFiles = (includedManifests.files + localManifest.get().asFile).filter { it.exists() }.sortedBy { it.invariantSeparatorsPath }
        allManifestFiles.forEachIndexed { index, file ->
            if (index > 0) append('\n')
            append(file.readText().trimEnd())
        }
        append('\n')
    }
}
