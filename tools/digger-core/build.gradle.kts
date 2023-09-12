@file:Suppress("UnstableApiUsage")

plugins {
    id("com.zegreatrob.tools.plugins.jvm")
    id("com.zegreatrob.tools.plugins.publish")
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
    implementation(project(":digger-model"))
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
