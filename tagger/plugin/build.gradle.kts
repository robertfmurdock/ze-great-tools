plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    base
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", libs.versions.org.jetbrains.kotlin.get()))
    implementation(libs.org.ajoberstar.grgit.gradle.plugin)
    testImplementation(libs.org.jetbrains.kotlin.kotlin.test.junit5)
}

testing {
    suites {
        val functionalTest by registering(JvmTestSuite::class) {
            gradlePlugin.testSourceSets(sources)
        }
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

tasks {
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }
    named<Test>("test") {
        useJUnitPlatform()
    }
}