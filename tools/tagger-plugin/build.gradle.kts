import java.nio.charset.Charset
import java.util.Base64

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.com.gradle.plugin.publish)
    base
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.versioning")
    id("org.jetbrains.kotlin.jvm")
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.org.ajoberstar.grgit.gradle.plugin)
    testImplementation(libs.org.jetbrains.kotlin.kotlin.test.junit5)
}

testing {
    suites {
        register<JvmTestSuite>("functionalTest") {
            gradlePlugin.testSourceSets(sources)
        }
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

tasks {
    "compileFunctionalTestKotlin" {
        dependsOn("compileKotlin")
    }
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }
    publish { finalizedBy("::closeAndReleaseSonatypeStagingRepository") }
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

group = "com.zegreatrob.tools"

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
                    email.set("robert.f.murdock@gmail.com")
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
