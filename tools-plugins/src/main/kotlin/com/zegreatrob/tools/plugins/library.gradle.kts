package com.zegreatrob.tools.plugins

plugins {
    id("com.zegreatrob.tools.plugins.mp")
    id("com.zegreatrob.tools.plugins.publish")
}

tasks {
    val javadocJar by registering(Jar::class, fun Jar.() {
        archiveClassifier.set("javadoc")
        from("${rootDir.absolutePath}/javadocs")
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
