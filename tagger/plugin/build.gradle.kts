import java.nio.charset.Charset
import java.util.Base64

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
    base
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    alias(libs.plugins.com.gradle.plugin.publish)
    signing
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
    formatKotlinMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    lintKotlinMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}


signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    if (signingKey != null) {
        val decodedKey = Base64.getDecoder().decode(signingKey).toString(Charset.defaultCharset())
        useInMemoryPgpKeys(
            decodedKey,
            signingPassword
        )
    }
    sign(publishing.publications)
}
