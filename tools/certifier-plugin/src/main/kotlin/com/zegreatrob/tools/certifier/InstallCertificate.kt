package com.zegreatrob.tools.certifier

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecResult
import org.gradle.process.internal.DefaultExecSpec
import org.gradle.process.internal.ExecAction
import org.gradle.process.internal.ExecActionFactory
import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@Suppress("unused")
open class InstallCertificate
@Inject
constructor(
    private val objectFactory: ObjectFactory,
    private val execActionFactory: ExecActionFactory,
    private val javaToolchainService: JavaToolchainService,
) : DefaultTask() {
    @Input
    lateinit var jdkSelector: String

    @Input
    lateinit var certificatePath: String

    @TaskAction
    fun installCertificate() {
        var execSpec: DefaultExecSpec = objectFactory.newInstance(DefaultExecSpec::class.java)
        val cert = certificatePath
        val javaLauncher =
            javaToolchainService.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(jdkSelector))
            }
        val javaHome = javaLauncher.get().metadata.installationPath
        execSpec.commandLine(
            ("$javaHome/bin/keytool -importcert -file $cert -alias $cert -cacerts -storepass changeit -noprompt")
                .split(" "),
        )
        val execAction: ExecAction = execActionFactory.newExecAction()

        val outStream = ByteArrayOutputStream()
        execAction.standardOutput = outStream

        execSpec.copyTo(execAction)
        try {
            objectFactory.property(ExecResult::class.java).set(execAction.execute())
        } catch (_: ExecException) {
            val results = outStream.toString()
            if (!results.contains("already exists")) {
                throw Exception("Unexpected error.\n$results")
            }
        }
    }
}
