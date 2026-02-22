plugins {
    id("com.zegreatrob.tools.plugins.jvm")
    id("com.zegreatrob.tools.plugins.publish")
    `java-library`
}

java {
    withSourcesJar()
}

dependencies {
    api(project(":tagger-core"))
    api(project(":git-adapter"))
}

tasks {
    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from("${rootDir.absolutePath}/javadocs")
    }

    publishing.publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(javadocJar)
        }
    }
}
