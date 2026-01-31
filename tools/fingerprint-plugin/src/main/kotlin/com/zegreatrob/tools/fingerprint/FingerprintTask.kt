package com.zegreatrob.tools.fingerprint

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest

@CacheableTask
abstract class FingerprintTask : DefaultTask() {
    @get:Input
    abstract val dependencies: Property<String>

    @get:Input
    abstract val pluginVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(dependencies.get().toByteArray())
            .joinToString("") { "%02x".format(it) }

        outputFile.get().asFile.writeText(hash)
    }
}
