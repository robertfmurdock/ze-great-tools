package com.zegreatrob.tools.plugins

plugins {
    base
    id("com.zegreatrob.tools.plugins.reports")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    id("org.jetbrains.kotlin.multiplatform")
    id("com.zegreatrob.tools.plugins.publish")
    signing
}

kotlin {
    jvmToolchain(11)
}

tasks {
    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from("${rootDir.absolutePath}/javadocs")
    }
    publishing.publications {
        withType<MavenPublication> { artifact(javadocJar) }
    }

    val signKotlinMultiplatformPublication = findByName("signKotlinMultiplatformPublication")
    if (signKotlinMultiplatformPublication != null) {
        tasks.findByName("publishJsPublicationToSonatypeRepository")
            ?.dependsOn(signKotlinMultiplatformPublication)
        tasks.findByName("publishJvmPublicationToSonatypeRepository")
            ?.dependsOn(signKotlinMultiplatformPublication)
    }
    tasks.findByName("signJsPublication")
        ?.let {
            tasks.findByName("publishKotlinMultiplatformPublicationToSonatypeRepository")
                ?.dependsOn(it)
        }
    tasks.findByName("signJvmPublication")
        ?.let {
            tasks.findByName("publishKotlinMultiplatformPublicationToSonatypeRepository")
                ?.dependsOn(it)
        }
}
