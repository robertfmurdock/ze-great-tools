package com.zegreatrob.tools.plugins

import gradle.kotlin.dsl.accessors._2502cef48cff830615fe1c6d6ab5e104.publishing
import gradle.kotlin.dsl.accessors._2502cef48cff830615fe1c6d6ab5e104.signing
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.signing
import org.gradle.kotlin.dsl.withType
import java.nio.charset.Charset
import java.util.Base64

plugins {
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

group = "com.zegreatrob.tools"

afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach {
        with(it) {
            val scmUrl = "https://github.com/robertfmurdock/ze-great-tools"

            pom.name.set(project.name)
            pom.description.set(project.name)
            pom.url.set(scmUrl)

            pom.licenses {
                license {
                    name.set("MIT License")
                    url.set(scmUrl)
                    distribution.set("repo")
                }
            }
            pom.developers {
                developer {
                    id.set("robertfmurdock")
                    name.set("Rob Murdock")
                    email.set("robert.f.murdock@gmail.com")
                }
            }
            pom.scm {
                url.set(scmUrl)
                connection.set("git@github.com:robertfmurdock/ze-great-tools.git")
                developerConnection.set("git@github.com:robertfmurdock/ze-great-tools.git")
            }
        }
    }
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

    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from("${rootDir.absolutePath}/javadocs")
    }
    publishing.publications {
        withType<MavenPublication> { artifact(javadocJar) }
    }
}

fun PublicationContainer.jvmPublication(): NamedDomainObjectSet<Publication> = matching { it.name == "jvm" }
