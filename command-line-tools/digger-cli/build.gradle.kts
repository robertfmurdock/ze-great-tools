@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.crypto.checksum.Checksum
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
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
    packageName.set("@continuous-excellence/digger")
    description.set("Privacy-controlled git analytics for team insights. CLI for extracting contribution statistics, commit analysis, and developer metrics from git repositories.")
    binaryName.set("digger")
    directory.set("command-line-tools/digger-cli")
    guideResourcesDir.set(rootProject.layout.projectDirectory.dir("../tools/digger-guide/src/commonMain/resources"))
    guideFile.set("help/digger-guide.md")
    keywords.set(
        listOf(
            "git-analytics",
            "contribution-tracking",
            "team-metrics",
            "git-statistics",
            "commit-analysis",
            "developer-metrics",
            "code-statistics",
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
                mainClass.set("com.zegreatrob.tools.digger.cli.MainKt")
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
    commonMainImplementation("com.zegreatrob.tools:digger-core")
    commonMainImplementation("com.zegreatrob.tools:digger-json")
    commonMainImplementation("com.zegreatrob.tools:digger-guide")
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt)
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt.markdown)
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    commonTestImplementation("com.zegreatrob.tools:digger-test")
    commonTestImplementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    commonTestImplementation(libs.org.jetbrains.kotlin.kotlin.test)
    commonTestImplementation(libs.com.zegreatrob.testmints.minassert)
    commonTestImplementation(libs.com.zegreatrob.testmints.standard)
    "jvmTestImplementation"(libs.org.jetbrains.kotlin.kotlin.test.junit5)
    "jvmTestImplementation"("org.junit.jupiter:junit-jupiter-api")
    "jvmTestImplementation"("org.junit.jupiter:junit-jupiter-engine")
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
        environment("EXPECTED_VERSION", project.version)
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    withType<CreateStartScripts> {
        applicationName = "digger"
    }
    val confirmJvmDiggerCanRun = register<Exec>("confirmJvmDiggerCanRun") {
        dependsOn("installJvmDist")
        workingDir(layout.projectDirectory)
        commandLine("build/install/digger-cli-jvm/bin/digger", "--version")
    }
    check {
        dependsOn(confirmJvmDiggerCanRun)
    }
    val copyTemplates = register<Copy>("copyTemplates") {
        inputs.property("version", rootProject.version)
        filteringCharset = "UTF-8"
        from(project.projectDir.resolve("src/commonMain/templates")) {
            filter<ReplaceTokens>("tokens" to mapOf("DIGGER_VERSION" to rootProject.version))
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

NodeJsRootPlugin.apply(project.rootProject)
project.rootProject.tasks.named("kotlinNpmInstall") {
    dependsOn(gradle.includedBuild("tools").task(":kotlinNpmInstall"))
}
project.rootProject.tasks.named("kotlinNodeJsSetup") {
    dependsOn(provider { gradle.includedBuild("tools").task(":kotlinNodeJsSetup") })
}
