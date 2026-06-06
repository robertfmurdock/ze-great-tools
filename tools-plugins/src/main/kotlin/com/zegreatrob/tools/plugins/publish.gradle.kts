package com.zegreatrob.tools.plugins

import org.gradle.api.publish.maven.MavenPom
import java.nio.charset.Charset
import java.util.*

plugins {
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

group = "com.zegreatrob.tools"

afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach { configurePom(it) }
}

fun configurePom(publication: MavenPublication) {
    val scmUrl = "https://github.com/robertfmurdock/ze-great-tools"
    publication.pom.apply {
        name.set(project.name)
        description.set(project.name)
        url.set(scmUrl)
        configureLicense(scmUrl)
        configureDevelopers()
        configureScm(scmUrl)
    }
}

fun MavenPom.configureLicense(scmUrl: String) = licenses {
    license {
        name.set("MIT License")
        url.set("$scmUrl/blob/main/LICENSE")
        distribution.set("repo")
    }
}

fun MavenPom.configureDevelopers() = developers {
    developer {
        id.set("robertfmurdock")
        name.set("Rob Murdock")
        email.set("rob@continuousexcellence.io")
    }
}

fun MavenPom.configureScm(scmUrl: String) = scm {
    url.set(scmUrl)
    connection.set("git@github.com:robertfmurdock/ze-great-tools.git")
    developerConnection.set("git@github.com:robertfmurdock/ze-great-tools.git")
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    if (signingKey != null) {
        val decodedKey = Base64.getDecoder().decode(signingKey).toString(Charset.defaultCharset())
        useInMemoryPgpKeys(
            decodedKey,
            signingPassword,
        )
    }
    sign(publishing.publications)
}

tasks {
    publish { finalizedBy("::closeAndReleaseSonatypeStagingRepository") }
}
