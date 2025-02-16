package com.zegreatrob.tools

plugins {
    base
}

tasks {
    register("installCert", com.zegreatrob.tools.certifier.InstallCertificate::class)
}
