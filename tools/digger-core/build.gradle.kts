@file:Suppress("UnstableApiUsage")

plugins {
    base
    id("com.zegreatrob.tools.plugins.reports")
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    id("com.zegreatrob.tools.plugins.publish")
    id("org.jetbrains.kotlin.jvm")
    signing
    `java-library`
}

group = "com.zegreatrob.tools"

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(libs.org.ajoberstar.grgit.grgit.core)
    testImplementation(libs.org.jetbrains.kotlin.kotlin.test.junit5)
}

tasks {
    named<Test>("test") {
        useJUnitPlatform()
    }
    formatKotlinMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    lintKotlinMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "$group"
            artifactId = project.name
            version = "${project.version}"

            from(components["java"])
        }
    }
}
