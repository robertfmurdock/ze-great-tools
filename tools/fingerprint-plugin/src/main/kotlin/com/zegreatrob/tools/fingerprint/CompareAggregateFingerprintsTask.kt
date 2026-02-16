package com.zegreatrob.tools.fingerprint

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CompareAggregateFingerprintsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val currentFingerprint: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val expectedFingerprint: RegularFileProperty

    @TaskAction
    fun execute() {
        val current = currentFingerprint.get().asFile.readText().trim()
        val expected = expectedFingerprint.get().asFile.readText().trim()

        if (current == expected) {
            println("FINGERPRINT_MATCH=true")
        } else {
            println("FINGERPRINT_MATCH=false")
            throw GradleException("Aggregate fingerprint did not match expected fingerprint.")
        }
    }
}
