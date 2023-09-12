@file:Suppress("UnstableApiUsage")

plugins {
    id("com.zegreatrob.tools.plugins.jvm")
    id("com.zegreatrob.tools.plugins.publish")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
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
    implementation(platform(project(":dependency-bom")))
    api(project(":digger-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
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
