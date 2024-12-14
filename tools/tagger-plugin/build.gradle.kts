@file:Suppress("UnstableApiUsage")

import java.nio.charset.Charset
import java.util.*

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.com.gradle.plugin.publish)
    id("com.zegreatrob.tools.plugins.jvm")
}

repositories {
    mavenCentral()
}

testing {
    suites {
        register<JvmTestSuite>("functionalTest") {
            gradlePlugin.testSourceSets(sources)
        }
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

dependencies {
    implementation(project(":git-adapter"))
    implementation(project(":tagger-core"))
    testImplementation(kotlin("test-junit5", embeddedKotlinVersion))
    "functionalTestImplementation"(platform(libs.org.junit.junit.bom))
    "functionalTestImplementation"(project(":tagger-test"))
}

gradlePlugin {
    website.set("https://github.com/robertfmurdock/ze-great-tools")
    vcsUrl.set("https://github.com/robertfmurdock/ze-great-tools")
    plugins {
        named("com.zegreatrob.tools.tagger") {
            displayName = "Tagger Plugin"
            description =
                "This plugin automates generation of version numbers based on commit messages and git tags."
            tags.addAll("git", "tags", "version", "semver", "github", "release", "commit")
        }
    }
}

tasks {
    "compileFunctionalTestKotlin" {
        dependsOn("compileKotlin")
    }
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }
    publish { finalizedBy("::closeAndReleaseSonatypeStagingRepository") }
    withType(Test::class) {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
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

afterEvaluate {
    publishing.publications.withType<MavenPublication>().forEach {
        with(it) {
            val scmUrl = "https://github.com/robertfmurdock/ze-great-tools"

            pom.name.set(project.name)
            pom.description.set(project.name)
            pom.url.set(scmUrl)

            pom.licenses {
                license {
                    name.set("MIT License")
                    url.set(scmUrl)
                    distribution.set("repo")
                }
            }
            pom.developers {
                developer {
                    id.set("robertfmurdock")
                    name.set("Rob Murdock")
                    email.set("rob@continuousexcellence.io")
                }
            }
            pom.scm {
                url.set(scmUrl)
                connection.set("git@github.com:robertfmurdock/ze-great-tools.git")
                developerConnection.set("git@github.com:robertfmurdock/ze-great-tools.git")
            }
        }
    }
}
