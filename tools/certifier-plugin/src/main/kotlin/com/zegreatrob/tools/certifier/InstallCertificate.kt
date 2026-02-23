package com.zegreatrob.tools.certifier

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@Suppress("unused")
abstract class InstallCertificate
@Inject
constructor(
    private val objectFactory: ObjectFactory,
    private val javaToolchainService: JavaToolchainService,
) : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @Input
    val jdkSelector = objectFactory.property(String::class.java)

    @Input
    val certificatePath = objectFactory.property(String::class.java)

    @TaskAction
    fun installCertificate() {
        val cert = certificatePath.get()
        val javaLauncher =
            javaToolchainService.launcherFor { spec ->
                spec.getLanguageVersion().set(JavaLanguageVersion.of(jdkSelector.get()))
            }
        val javaHome = javaLauncher.get().metadata.installationPath

        val outStream = ByteArrayOutputStream()
        val result = execOperations.exec { spec ->
            spec.commandLine(
                javaHome.file("bin/keytool").asFile.absolutePath,
                "-importcert",
                "-file",
                cert,
                "-alias",
                cert,
                "-cacerts",
                "-storepass",
                "changeit",
                "-noprompt",
            )
            spec.standardOutput = outStream
            spec.errorOutput = outStream
            spec.isIgnoreExitValue = true
        }

        val results = outStream.toString()
        if (result.exitValue != 0 && !results.contains("already exists")) {
            throw Exception("Unexpected error.\n$results")
        }
    }
}
