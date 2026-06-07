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
}

repositories {
    mavenCentral()
}

tasks.register<Checksum>("jvmDistZipChecksum") {
    group = "distribution"
    description = "Generate SHA-256 checksum for JVM distribution zip"
    dependsOn("jvmDistZip")
    inputFiles.from(layout.buildDirectory.file("distributions/digger-cli-jvm.zip"))
    outputDirectory.set(layout.buildDirectory.dir("distributions"))
    checksumAlgorithm.set(Checksum.Algorithm.SHA256)
    appendFileNameToChecksum.set(false)
}

val generatedDirectory = project.layout.buildDirectory.dir("generated-sources/templates/kotlin/main")

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
        compilations {
            "main" {
                packageJson {
                    name = "@continuous-excellence/digger"
                    customField("package-name", "@continuous-excellence/digger")
                    customField("author", "rob@continuousexcellence.io")
                    customField("license", "MIT")
                    customField("keywords", arrayOf("git", "contribution", "pair", "agile", "coaching", "statistics"))
                    customField("bin", mapOf("digger" to "kotlin/bin/digger"))
                    customField("homepage", "https://github.com/robertfmurdock/ze-great-tools")
                    customField("repository", "github:robertfmurdock/ze-great-tools")
                }
            }
        }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

val mainNpmProjectDir = kotlin.js().compilations.getByName("main").npmProject.dir

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
    val copyGuideResources by registering(Copy::class) {
        group = "build"
        description = "Copy guide resources from digger-guide module source"
        from(rootProject.layout.projectDirectory.dir("../tools/digger-guide/src/commonMain/resources"))
        into(layout.buildDirectory.dir("generated/resources/commonMain"))
        include("help/digger-guide.md")
    }
    withType<ProcessResources>().configureEach {
        dependsOn(copyGuideResources)
    }
    withType(Test::class) {
        useJUnitPlatform()
        environment("EXPECTED_VERSION", project.version)
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    withType<CreateStartScripts> {
        applicationName = "digger"
    }
    val copyReadme by registering(Copy::class) {
        dependsOn("jsPackageJson", ":kotlinNpmInstall")
        from(layout.projectDirectory.file("README.md"))
        into(mainNpmProjectDir)
    }
    val jsCliTar by registering(Tar::class) {
        dependsOn(
            copyReadme,
            "jsPackageJson",
            ":kotlinNpmInstall",
            "compileKotlinJs",
            "jsProcessResources",
            "compileProductionExecutableKotlinJs",
            "jsProductionExecutableCompileSync",
        )
        from(mainNpmProjectDir)
        compression = Compression.GZIP
        archiveFileName.set("digger-cli-js.tgz")
    }
    register<Exec>("jsLink") {
        group = "build setup"
        description = "Link digger CLI to local npm for development testing"
        dependsOn(jsCliTar)
        workingDir(mainNpmProjectDir)
        commandLine("npm", "link")
    }
    val confirmJvmDiggerCanRun by registering(Exec::class) {
        dependsOn("installJvmDist")
        workingDir(layout.projectDirectory)
        commandLine("build/install/digger-cli-jvm/bin/digger", "--version")
    }
    check {
        dependsOn(confirmJvmDiggerCanRun)
    }
    val jsPublish by registering(Exec::class) {
        dependsOn(jsCliTar)
        mustRunAfter(check)
        workingDir(mainNpmProjectDir)
        if (isSnapshot()) {
            commandLine("npm", "publish", "--dry-run", "--access", "public", "--tag", "snapshot")
        } else {
            commandLine("npm", "publish", "--access", "public")
        }
    }
    register("publish") {
        group = "publishing"
        description = "Publish digger CLI to npm registry"
        dependsOn(jsPublish)
        mustRunAfter(check)
    }
    val copyTemplates by registering(Copy::class) {
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
            resources.srcDir(copyGuideResources.map { it.destinationDir })
        }
    }
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")

NodeJsRootPlugin.apply(project.rootProject)
project.rootProject.tasks.named("kotlinNpmInstall") {
    dependsOn(gradle.includedBuild("tools").task(":kotlinNpmInstall"))
}
project.rootProject.tasks.named("kotlinNodeJsSetup") {
    dependsOn(provider { gradle.includedBuild("tools").task(":kotlinNodeJsSetup") })
}
