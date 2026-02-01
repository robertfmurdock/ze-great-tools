package com.zegreatrob.tools.fingerprint

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
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

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val digest = MessageDigest.getInstance("SHA-256")

        digest.update(("pluginVersion=" + pluginVersion.get()).toByteArray())

        classpath.files
            .sortedBy { it.name }
            .forEach { file ->
                digest.update(0)
                digest.update(file.name.toByteArray())
                digest.update(0)
                digest.update(file.length().toString().toByteArray())
            }

        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        outputFile.get().asFile.writeText(hash)
    }
}
