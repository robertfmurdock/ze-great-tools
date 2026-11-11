package com.zegreatrob.tools.plugins

import java.nio.charset.Charset
import java.util.*

plugins {
    `maven-publish`
    signing
}

group = "com.zegreatrob.tools"

// Keep all POM configuration inline within this convention plugin.
// See: PUBLISH_ISSUE_ANALYSIS.md and agents.d/context/GRADLE_PLUGIN_CONSTRAINTS.md
publishing.publications.withType<MavenPublication>().configureEach {

    val scmUrl = "https://github.com/robertfmurdock/ze-great-tools"

    pom.name.set(project.name)
    pom.description.set(project.name)
    pom.url.set(scmUrl)

    pom.licenses {
        license {
            name.set("MIT License")
            url.set("$scmUrl/blob/main/LICENSE")
            distribution.set("repo")
        }
    }
    pom.developers {
        developer {
            id.set("robertfmurdock")
            name.set("Rob Murdock")
            email.set("rob@continuousexcellence.io")
        }
    }
    pom.scm {
        url.set(scmUrl)
        connection.set("git@github.com:robertfmurdock/ze-great-tools.git")
        developerConnection.set("git@github.com:robertfmurdock/ze-great-tools.git")
    }
}

signing {
    val signingKey = project.findProperty("signingKey") as String?
    val signingPassword = project.findProperty("signingPassword") as String?

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
