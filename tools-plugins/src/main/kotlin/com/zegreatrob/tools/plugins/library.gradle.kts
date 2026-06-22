package com.zegreatrob.tools.plugins

plugins {
    id("com.zegreatrob.tools.plugins.mp")
    id("com.zegreatrob.tools.plugins.publish")
}

tasks {
    val javadocJar = register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(projectDir) {
            include("src/**/*.kt", "src/**/*.md")
        }
    }
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
