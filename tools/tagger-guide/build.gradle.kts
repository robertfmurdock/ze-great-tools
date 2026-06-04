plugins {
    `java-library`
    id("com.zegreatrob.tools.plugins.publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
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
