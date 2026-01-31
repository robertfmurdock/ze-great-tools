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

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val allFiles = includedFingerprints.files + localFingerprint.get().asFile

        val combinedContent = allFiles.filter { it.exists() }
            .map { it.readText().trim() }
            .sorted()
            .joinToString("|")

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(combinedContent.toByteArray())
            .joinToString("") { "%02x".format(it) }

        outputFile.get().asFile.writeText(hash)
    }
}
