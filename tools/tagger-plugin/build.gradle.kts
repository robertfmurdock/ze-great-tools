@file:Suppress("UnstableApiUsage")

import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.nio.charset.Charset
import java.util.*

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.com.gradle.plugin.publish)
    id("com.zegreatrob.tools.plugins.jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version embeddedKotlinVersion
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":dependency-bom")))
    implementation(project(":tagger-gradle"))
    implementation(project(":tagger-json"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    testImplementation(kotlin("test-junit5", embeddedKotlinVersion))
}

gradlePlugin {
    website.set("https://github.com/robertfmurdock/ze-great-tools")
    vcsUrl.set("https://github.com/robertfmurdock/ze-great-tools")
    plugins {
        create("tagger") {
            id = "com.zegreatrob.tools.tagger"
            implementationClass = "com.zegreatrob.tools.TaggerPlugin"
            displayName = "Tagger Plugin"
            description =
                "This plugin automates generation of version numbers based on commit messages and git tags."
            tags.addAll("git", "tags", "version", "semver", "github", "release", "commit")
        }
    }
}

tasks {
    val copyGuideFromCli by registering(Copy::class) {
        description = "Copy guide markdown from tagger-cli to plugin resources (single source of truth)"
        from(rootProject.projectDir.resolve("../command-line-tools/tagger-cli/src/commonMain/resources/help"))
        into(project.projectDir.resolve("src/main/resources/help"))
        include("tagger-guide.md")
    }
    processResources {
        dependsOn(copyGuideFromCli)
    }
    publish { finalizedBy("::closeAndReleaseSonatypeStagingRepository") }
    withType(Test::class) {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
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
                    url.set("$scmUrl/blob/main/LICENSE")
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
