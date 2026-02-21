package com.zegreatrob.tools

import com.zegreatrob.tools.certifier.InstallCertificate
import org.gradle.api.Plugin
import org.gradle.api.Project

class CertifierPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("base")
        project.tasks.register("installCert", InstallCertificate::class.java)
    }
}
