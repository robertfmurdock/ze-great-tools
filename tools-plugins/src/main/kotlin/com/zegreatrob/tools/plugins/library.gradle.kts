package com.zegreatrob.tools.plugins

plugins {
    id("com.zegreatrob.tools.plugins.mp")
    id("com.zegreatrob.tools.plugins.publish")
}

tasks {
    val javadocJar by registering(Jar::class, fun Jar.() {
        archiveClassifier.set("javadoc")
        // Include project source directories as minimal javadoc content for Maven Central compliance
        // Kotlin multiplatform libraries don't generate traditional javadocs
        from(projectDir) {
            include("src/**/*.kt", "src/**/*.md")
        }
    })
    publishing.publications {
        withType<MavenPublication> { artifact(javadocJar) }
    }
}

afterEvaluate {
    tasks {
        withType<PublishToMavenRepository> {
            mustRunAfter(withType<Sign>())
        }
    }
}
