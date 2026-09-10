@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.crypto.checksum.Checksum
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version embeddedKotlinVersion
    alias(libs.plugins.org.jmailen.kotlinter)
    alias(libs.plugins.io.sdkman.vendors)
    alias(libs.plugins.org.gradle.crypto.checksum)
    id("com.zegreatrob.tools.plugins.npm-cli")
}

repositories {
    mavenCentral()
}

tasks.register<Checksum>("jvmDistZipChecksum") {
    group = "distribution"
    description = "Generate SHA-256 checksum for JVM distribution zip"
    inputFiles.from(tasks.named("jvmDistZip"))
    outputDirectory.set(layout.buildDirectory.dir("distributions"))
    checksumAlgorithm.set(Checksum.Algorithm.SHA256)
    appendFileNameToChecksum.set(false)
}

val generatedDirectory = project.layout.buildDirectory.dir("generated-sources/templates/kotlin/main")

npmCli {
    packageName.set("@continuous-excellence/tagger")
    description.set("Deterministic semantic versioning from git history. Platform-neutral CLI for calculating versions based on commit messages, with zero configuration required.")
    binaryName.set("tagger")
    directory.set("command-line-tools/tagger-cli")
    guideResourcesDir.set(rootProject.layout.projectDirectory.dir("../tools/tagger-guide/src/commonMain/resources"))
    guideFile.set("help/tagger-guide.md")
    keywords.set(
        listOf(
            "semantic-versioning",
            "semver",
            "git-tags",
            "release-automation",
            "version-management",
            "gradle-plugin",
            "ci-cd",
            "devops",
            "git",
            "contribution",
            "pair",
            "agile",
            "coaching",
            "statistics",
        ),
    )
}

kotlin {
    jvm {
        binaries {
            executable {
                mainClass.set("com.zegreatrob.tools.tagger.cli.MainKt")
            }
        }
    }
    js {
        nodejs {
            useCommonJs()
            binaries.executable()
            testTask {
                useMocha { timeout = "10s" }
                environment("EXPECTED_VERSION", "${project.version}")
                environment("GIT_CONFIG_GLOBAL", "/dev/null")
                environment("GIT_CONFIG_SYSTEM", "/dev/null")
            }
        }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    commonMainImplementation(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    commonMainImplementation("com.zegreatrob.tools:cli-tools")
    commonMainImplementation("com.zegreatrob.tools:tagger-json")
    commonMainImplementation("com.zegreatrob.tools:tagger-core")
    commonMainImplementation("com.zegreatrob.tools:tagger-guide")
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt)
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt.markdown)
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    commonTestImplementation("com.zegreatrob.tools:tagger-test")
    commonTestImplementation(kotlin("test"))
    commonTestImplementation(libs.com.zegreatrob.testmints.minassert)
    commonTestImplementation(libs.com.zegreatrob.testmints.standard)
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
        environment("EXPECTED_VERSION", project.version)
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    withType<CreateStartScripts> {
        applicationName = "tagger"
    }
    val confirmJsTaggerCanRun = register<Exec>("confirmJsTaggerCanRun") {
        dependsOn("jsCliTar")
        workingDir(kotlin.js().compilations.getByName("main").npmProject.dir)
        commandLine("kotlin/bin/tagger", "calculate-version")
    }
    val confirmJvmTaggerCanRun = register<Exec>("confirmJvmTaggerCanRun") {
        dependsOn("installJvmDist")
        workingDir(layout.projectDirectory)
        commandLine("build/install/tagger-cli-jvm/bin/tagger", "--version")
    }
    check {
        dependsOn(confirmJsTaggerCanRun)
        dependsOn(confirmJvmTaggerCanRun)
    }
    val copyTemplates = register<Copy>("copyTemplates") {
        inputs.property("version", rootProject.version)
        filteringCharset = "UTF-8"
        from(project.projectDir.resolve("src/commonMain/templates")) {
            filter<ReplaceTokens>("tokens" to mapOf("TAGGER_VERSION" to rootProject.version))
        }
        into(generatedDirectory)
    }
    withType<KotlinCompile> {
        dependsOn(copyTemplates)
    }
    kotlin.sourceSets {
        commonMain {
            kotlin.srcDir(copyTemplates)
        }
    }
}
