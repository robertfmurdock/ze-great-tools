package com.zegreatrob.tools.certifier

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@Suppress("unused")
@CacheableTask
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
        val keytoolPath = resolveKeytoolPath()
        val output = executeKeytool(keytoolPath, cert)
        validateResult(output)
    }

    private fun resolveKeytoolPath(): String {
        val javaLauncher = javaToolchainService.launcherFor { spec ->
            spec.getLanguageVersion().set(JavaLanguageVersion.of(jdkSelector.get()))
        }
        val javaHome = javaLauncher.get().metadata.installationPath
        return javaHome.file("bin/keytool").asFile.absolutePath
    }

    private fun executeKeytool(keytoolPath: String, cert: String): ExecutionOutput {
        val outStream = ByteArrayOutputStream()
        val result = execOperations.exec { spec ->
            spec.commandLine(keytoolPath, "-importcert", "-file", cert, "-alias", cert, "-cacerts", "-storepass", "changeit", "-noprompt")
            spec.standardOutput = outStream
            spec.errorOutput = outStream
            spec.isIgnoreExitValue = true
        }
        return ExecutionOutput(result.exitValue, outStream.toString())
    }

    private fun validateResult(output: ExecutionOutput) {
        if (output.exitCode != 0 && !output.text.contains("already exists")) {
            throw Exception("Unexpected error.\n${output.text}")
        }
    }

    private data class ExecutionOutput(val exitCode: Int, val text: String)
}
